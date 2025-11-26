package org.nextgate.nextgatebackend.e_commerce.installment_purchase.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InstallmentService {

    // ========================================
    // CORE OPERATIONS
    // ========================================

    InstallmentAgreementEntity createInstallmentAgreement(ProductCheckoutSessionEntity checkoutSession) throws ItemNotFoundException, BadRequestException;


    InstallmentPaymentEntity processInstallmentPayment(UUID paymentId) throws ItemNotFoundException, BadRequestException;


    void handleMissedPayment(UUID paymentId, String failureReason) throws ItemNotFoundException;

    BigDecimal calculateEarlyPayoffAmount(UUID agreementId) throws ItemNotFoundException;


    InstallmentAgreementEntity processEarlyPayoff(UUID agreementId) throws ItemNotFoundException, BadRequestException;

    // ========================================
    // QUERY OPERATIONS - AGREEMENTS
    // ========================================

    InstallmentAgreementEntity getAgreementById(UUID agreementId) throws ItemNotFoundException;

    InstallmentAgreementEntity getAgreementByNumber(String agreementNumber) throws ItemNotFoundException;


    List<InstallmentAgreementEntity> getMyAgreements(AccountEntity customer);


    List<InstallmentAgreementEntity> getMyAgreementsByStatus(AccountEntity customer, AgreementStatus status);

    InstallmentAgreementEntity getAgreementByCheckoutSession(UUID checkoutSessionId) throws ItemNotFoundException;

    // ========================================
    // QUERY OPERATIONS - PAYMENTS
    // ========================================

    List<InstallmentPaymentEntity> getAgreementPayments(UUID agreementId) throws ItemNotFoundException;

    InstallmentPaymentEntity getNextPayment(UUID agreementId) throws ItemNotFoundException;


    List<InstallmentPaymentEntity> getPaymentsDueToday();


    List<InstallmentPaymentEntity> getOverduePayments();

    // ========================================
    // VALIDATION OPERATIONS
    // ========================================


    boolean canMakeEarlyPayment(UUID agreementId);


    boolean canCancelAgreement(UUID agreementId);

    void handleAgreementCompletion(InstallmentAgreementEntity agreement);

}