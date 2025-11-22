package org.nextgate.nextgatebackend.e_commerce.installment_purchase.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.*;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;

import java.util.List;
import java.util.UUID;

public interface CustomerInstallmentService {

    List<InstallmentAgreementSummaryResponse> getMyAgreements(
            AccountEntity customer,
            AgreementStatus status);

    List<InstallmentAgreementSummaryResponse> getMyActiveAgreements(
            AccountEntity customer);

    InstallmentAgreementResponse getAgreementById(
            UUID agreementId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    InstallmentAgreementResponse getAgreementByNumber(
            String agreementNumber,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    List<InstallmentPaymentDetailResponse> getAgreementPayments(
            UUID agreementId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    List<InstallmentPaymentDetailResponse> getUpcomingPayments(
            AccountEntity customer);

    ProcessPaymentResponse makeManualPayment(
            UUID agreementId,
            UUID paymentId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    ProcessPaymentResponse retryPayment(
            UUID paymentId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    EarlyPayoffCalculation calculateEarlyPayoff(
            UUID agreementId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    ProcessPaymentResponse processEarlyPayoff(
            UUID agreementId,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;

    void cancelAgreement(
            UUID agreementId,
            CancelAgreementRequest request,
            AccountEntity customer)
            throws ItemNotFoundException, BadRequestException;


    FlexiblePaymentPreviewResponse previewFlexiblePayment(
            UUID agreementId,
            FlexiblePaymentPreviewRequest request,
            AccountEntity customer
    ) throws ItemNotFoundException, BadRequestException;

    // : Process flexible payment
    FlexiblePaymentResponse makeFlexiblePayment(
            UUID agreementId,
            FlexiblePaymentRequest request,
            AccountEntity customer
    ) throws ItemNotFoundException, BadRequestException;
}