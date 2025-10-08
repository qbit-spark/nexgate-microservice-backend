package org.nextgate.nextgatebackend.installment_purchase.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InstallmentService {

    // ========================================
    // CORE OPERATIONS
    // ========================================

    /**
     * Create installment agreement after down payment is completed
     * Called by PaymentOrchestrator after checkout payment succeeds
     */
    InstallmentAgreementEntity createInstallmentAgreement(
            CheckoutSessionEntity checkoutSession
    ) throws ItemNotFoundException, BadRequestException;

//    /**
//     * Process a scheduled installment payment
//     * Called by scheduled job or manual payment
//     */
    InstallmentPaymentEntity processInstallmentPayment(
            UUID paymentId
    ) throws ItemNotFoundException, BadRequestException;

//    /**
//     * Handle missed/failed payment
//     */
    void handleMissedPayment(
            UUID paymentId,
            String failureReason
    ) throws ItemNotFoundException;



//    /**
//     * Calculate early payoff amount
//     */
    BigDecimal calculateEarlyPayoffAmount(
            UUID agreementId
    ) throws ItemNotFoundException;


//    /**
//     * Process early payoff
//     */
    InstallmentAgreementEntity processEarlyPayoff(
            UUID agreementId
    ) throws ItemNotFoundException, BadRequestException;
//
//    // ========================================
//    // QUERY OPERATIONS - AGREEMENTS
//    // ========================================
//
//    /**
//     * Get agreement by ID
//     */
    InstallmentAgreementEntity getAgreementById(
            UUID agreementId
    ) throws ItemNotFoundException;
//
//    /**
//     * Get agreement by agreement number
//     */
    InstallmentAgreementEntity getAgreementByNumber(
            String agreementNumber
    ) throws ItemNotFoundException;

//    /**
//     * Get customer's agreements
//     */
    List<InstallmentAgreementEntity> getMyAgreements(
            AccountEntity customer
    );

//
//    /**
//     * Get customer's agreements by status
//     */
    List<InstallmentAgreementEntity> getMyAgreementsByStatus(
            AccountEntity customer,
            AgreementStatus status
    );


//    /**
//     * Get agreement by checkout session
//     */
    InstallmentAgreementEntity getAgreementByCheckoutSession(
            UUID checkoutSessionId
    ) throws ItemNotFoundException;

//
//    // ========================================
//    // QUERY OPERATIONS - PAYMENTS
//    // ========================================
//
//    /**
//     * Get all payments for an agreement
//     */
    List<InstallmentPaymentEntity> getAgreementPayments(
            UUID agreementId
    ) throws ItemNotFoundException;


//    /**
//     * Get next payment for agreement
//     */
    InstallmentPaymentEntity getNextPayment(
            UUID agreementId
    ) throws ItemNotFoundException;

//    /**
//     * Get payments due today
//     */
    List<InstallmentPaymentEntity> getPaymentsDueToday();

//    /**
//     * Get overdue payments
//     */
    List<InstallmentPaymentEntity> getOverduePayments();
//
//    // ========================================
//    // VALIDATION OPERATIONS
//    // ========================================
//
//    /**
//     * Check if customer can make early payment
//     */
    boolean canMakeEarlyPayment(UUID agreementId);
//
//    /**
//     * Check if agreement can be cancelled
//     */
    boolean canCancelAgreement(UUID agreementId);

}