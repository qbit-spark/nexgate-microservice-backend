package org.nextgate.nextgatebackend.installment_purchase.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPaymentRepo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class InstallmentValidator {

    // ========================================
    // AGREEMENT CREATION VALIDATIONS
    // ========================================

    public void validateCheckoutSessionForAgreementCreation(
            CheckoutSessionEntity session,
            AccountEntity authenticatedUser
    ) throws BadRequestException {

        log.debug("Validating checkout session for agreement creation: {}",
                session.getSessionId());

        // 1. Validate session exists
        if (session == null) {
            throw new BadRequestException("Checkout session is null");
        }

        // 2. Validate session type
        if (session.getSessionType() != CheckoutSessionType.INSTALLMENT) {
            throw new BadRequestException(
                    String.format("Invalid session type: %s. Expected: INSTALLMENT",
                            session.getSessionType())
            );
        }

        // 3. Validate session status
        if (session.getStatus() != CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException(
                    String.format("Payment not completed. Session status: %s",
                            session.getStatus())
            );
        }

        // 4. Validate has customer
        if (session.getCustomer() == null) {
            throw new BadRequestException("Checkout session has no customer");
        }

        // 5. Validate authenticated user matches session customer
        if (!authenticatedUser.getAccountId().equals(session.getCustomer().getAccountId())) {
            throw new BadRequestException(
                    "Authenticated user does not match checkout session customer"
            );
        }

        // 6. Validate has installment plan ID
        if (session.getSelectedInstallmentPlanId() == null) {
            throw new BadRequestException("No installment plan selected in checkout session");
        }

        // 7. Validate has installment config
        if (session.getInstallmentConfig() == null) {
            throw new BadRequestException("No installment configuration in checkout session");
        }

        // 8. Validate has items
        if (session.getItems() == null || session.getItems().isEmpty()) {
            throw new BadRequestException("Checkout session has no items");
        }

        // 9. Validate exactly 1 item
        if (session.getItems().size() != 1) {
            throw new BadRequestException("INSTALLMENT must have exactly 1 item");
        }

        log.debug("Checkout session validation passed for agreement creation");
    }

    // ========================================
    // PAYMENT PROCESSING VALIDATIONS
    // ========================================

    public void validatePaymentCanBeProcessed(InstallmentPaymentEntity payment)
            throws BadRequestException {

        log.debug("Validating payment can be processed: {}", payment.getPaymentId());

        // 1. Validate payment exists
        if (payment == null) {
            throw new BadRequestException("Payment is null");
        }

        // 2. Validate payment is not already paid
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new BadRequestException("Payment is already completed");
        }

        // 3. Validate payment is not skipped
        if (payment.getPaymentStatus() == PaymentStatus.SKIPPED) {
            throw new BadRequestException("Payment has been skipped");
        }

        // 4. Validate payment is due or past due
        if (!payment.isDue() && payment.getPaymentStatus() == PaymentStatus.SCHEDULED) {
            throw new BadRequestException(
                    String.format("Payment is not due yet. Due date: %s",
                            payment.getDueDate())
            );
        }

        // 5. Validate associated agreement is active
        InstallmentAgreementEntity agreement = payment.getAgreement();
        if (agreement.getAgreementStatus() != AgreementStatus.ACTIVE &&
                agreement.getAgreementStatus() != AgreementStatus.PENDING_FIRST_PAYMENT) {
            throw new BadRequestException(
                    String.format("Agreement is not active. Status: %s",
                            agreement.getAgreementStatus())
            );
        }

        log.debug("Payment validation passed");
    }

    // ========================================
    // EARLY PAYOFF VALIDATIONS
    // ========================================

    public void validateCanMakeEarlyPayoff(InstallmentAgreementEntity agreement)
            throws BadRequestException {

        log.debug("Validating early payoff for agreement: {}", agreement.getAgreementId());

        // 1. Validate agreement is active
        if (agreement.getAgreementStatus() != AgreementStatus.ACTIVE) {
            throw new BadRequestException(
                    String.format("Agreement is not active. Status: %s",
                            agreement.getAgreementStatus())
            );
        }

        // 2. Validate has remaining payments
        if (agreement.getPaymentsRemaining() == 0) {
            throw new BadRequestException("No remaining payments to pay off");
        }

        // 3. Validate amount remaining is positive
        if (agreement.getAmountRemaining().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("No remaining balance to pay off");
        }

        log.debug("Early payoff validation passed");
    }

    // ========================================
    // AGREEMENT STATUS VALIDATIONS
    // ========================================

    public void validateAgreementIsActive(InstallmentAgreementEntity agreement)
            throws BadRequestException {

        if (agreement.getAgreementStatus() != AgreementStatus.ACTIVE &&
                agreement.getAgreementStatus() != AgreementStatus.PENDING_FIRST_PAYMENT) {
            throw new BadRequestException(
                    String.format("Agreement is not active. Status: %s",
                            agreement.getAgreementStatus())
            );
        }
    }

    public void validateAgreementNotDefaulted(InstallmentAgreementEntity agreement)
            throws BadRequestException {

        if (agreement.getAgreementStatus() == AgreementStatus.DEFAULTED) {
            throw new BadRequestException(
                    "Agreement is in default. Please contact support."
            );
        }
    }

    // NEW: Validate flexible payment request
    public void validateFlexiblePaymentRequest(
            InstallmentAgreementEntity agreement,
            BigDecimal requestedAmount
    ) throws BadRequestException {

        // 1. Agreement must be active
        if (agreement.getAgreementStatus() != AgreementStatus.ACTIVE &&
                agreement.getAgreementStatus() != AgreementStatus.PENDING_FIRST_PAYMENT) {
            throw new BadRequestException(
                    "Cannot make payment on inactive agreement. Status: " +
                            agreement.getAgreementStatus()
            );
        }

        // 2. Agreement must not be defaulted
        if (agreement.getAgreementStatus() == AgreementStatus.DEFAULTED) {
            throw new BadRequestException(
                    "Agreement is in default. Please contact support."
            );
        }

        // 3. Must have remaining balance
        if (agreement.getAmountRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Agreement is already fully paid"
            );
        }

        // 4. Amount must be positive
        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                    "Payment amount must be greater than zero"
            );
        }

        // 5. Amount cannot exceed remaining balance
        if (requestedAmount.compareTo(agreement.getAmountRemaining()) > 0) {
            throw new BadRequestException(
                    String.format(
                            "Payment amount (%s) exceeds remaining balance (%s). " +
                                    "Use early payoff endpoint if paying off completely.",
                            requestedAmount, agreement.getAmountRemaining()
                    )
            );
        }
    }

    // NEW: Get minimum payment required (next incomplete payment)
    public BigDecimal getMinimumPaymentRequired(
            InstallmentAgreementEntity agreement,
            InstallmentPaymentRepo paymentRepo
    ) {
        Optional<InstallmentPaymentEntity> nextPayment =
                paymentRepo.findNextIncompletePayment(agreement);

        if (nextPayment.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return nextPayment.get().getRemainingAmount();
    }
}