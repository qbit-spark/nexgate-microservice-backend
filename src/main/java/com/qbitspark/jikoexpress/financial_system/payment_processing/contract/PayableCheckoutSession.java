package com.qbitspark.jikoexpress.financial_system.payment_processing.contract;



import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment contract interface for all checkout session types.
 * This allows the payment system to be agnostic to the session domain (Product, Event, etc.)
 *
 * Any entity that wants to be payable must implement this interface.
 */
public interface PayableCheckoutSession {

    // ========================================
    // IDENTIFICATION
    // ========================================

    /**
     * Unique session identifier
     */
    UUID getSessionId();

    /**
     * Domain identifier: "PRODUCT", "EVENT", "SUBSCRIPTION", etc.
     * Used by strategy pattern to route domain-specific logic
     */
    String getSessionDomain();

    // ========================================
    // PAYMENT PARTICIPANTS (Money Flow)
    // ========================================

    /**
     * The payer (source of funds)
     * - In Product checkout: The customer buying products
     * - In Event checkout: The attendee buying tickets
     *
     * Money flows FROM this account
     */
    AccountEntity getPayer();

    /**
     * The payee (destination of funds) - extracted via strategy pattern
     * Note: This is NOT stored in the session entity directly.
     * It's derived based on session domain:
     * - Product: Shop owner (from items)
     * - Event: Event organizer (from event)
     *
     * This method signature exists for documentation purposes.
     * Actual extraction is done by SessionMetadataExtractor strategy.
     *
     * Money flows TO this account (after platform fee deduction)
     */
    // AccountEntity getPayee();  // ← Removed from interface, handled by strategy

    // ========================================
    // PAYMENT AMOUNT
    // ========================================

    /**
     * Total amount to be paid (after all calculations)
     */
    BigDecimal getTotalAmount();

    /**
     * Currency code (e.g., "TZS", "USD")
     */
    String getCurrency();

    // ========================================
    // SESSION STATE
    // ========================================

    /**
     * Current status of the checkout session
     */
    CheckoutSessionStatus getStatus();

    /**
     * Update session status
     */
    void setStatus(CheckoutSessionStatus status);

    /**
     * When the session expires (if not completed)
     */
    LocalDateTime getExpiresAt();

    /**
     * Check if session has expired
     */
    boolean isExpired();

    /**
     * When the session was completed (payment successful)
     */
    LocalDateTime getCompletedAt();

    /**
     * Set completion timestamp
     */
    void setCompletedAt(LocalDateTime completedAt);

    // ========================================
    // PAYMENT RESULT TRACKING
    // ========================================

    /**
     * Set escrow ID after successful payment
     */
    void setEscrowId(UUID escrowId);

    /**
     * Get escrow ID if payment was successful
     */
    UUID getEscrowId();

    /**
     * Get payment attempt count
     */
    int getPaymentAttemptCount();

    /**
     * Check if payment can be retried
     */
    boolean canRetryPayment();
}