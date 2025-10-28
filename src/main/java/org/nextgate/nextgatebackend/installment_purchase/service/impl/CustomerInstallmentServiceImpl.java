package org.nextgate.nextgatebackend.installment_purchase.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.installment_purchase.payloads.*;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPaymentRepo;
import org.nextgate.nextgatebackend.installment_purchase.service.CustomerInstallmentService;
import org.nextgate.nextgatebackend.installment_purchase.service.InstallmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerInstallmentServiceImpl implements CustomerInstallmentService {

    private final InstallmentService installmentService;
    private final InstallmentAgreementRepo agreementRepo;
    private final InstallmentPaymentRepo paymentRepo;

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentAgreementSummaryResponse> getMyAgreements(
            AccountEntity customer, AgreementStatus status) {

        log.info("Fetching agreements for customer: {}", customer.getAccountId());

        List<InstallmentAgreementEntity> agreements;

        if (status != null) {
            agreements = installmentService.getMyAgreementsByStatus(customer, status);
        } else {
            agreements = installmentService.getMyAgreements(customer);
        }

        return agreements.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentAgreementSummaryResponse> getMyActiveAgreements(AccountEntity customer) {
        return getMyAgreements(customer, AgreementStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentAgreementResponse getAgreementById(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Fetching agreement: {} for customer: {}", agreementId, customer.getAccountId());

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        return toDetailedResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentAgreementResponse getAgreementByNumber(
            String agreementNumber, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Fetching agreement: {} for customer: {}", agreementNumber, customer.getAccountId());

        InstallmentAgreementEntity agreement = installmentService.getAgreementByNumber(agreementNumber);
        validateAgreementOwnership(agreement, customer);

        return toDetailedResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentDetailResponse> getAgreementPayments(
            UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Fetching payments for agreement: {}", agreementId);

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        List<InstallmentPaymentEntity> payments = installmentService.getAgreementPayments(agreementId);

        return payments.stream()
                .map(this::toPaymentDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentDetailResponse> getUpcomingPayments(AccountEntity customer) {

        log.info("Fetching upcoming payments for customer: {}", customer.getAccountId());

        List<InstallmentAgreementEntity> activeAgreements =
                installmentService.getMyAgreementsByStatus(customer, AgreementStatus.ACTIVE);

        return activeAgreements.stream()
                .filter(agreement -> agreement.getNextPaymentDate() != null)
                .flatMap(agreement -> {
                    try {
                        return installmentService.getAgreementPayments(agreement.getAgreementId())
                                .stream()
                                .filter(payment -> payment.getPaymentStatus() == PaymentStatus.SCHEDULED ||
                                        payment.getPaymentStatus() == PaymentStatus.PENDING)
                                .limit(1);
                    } catch (ItemNotFoundException e) {
                        return List.<InstallmentPaymentEntity>of().stream();
                    }
                })
                .map(this::toPaymentDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProcessPaymentResponse makeManualPayment(
            UUID agreementId, UUID paymentId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Processing manual payment: {} for agreement: {}", paymentId, agreementId);

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        InstallmentPaymentEntity payment = installmentService.processInstallmentPayment(paymentId);
        InstallmentAgreementEntity updatedAgreement = installmentService.getAgreementById(agreementId);

        return ProcessPaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .agreementId(updatedAgreement.getAgreementId())
                .agreementNumber(updatedAgreement.getAgreementNumber())
                .amount(payment.getPaidAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .status(payment.getPaymentStatus().name())
                .processedAt(payment.getPaidAt())
                .message("Payment processed successfully")
                .agreementUpdate(buildAgreementUpdate(updatedAgreement))
                .build();
    }

    @Override
    @Transactional
    public ProcessPaymentResponse retryPayment(UUID paymentId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Retrying payment: {}", paymentId);

        InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ItemNotFoundException("Payment not found"));

        InstallmentAgreementEntity agreement = payment.getAgreement();
        validateAgreementOwnership(agreement, customer);

        if (!payment.canRetry()) {
            throw new BadRequestException("Payment cannot be retried");
        }

        InstallmentPaymentEntity processedPayment = installmentService.processInstallmentPayment(paymentId);
        InstallmentAgreementEntity updatedAgreement = installmentService.getAgreementById(agreement.getAgreementId());

        return ProcessPaymentResponse.builder()
                .paymentId(processedPayment.getPaymentId())
                .agreementId(updatedAgreement.getAgreementId())
                .agreementNumber(updatedAgreement.getAgreementNumber())
                .amount(processedPayment.getPaidAmount())
                .currency(processedPayment.getCurrency())
                .paymentMethod(processedPayment.getPaymentMethod())
                .transactionId(processedPayment.getTransactionId())
                .status(processedPayment.getPaymentStatus().name())
                .processedAt(processedPayment.getPaidAt())
                .message("Payment retry successful")
                .agreementUpdate(buildAgreementUpdate(updatedAgreement))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EarlyPayoffCalculation calculateEarlyPayoff(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Calculating early payoff for agreement: {}", agreementId);

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        if (!installmentService.canMakeEarlyPayment(agreementId)) {
            throw new BadRequestException("Early payoff not available for this agreement");
        }

        BigDecimal payoffAmount = installmentService.calculateEarlyPayoffAmount(agreementId);

        List<InstallmentPaymentEntity> remainingPayments = paymentRepo
                .findByAgreementAndPaymentStatusOrderByPaymentNumberAsc(agreement, PaymentStatus.SCHEDULED);

        BigDecimal remainingPrincipal = remainingPayments.stream()
                .map(InstallmentPaymentEntity::getPrincipalPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingInterest = remainingPayments.stream()
                .map(InstallmentPaymentEntity::getInterestPortion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestRebate = remainingInterest.multiply(BigDecimal.valueOf(0.75));

        return EarlyPayoffCalculation.builder()
                .agreementId(agreement.getAgreementId())
                .paymentsCompleted(agreement.getPaymentsCompleted())
                .paymentsRemaining(agreement.getPaymentsRemaining())
                .amountPaid(agreement.getAmountPaid())
                .remainingPrincipal(remainingPrincipal)
                .unaccruedInterest(remainingInterest)
                .interestRebate(interestRebate)
                .payoffWithRebate(payoffAmount)
                .payoffWithoutRebate(agreement.getAmountRemaining())
                .savingsVsScheduled(agreement.getAmountRemaining().subtract(payoffAmount))
                .rebatePolicy("75% discount on remaining interest for early payoff")
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public ProcessPaymentResponse processEarlyPayoff(UUID agreementId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Processing early payoff for agreement: {}", agreementId);

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        BigDecimal payoffAmount = installmentService.calculateEarlyPayoffAmount(agreementId);
        InstallmentAgreementEntity completedAgreement = installmentService.processEarlyPayoff(agreementId);

        return ProcessPaymentResponse.builder()
                .agreementId(completedAgreement.getAgreementId())
                .agreementNumber(completedAgreement.getAgreementNumber())
                .amount(payoffAmount)
                .currency(completedAgreement.getCurrency())
                .paymentMethod("WALLET")
                .status("COMPLETED")
                .processedAt(completedAgreement.getCompletedAt())
                .message("Early payoff processed successfully")
                .agreementUpdate(buildAgreementUpdate(completedAgreement))
                .build();
    }

    @Override
    @Transactional
    public void cancelAgreement(UUID agreementId, CancelAgreementRequest request, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Cancelling agreement: {}", agreementId);

        InstallmentAgreementEntity agreement = installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        if (!installmentService.canCancelAgreement(agreementId)) {
            throw new BadRequestException(
                    "Agreement cannot be cancelled. Only agreements with no completed payments can be cancelled.");
        }

        agreement.setAgreementStatus(AgreementStatus.CANCELLED);
        agreement.setDeleteReason(request.getReason());
        agreement.setUpdatedAt(LocalDateTime.now());
        agreementRepo.save(agreement);

        log.info("Agreement {} cancelled successfully", agreementId);
    }

    private void validateAgreementOwnership(InstallmentAgreementEntity agreement, AccountEntity customer)
            throws BadRequestException {

        if (!agreement.getCustomer().getAccountId().equals(customer.getAccountId())) {
            throw new BadRequestException("You do not have access to this agreement");
        }
    }

    private InstallmentAgreementSummaryResponse toSummaryResponse(InstallmentAgreementEntity agreement) {

        return InstallmentAgreementSummaryResponse.builder()
                .agreementId(agreement.getAgreementId())
                .agreementNumber(agreement.getAgreementNumber())
                .productId(agreement.getProduct().getProductId())
                .productName(agreement.getProductName())
                .productImage(agreement.getProductImage())
                .shopId(agreement.getShop().getShopId())
                .shopName(agreement.getShop().getShopName())
                .totalAmount(agreement.getTotalAmount())
                .amountPaid(agreement.getAmountPaid())
                .amountRemaining(agreement.getAmountRemaining())
                .currency(agreement.getCurrency())
                .paymentsCompleted(agreement.getPaymentsCompleted())
                .paymentsRemaining(agreement.getPaymentsRemaining())
                .totalPayments(agreement.getNumberOfPayments())
                .progressPercentage(agreement.getProgressPercentage().doubleValue())
                .nextPaymentDate(agreement.getNextPaymentDate())
                .nextPaymentAmount(agreement.getNextPaymentAmount())
                .agreementStatus(agreement.getAgreementStatus())
                .agreementStatusDisplay(agreement.getAgreementStatus().name())
                .createdAt(agreement.getCreatedAt())
                .completedAt(agreement.getCompletedAt())
                .canMakeEarlyPayment(installmentService.canMakeEarlyPayment(agreement.getAgreementId()))
                .canCancel(installmentService.canCancelAgreement(agreement.getAgreementId()))
                .build();
    }

    private InstallmentAgreementResponse toDetailedResponse(InstallmentAgreementEntity agreement) {

        InstallmentAgreementResponse response = new InstallmentAgreementResponse();
        response.setAgreementId(agreement.getAgreementId());
        response.setAgreementNumber(agreement.getAgreementNumber());
        response.setCustomerId(agreement.getCustomer().getAccountId());
        response.setCustomerName(agreement.getCustomer().getUserName());
        response.setCustomerEmail(agreement.getCustomer().getEmail());
        response.setProductId(agreement.getProduct().getProductId());
        response.setProductName(agreement.getProductName());
        response.setProductImage(agreement.getProductImage());
        response.setProductPrice(agreement.getProductPrice());
        response.setQuantity(agreement.getQuantity());
        response.setShopId(agreement.getShop().getShopId());
        response.setShopName(agreement.getShop().getShopName());
        response.setSelectedPlanId(agreement.getSelectedPlan().getPlanId());
        response.setPlanName(agreement.getSelectedPlan().getPlanName());
        response.setPaymentFrequency(agreement.getPaymentFrequency());
        response.setPaymentFrequencyDisplay(agreement.getPaymentFrequency().getDisplayName());
        response.setCustomFrequencyDays(agreement.getCustomFrequencyDays());
        response.setNumberOfPayments(agreement.getNumberOfPayments());
        response.setDuration(agreement.getSelectedPlan().getCalculatedDurationDisplay());
        response.setApr(agreement.getApr());
        response.setPaymentStartDelayDays(agreement.getPaymentStartDelayDays());
        response.setDownPaymentAmount(agreement.getDownPaymentAmount());
        response.setFinancedAmount(agreement.getFinancedAmount());
        response.setMonthlyPaymentAmount(agreement.getMonthlyPaymentAmount());
        response.setTotalInterestAmount(agreement.getTotalInterestAmount());
        response.setTotalAmount(agreement.getTotalAmount());
        response.setCurrency(agreement.getCurrency());
        response.setPaymentsCompleted(agreement.getPaymentsCompleted());
        response.setPaymentsRemaining(agreement.getPaymentsRemaining());
        response.setAmountPaid(agreement.getAmountPaid());
        response.setAmountRemaining(agreement.getAmountRemaining());
        response.setProgressPercentage(agreement.getProgressPercentage().doubleValue());
        response.setNextPaymentDate(agreement.getNextPaymentDate());
        response.setNextPaymentAmount(agreement.getNextPaymentAmount());
        response.setAgreementStatus(agreement.getAgreementStatus());
        response.setDefaultCount(agreement.getDefaultCount());
        response.setCreatedAt(agreement.getCreatedAt());
        response.setFirstPaymentDate(agreement.getFirstPaymentDate());
        response.setLastPaymentDate(agreement.getLastPaymentDate());
        response.setCompletedAt(agreement.getCompletedAt());
        response.setFulfillmentTiming(agreement.getFulfillmentTiming());
        response.setShippedAt(agreement.getShippedAt());
        response.setDeliveredAt(agreement.getDeliveredAt());
        response.setOrderId(agreement.getOrderId());
        response.setCanMakeEarlyPayment(installmentService.canMakeEarlyPayment(agreement.getAgreementId()));
        response.setCanCancel(installmentService.canCancelAgreement(agreement.getAgreementId()));
        response.setCanUpdatePaymentMethod(false);

        return response;
    }

    private InstallmentPaymentDetailResponse toPaymentDetailResponse(InstallmentPaymentEntity payment) {

        InstallmentPaymentDetailResponse response = InstallmentPaymentDetailResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentNumber(payment.getPaymentNumber())
                .scheduledAmount(payment.getScheduledAmount())
                .paidAmount(payment.getPaidAmount())
                .principalPortion(payment.getPrincipalPortion())
                .interestPortion(payment.getInterestPortion())
                .remainingBalance(payment.getRemainingBalance())
                .lateFee(payment.getLateFee())
                .currency(payment.getCurrency())
                .paymentStatus(payment.getPaymentStatus())
                .paymentStatusDisplay(payment.getPaymentStatus().name())
                .dueDate(payment.getDueDate())
                .paidAt(payment.getPaidAt())
                .attemptedAt(payment.getAttemptedAt())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .retryCount(payment.getRetryCount())
                .canPay(!payment.isPaid() && payment.isDue())
                .canRetry(payment.canRetry())
                .build();

        if (payment.getDueDate() != null) {
            long daysUntil = java.time.Duration.between(LocalDateTime.now(), payment.getDueDate()).toDays();
            response.setDaysUntilDue((int) daysUntil);
        }

        if (payment.isOverdue()) {
            response.setDaysOverdue(payment.getDaysOverdue());
        }

        return response;
    }

    private ProcessPaymentResponse.AgreementUpdate buildAgreementUpdate(InstallmentAgreementEntity agreement) {

        return ProcessPaymentResponse.AgreementUpdate.builder()
                .paymentsCompleted(agreement.getPaymentsCompleted())
                .paymentsRemaining(agreement.getPaymentsRemaining())
                .amountPaid(agreement.getAmountPaid())
                .amountRemaining(agreement.getAmountRemaining())
                .nextPaymentDate(agreement.getNextPaymentDate())
                .nextPaymentAmount(agreement.getNextPaymentAmount())
                .agreementStatus(agreement.getAgreementStatus().name())
                .isCompleted(agreement.isCompleted())
                .build();
    }
}