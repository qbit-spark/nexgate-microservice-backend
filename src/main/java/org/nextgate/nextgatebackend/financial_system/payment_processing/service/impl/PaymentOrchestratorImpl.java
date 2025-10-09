package org.nextgate.nextgatebackend.financial_system.payment_processing.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
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
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID checkoutSessionId)
            throws ItemNotFoundException, RandomExceptions {

        PaymentRequest request = PaymentRequest.builder()
                .checkoutSessionId(checkoutSessionId)
                .build();

        return processPayment(request);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request)
            throws ItemNotFoundException, RandomExceptions {

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

        return switch (paymentMethod) {
            case WALLET -> walletPaymentProcessor.processPayment(checkoutSession);
            case MPESA, TIGO_PESA, AIRTEL_MONEY, HALOPESA,
                 CREDIT_CARD, DEBIT_CARD, BANK_TRANSFER ->
                    externalPaymentProcessor.processPayment(checkoutSession, paymentMethod);
            case CASH_ON_DELIVERY -> throw new RandomExceptions("COD payments handled separately");
        };
    }

    // Handles successful payment
    private PaymentResponse handleSuccessfulPayment(CheckoutSessionEntity checkoutSession, PaymentResult result) {

        log.info("Payment successful for checkout session: {}", checkoutSession.getSessionId());

        // Update checkout session status
        checkoutSession.setStatus(CheckoutSessionStatus.PAYMENT_COMPLETED);
        checkoutSession.setCompletedAt(LocalDateTime.now());

        // ========================================
        // INVOKE SUCCESS CALLBACK
        // ========================================
        try {
            paymentCallback.onPaymentSuccess(checkoutSession, result.getEscrow());
        } catch (Exception e) {
            log.error("Payment callback failed, but payment was successful", e);
            // Continue processing - callback failure shouldn't fail the payment
        }

        // Create order via OrderService (placeholder for now)
        UUID orderId = orderService.createOrderFromCheckoutSession(checkoutSession, result.getEscrow());

        if (orderId != null) {
            checkoutSession.setCreatedOrderId(orderId);
            checkoutSession.setStatus(CheckoutSessionStatus.COMPLETED);
            log.info("Order created: {}", orderId);
        }

        checkoutSessionRepo.save(checkoutSession);

        // Build response
        return PaymentResponse.builder()
                .success(true)
                .status(PaymentStatus.SUCCESS)
                .message("Payment completed successfully")
                .checkoutSessionId(checkoutSession.getSessionId())
                .escrowId(result.getEscrow().getId())
                .escrowNumber(result.getEscrow().getEscrowNumber())
                .orderId(orderId)
                .orderNumber(orderId != null ? "ORD-" + orderId.toString().substring(0, 8) : null)
                .paymentMethod(PaymentMethod.WALLET)
                .amountPaid(result.getEscrow().getTotalAmount())
                .platformFee(result.getEscrow().getPlatformFeeAmount())
                .sellerAmount(result.getEscrow().getSellerAmount())
                .currency(result.getEscrow().getCurrency())
                .build();
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
}