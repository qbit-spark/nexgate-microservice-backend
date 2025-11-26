package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.PaymentCallback;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;
import org.nextgate.nextgatebackend.financial_system.payment_processing.events.PaymentCompletedEvent;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentRequest;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.ExternalPaymentProcessor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.PaymentOrchestrator;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.UniversalCheckoutSessionService;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.WalletPaymentProcessor;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.PaymentNotificationMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrchestratorImpl implements PaymentOrchestrator {

    private final WalletPaymentProcessor walletPaymentProcessor;
    private final ExternalPaymentProcessor externalPaymentProcessor;
    private final PaymentCallback paymentCallback;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPublisher notificationPublisher;
    private final UniversalCheckoutSessionService checkoutSessionService;

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID checkoutSessionId, CheckoutSessionsDomains sessionDomain)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        PaymentRequest request = PaymentRequest.builder()
                .checkoutSessionId(checkoutSessionId)
                .sessionDomain(sessionDomain)
                .build();

        return processPayment(request);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        log.info("Processing payment | Session: {} | Domain: {}",
                request.getCheckoutSessionId(), request.getSessionDomain());

        // Fetch checkout session (universal)
        PayableCheckoutSession session = checkoutSessionService.findCheckoutSession(
                request.getCheckoutSessionId(),
                request.getSessionDomain()
        );

        // Validate session status
        if (session.getStatus() != CheckoutSessionStatus.PENDING_PAYMENT) {
            throw new RandomExceptions("Cannot process payment - session status: " + session.getStatus());
        }

        // Check expiration
        if (session.isExpired()) {
            session.setStatus(CheckoutSessionStatus.EXPIRED);
            checkoutSessionService.saveCheckoutSession(session);
            throw new RandomExceptions("Checkout session has expired");
        }

        try {
            // Determine payment method
            PaymentMethod paymentMethod = determinePaymentMethod(session, request);

            log.info("Payment method: {}", paymentMethod);

            // Route to processor
            PaymentResult result = routeToProcessor(session, paymentMethod);

            // Handle result
            if (result.isSuccess()) {
                return handleSuccessfulPayment(session, result);
            } else if (result.isPending()) {
                return handlePendingPayment(session, result);
            } else {
                return handleFailedPayment(session, result);
            }

        } catch (Exception e) {
            log.error("Payment failed for session: {}", session.getSessionId(), e);

            session.setStatus(CheckoutSessionStatus.PAYMENT_FAILED);
            checkoutSessionService.saveCheckoutSession(session);

            throw e;
        }
    }

    private PaymentMethod determinePaymentMethod(
            PayableCheckoutSession session,
            PaymentRequest request) throws RandomExceptions {

        if (request.getPaymentMethod() != null) {
            return request.getPaymentMethod();
        }

        // Default to WALLET
        log.info("No payment method specified, defaulting to WALLET");
        return PaymentMethod.WALLET;
    }

    private PaymentResult routeToProcessor(
            PayableCheckoutSession session,
            PaymentMethod paymentMethod) throws ItemNotFoundException, RandomExceptions {

        return switch (paymentMethod) {
            case WALLET -> walletPaymentProcessor.processPayment(session);
            case MPESA, TIGO_PESA, AIRTEL_MONEY, HALOPESA,
                 CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER ->
                    externalPaymentProcessor.processPayment(session, paymentMethod);
            case CASH_ON_DELIVERY -> throw new RandomExceptions("COD handled separately");
        };
    }

    private PaymentResponse handleSuccessfulPayment(
            PayableCheckoutSession session,
            PaymentResult result) throws RandomExceptions {

        log.info("✅ Payment successful | Session: {} | Domain: {}",
                session.getSessionId(), session.getSessionDomain());

        session.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session.setEscrowId(result.getEscrow().getId());

        checkoutSessionService.saveCheckoutSession(session);

        sendPaymentSuccessNotification(session, result.getEscrow());

        // Invoke callback
        try {
            paymentCallback.onPaymentSuccess(session, result.getEscrow());
        } catch (Exception e) {
            log.error("Payment callback failed", e);
        }

        // Publish event for async order/booking creation
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    this,
                    session.getSessionId(),
                    session.getSessionDomain(), // ← Domain identifier
                    session,                     // ← Universal interface
                    result.getEscrow(),
                    LocalDateTime.now()
            );

            eventPublisher.publishEvent(event);

            log.info("✓ PaymentCompletedEvent published");

        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent", e);
        }

        checkoutSessionService.saveCheckoutSession(session);

        return PaymentResponse.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .message(buildSuccessMessage(session.getSessionDomain()))
                .checkoutSessionId(session.getSessionId())
                .escrowId(result.getEscrow().getId())
                .escrowNumber(result.getEscrow().getEscrowNumber())
                .orderId(null)
                .orderNumber(null)
                .paymentMethod(PaymentMethod.WALLET)
                .amountPaid(result.getEscrow().getTotalAmount())
                .platformFee(result.getEscrow().getPlatformFeeAmount())
                .sellerAmount(result.getEscrow().getSellerAmount())
                .currency(result.getEscrow().getCurrency())
                .build();
    }

    private String buildSuccessMessage(CheckoutSessionsDomains sessionDomain) {
        return switch (sessionDomain) {
            case PRODUCT -> "Payment completed successfully. Your order is being processed.";
            case EVENT -> "Payment completed successfully. Your booking is being processed.";
            default -> "Payment completed successfully.";
        };
    }

    private PaymentResponse handlePendingPayment(
            PayableCheckoutSession session,
            PaymentResult result) throws RandomExceptions {

        log.info("Payment pending | Session: {}", session.getSessionId());

        session.setStatus(CheckoutSessionStatus.PAYMENT_PROCESSING);
        checkoutSessionService.saveCheckoutSession(session);

        try {
            paymentCallback.onPaymentPending(session, result);
        } catch (Exception e) {
            log.error("Payment pending callback failed", e);
        }

        return PaymentResponse.builder()
                .success(false)
                .status(PaymentStatus.PENDING)
                .message(result.getMessage())
                .checkoutSessionId(session.getSessionId())
                .paymentUrl(result.getPaymentUrl())
                .ussdCode(result.getUssdCode())
                .referenceNumber(result.getExternalReference())
                .build();
    }

    private PaymentResponse handleFailedPayment(
            PayableCheckoutSession session,
            PaymentResult result) throws RandomExceptions {

        log.warn("Payment failed | Session: {} | Reason: {}",
                session.getSessionId(), result.getMessage());

        session.setStatus(CheckoutSessionStatus.PAYMENT_FAILED);
        checkoutSessionService.saveCheckoutSession(session);

        try {
            paymentCallback.onPaymentFailure(session, result, result.getErrorMessage());
        } catch (Exception e) {
            log.error("Payment failure callback failed", e);
        }

        return PaymentResponse.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .message(result.getMessage())
                .checkoutSessionId(session.getSessionId())
                .build();
    }

    private void sendPaymentSuccessNotification(
            PayableCheckoutSession session,
            EscrowAccountEntity escrow) {

        AccountEntity customer = session.getPayer();

        Map<String, Object> data = PaymentNotificationMapper.mapPaymentReceived(
                customer.getFirstName(),
                customer.getEmail(),
                escrow.getTotalAmount(),
                escrow.getCurrency(),
                "WALLET",
                escrow.getId().toString(),
                escrow.getEscrowNumber(),
                String.valueOf(session.getSessionId())
        );

        Recipient recipient = Recipient.builder()
                .userId(customer.getId().toString())
                .email(customer.getEmail())
                .phone(customer.getPhoneNumber())
                .name(customer.getFirstName())
                .language("en")
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.PAYMENT_RECEIVED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SMS,
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.HIGH)
                .data(data)
                .build();

        notificationPublisher.publish(event);

        log.info("📤 Payment success notification sent");
    }
}