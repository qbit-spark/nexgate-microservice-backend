package org.nextgate.nextgatebackend.installment_purchase.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.payloads.*;

import java.util.List;
import java.util.UUID;

public interface CustomerInstallmentService {

    List<InstallmentAgreementSummaryResponse> getMyAgreements(
            AccountEntity customer, AgreementStatus status);

    List<InstallmentAgreementSummaryResponse> getMyActiveAgreements(AccountEntity customer);

    InstallmentAgreementResponse getAgreementById(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    InstallmentAgreementResponse getAgreementByNumber(String agreementNumber, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    List<InstallmentPaymentResponse> getAgreementPayments(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    List<InstallmentPaymentResponse> getUpcomingPayments(AccountEntity customer);

    PaymentResponse makeManualPayment(UUID agreementId, UUID paymentId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    PaymentResponse retryPayment(UUID paymentId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    EarlyPayoffCalculation calculateEarlyPayoff(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    PaymentResponse processEarlyPayoff(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    void cancelAgreement(UUID agreementId, CancelAgreementRequest request, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;
}