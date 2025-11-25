package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.events.PaymentCompletedEvent;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.callbacks.PaymentCallback;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentMethod;
import org.nextgate.nextgatebackend.financial_system.payment_processing.enums.PaymentStatus;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentRequest;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.ExternalPaymentProcessor;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.PaymentOrchestrator;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.WalletPaymentProcessor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.PaymentNotificationMapper;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.repo.OrderRepository;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.service.OrderService;
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

    private final CheckoutSessionRepo checkoutSessionRepo;
    private final WalletPaymentProcessor walletPaymentProcessor;
    private final ExternalPaymentProcessor externalPaymentProcessor;
    private final OrderService orderService;
    private final PaymentCallback paymentCallback;
    private final OrderRepository orderRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID checkoutSessionId)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        PaymentRequest request = PaymentRequest.builder()
                .checkoutSessionId(checkoutSessionId)
                .build();

        return processPayment(request);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request)
            throws ItemNotFoundException, RandomExceptions, BadRequestException {

        log.info("Processing payment for checkout session: {}", request.getCheckoutSessionId());

        // Fetch checkout session
        CheckoutSessionEntity checkoutSession = checkoutSessionRepo
                .findById(request.getCheckoutSessionId())
                .orElseThrow(() -> new ItemNotFoundException("Checkout session not found"));

        // Validate checkout session status
        if (checkoutSession.getStatus() != CheckoutSessionStatus.PENDING_PAYMENT) {
            throw new RandomExceptions(
                    "Cannot process payment - checkout session status: " + checkoutSession.getStatus()
            );
        }

        //Todo: Before save we have to find the best

        // Check if session expired
        if (checkoutSession.isExpired()) {
            checkoutSession.setStatus(CheckoutSessionStatus.EXPIRED);
            checkoutSessionRepo.save(checkoutSession);
            throw new RandomExceptions("Checkout session has expired");
        }

        try {
            // Determine payment method
            PaymentMethod paymentMethod = determinePaymentMethod(checkoutSession, request);

            log.info("Payment method determined: {}", paymentMethod);

            // Route to appropriate processor
            PaymentResult result = routeToProcessor(checkoutSession, paymentMethod);

            // Handle payment result-- This is the heart of payment orchestration
            if (result.isSuccess()) {
                return handleSuccessfulPayment(checkoutSession, result);
            } else if (result.isPending()) {
                return handlePendingPayment(checkoutSession, result);
            } else {
                return handleFailedPayment(checkoutSession, result);
            }

        } catch (Exception e) {
            log.error("Payment processing failed for checkout session: {}",
                    checkoutSession.getSessionId(), e);

            // Update checkout session to PAYMENT_FAILED
            checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_FAILED);
            checkoutSessionRepo.save(checkoutSession);

            throw e;
        }
    }


    // Determines payment method from checkout session or request override
    private PaymentMethod determinePaymentMethod(
            CheckoutSessionEntity checkoutSession,
            PaymentRequest request) throws RandomExceptions {

        // Override from request if provided
        if (request.getPaymentMethod() != null) {
            return request.getPaymentMethod();
        }

        // Get from checkout session payment intent
        if (checkoutSession.getPaymentIntent() != null) {
            String provider = checkoutSession.getPaymentIntent().getProvider();

            if (provider == null) {
                throw new RandomExceptions("Payment method not specified");
            }

            return switch (provider.toUpperCase()) {
                case "WALLET" -> PaymentMethod.WALLET;
                case "MPESA", "MNO_PAYMENT" -> PaymentMethod.MPESA;
                case "TIGO_PESA" -> PaymentMethod.TIGO_PESA;
                case "AIRTEL_MONEY" -> PaymentMethod.AIRTEL_MONEY;
                case "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD;
                case "DEBIT_CARD" -> PaymentMethod.DEBIT_CARD;
                case "CASH_ON_DELIVERY" -> PaymentMethod.CASH_ON_DELIVERY;
                default -> throw new RandomExceptions("Unsupported payment method: " + provider);
            };
        }

        // Default to wallet if no payment intent
        log.info("No payment method specified, defaulting to WALLET");
        return PaymentMethod.WALLET;
    }

    // Routes to appropriate payment processor
    private PaymentResult routeToProcessor(
            CheckoutSessionEntity checkoutSession,
            PaymentMethod paymentMethod) throws ItemNotFoundException, RandomExceptions {

        //Todo: Here is where we can add more payment methods in the future
        return switch (paymentMethod) {
            case WALLET -> walletPaymentProcessor.processPayment(checkoutSession);
            case MPESA, TIGO_PESA, AIRTEL_MONEY, HALOPESA,
                 CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER ->
                    externalPaymentProcessor.processPayment(checkoutSession, paymentMethod);
            case CASH_ON_DELIVERY -> throw new RandomExceptions("COD payments handled separately");
        };
    }

    // Handles successful payment
    private PaymentResponse handleSuccessfulPayment(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result) {

        log.info("Payment successful for checkout session: {}",
                checkoutSession.getSessionId());

        // Update checkout session status
        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        checkoutSession.setCompletedAt(LocalDateTime.now());
        checkoutSession.setEscrowId(result.getEscrow().getId());

        checkoutSessionRepo.save(checkoutSession);

        //Todo: Publish notification event here to show payment success notification

        sendPaymentSuccessNotification(checkoutSession, result.getEscrow());

        // ========================================
        // INVOKE SUCCESS CALLBACK
        // ========================================
        try {
            paymentCallback.onPaymentSuccess(checkoutSession, result.getEscrow());
        } catch (Exception e) {
            log.error("Payment callback failed, but payment was successful", e);
        }

        // ========================================
        // PUBLISH EVENT FOR ORDER CREATION
        // ========================================
        try {
            PaymentCompletedEvent event = new PaymentCompletedEvent(
                    this,
                    checkoutSession.getSessionId(),
                    checkoutSession,
                    result.getEscrow().getId().toString(),  // or escrow ID
                    LocalDateTime.now()
            );

            eventPublisher.publishEvent(event);

            log.info("✓ PaymentCompletedEvent published");
            log.info("  Order creation will be handled asynchronously");

        } catch (Exception e) {
            log.error("Failed to publish PaymentCompletedEvent", e);
            // Don't throw - payment succeeded
        }

        // Save session
        checkoutSessionRepo.save(checkoutSession);

        // ========================================
        // BUILD RESPONSE (WITHOUT ORDER ID)
        // ========================================
        return PaymentResponse.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .message(buildSuccessMessage(checkoutSession.getSessionType()))
                .checkoutSessionId(checkoutSession.getSessionId())
                .escrowId(result.getEscrow().getId())
                .escrowNumber(result.getEscrow().getEscrowNumber())
                .orderId(null)  // Will be set later by listener
                .orderNumber(null)  // Will be set later
                .paymentMethod(PaymentMethod.WALLET)
                .amountPaid(result.getEscrow().getTotalAmount())
                .platformFee(result.getEscrow().getPlatformFeeAmount())
                .sellerAmount(result.getEscrow().getSellerAmount())
                .currency(result.getEscrow().getCurrency())
                .build();
    }


    // ========================================
    // HELPER METHOD: BUILD SUCCESS MESSAGE
    // ========================================

    private String buildSuccessMessage(CheckoutSessionType sessionType) {

        return switch (sessionType) {
            case REGULAR_DIRECTLY, REGULAR_CART -> "Payment completed successfully. Your order is being processed.";

            case INSTALLMENT -> "Down payment completed successfully. Order is being processed.";

            case GROUP_PURCHASE -> "Payment completed. Order will be created when group completes.";
        };
    }


    // Handles pending payment (for external payments)
    private PaymentResponse handlePendingPayment(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result) {

        log.info("Payment pending for checkout session: {}", checkoutSession.getSessionId());

        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_PROCESSING);
        checkoutSessionRepo.save(checkoutSession);

        // ========================================
        // INVOKE PENDING CALLBACK
        // ========================================
        try {
            paymentCallback.onPaymentPending(checkoutSession, result);
        } catch (Exception e) {
            log.error("Payment pending callback failed", e);
        }

        return PaymentResponse.builder()
                .success(false)
                .status(PaymentStatus.PENDING)
                .message(result.getMessage())
                .checkoutSessionId(checkoutSession.getSessionId())
                .paymentUrl(result.getPaymentUrl())
                .ussdCode(result.getUssdCode())
                .referenceNumber(result.getExternalReference())
                .build();
    }

    // Handles failed payment
    private PaymentResponse handleFailedPayment(
            CheckoutSessionEntity checkoutSession,
            PaymentResult result) {

        log.warn("Payment failed for checkout session: {} - Reason: {}",
                checkoutSession.getSessionId(), result.getMessage());

        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_FAILED);
        checkoutSessionRepo.save(checkoutSession);

        // ========================================
        // INVOKE FAILURE CALLBACK
        // ========================================
        try {
            paymentCallback.onPaymentFailure(
                    checkoutSession,
                    result,
                    result.getErrorMessage()
            );
        } catch (Exception e) {
            log.error("Payment failure callback failed", e);
        }

        return PaymentResponse.builder()
                .success(false)
                .status(PaymentStatus.FAILED)
                .message(result.getMessage())
                .checkoutSessionId(checkoutSession.getSessionId())
                .build();
    }

    /**
     * Send payment success notification to customer
     */
    private void sendPaymentSuccessNotification(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) {

        AccountEntity customer = checkoutSession.getCustomer();

        // 1. Prepare notification data using EventCategoryMapper
        Map<String, Object> data = PaymentNotificationMapper.mapPaymentReceived(
                customer.getFirstName(),
                customer.getEmail(),
                escrow.getTotalAmount(),
                escrow.getCurrency(),
                "WALLET",  // or checkoutSession.getPaymentMethod()
                escrow.getId().toString(),
                escrow.getEscrowNumber(),
                String.valueOf(checkoutSession.getSessionId())
        );

        // 2. Build recipient
        Recipient recipient = Recipient.builder()
                .userId(customer.getId().toString())
                .email(customer.getEmail())
                .phone(customer.getPhoneNumber())
                .name(customer.getFirstName())
                .language("en")
                .build();

        // 3. Create notification event
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

        // 4. Publish notification
        notificationPublisher.publish(event);

        log.info("📤 Payment success notification sent: user={}, amount={}, escrow={}",
                customer.getUserName(), escrow.getTotalAmount(), escrow.getEscrowNumber());

    }
}