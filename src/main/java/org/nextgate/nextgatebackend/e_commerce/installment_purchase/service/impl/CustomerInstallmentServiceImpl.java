package org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.*;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerEntryEntity;
import org.nextgate.nextgatebackend.financial_system.ledger.enums.LedgerEntryType;
import org.nextgate.nextgatebackend.financial_system.ledger.service.LedgerService;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionDirection;
import org.nextgate.nextgatebackend.financial_system.transaction_history.enums.TransactionType;
import org.nextgate.nextgatebackend.financial_system.transaction_history.service.TransactionHistoryService;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.listeners.InstallmentNotificationListener;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentPaymentRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.CustomerInstallmentService;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.InstallmentService;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.utils.InstallmentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerInstallmentServiceImpl implements CustomerInstallmentService {

    private final InstallmentService installmentService;
    private final InstallmentAgreementRepo agreementRepo;
    private final InstallmentPaymentRepo paymentRepo;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final TransactionHistoryService transactionHistoryService;
    private final InstallmentValidator validator;
    private final InstallmentNotificationListener notificationListener;

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




    // NEW: Preview flexible payment
    @Override
    @Transactional(readOnly = true)
    public FlexiblePaymentPreviewResponse previewFlexiblePayment(UUID agreementId, FlexiblePaymentPreviewRequest request, AccountEntity customer) throws ItemNotFoundException, BadRequestException {

        log.info("Previewing flexible payment: {} TZS for agreement: {}",
                request.getAmount(), agreementId);

        // 1. Get and validate agreement
        InstallmentAgreementEntity agreement =
                installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        // 2. Validate flexible payment
        validator.validateFlexiblePaymentRequest(agreement, request.getAmount());

        // 3. Get minimum and maximum
        BigDecimal minimumRequired = validator.getMinimumPaymentRequired(
                agreement, paymentRepo
        );
        BigDecimal maximumAllowed = agreement.getAmountRemaining();

        // 4. Check if requested amount is valid
        boolean isValid = request.getAmount().compareTo(minimumRequired) >= 0 &&
                request.getAmount().compareTo(maximumAllowed) <= 0;

        String validationMessage = null;
        if (!isValid) {
            if (request.getAmount().compareTo(minimumRequired) < 0) {
                validationMessage = String.format(
                        "Minimum payment required: %s TZS", minimumRequired
                );
            } else {
                validationMessage = String.format(
                        "Maximum payment allowed: %s TZS (use early payoff for full amount)",
                        maximumAllowed
                );
            }
        }

        // 5. Simulate payment distribution
        List<InstallmentPaymentEntity> unpaidPayments =
                paymentRepo.findUnpaidPaymentsInOrder(agreement);

        List<FlexiblePaymentPreviewResponse.PaymentImpactPreview> impacts =
                simulatePaymentDistribution(unpaidPayments, request.getAmount());

        // 6. Calculate summary
        int paymentsWillComplete = (int) impacts.stream()
                .filter(p -> "Will be COMPLETED".equals(p.getResultStatus()))
                .count();

        int paymentsWillBePartial = (int) impacts.stream()
                .filter(p -> "Will be PARTIALLY_PAID".equals(p.getResultStatus()))
                .count();

        BigDecimal remainingAfter = agreement.getAmountRemaining()
                .subtract(request.getAmount());

        // 7. Build response
        return FlexiblePaymentPreviewResponse.builder()
                .requestedAmount(request.getAmount())
                .minimumRequired(minimumRequired)
                .maximumAllowed(maximumAllowed)
                .isValid(isValid)
                .validationMessage(validationMessage)
                .impactedPayments(impacts)
                .paymentsWillComplete(paymentsWillComplete)
                .paymentsWillBePartial(paymentsWillBePartial)
                .remainingAfter(remainingAfter)
                .build();
    }


    // NEW: Make flexible payment
    @Override
    @Transactional
    public FlexiblePaymentResponse makeFlexiblePayment(
            UUID agreementId,
            FlexiblePaymentRequest request,
            AccountEntity customer
    ) throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         PROCESSING FLEXIBLE PAYMENT                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement: {}, Amount: {} TZS", agreementId, request.getAmount());

        // 1. Get and validate agreement
        InstallmentAgreementEntity agreement =
                installmentService.getAgreementById(agreementId);
        validateAgreementOwnership(agreement, customer);

        // 2. Validate payment
        validator.validateFlexiblePaymentRequest(agreement, request.getAmount());

        // 3. Check wallet balance
        WalletEntity customerWallet = walletService.getWalletByAccountId(
                customer.getAccountId()
        );

        if (!customerWallet.getIsActive()) {
            throw new BadRequestException("Wallet is not active");
        }

        BigDecimal walletBalance = walletService.getWalletBalance(customerWallet);

        if (walletBalance.compareTo(request.getAmount()) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient wallet balance. Required: %s TZS, Available: %s TZS",
                    request.getAmount(), walletBalance
            ));
        }

        log.info("✓ Wallet balance sufficient: {} TZS", walletBalance);

        // 4. Execute payment via ledger (single transaction)
        log.info("Creating ledger entry for flexible payment...");

        LedgerAccountEntity walletLedger =
                ledgerService.getOrCreateWalletAccount(customerWallet);
        LedgerAccountEntity platformRevenue =
                ledgerService.getPlatformRevenueAccount();

        String description = String.format(
                "Flexible payment for agreement %s (%s) - Amount: %s TZS%s",
                agreement.getAgreementNumber(),
                agreement.getProductName(),
                request.getAmount(),
                request.getNote() != null ? " - " + request.getNote() : ""
        );

        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                walletLedger,
                platformRevenue,
                request.getAmount(),
                LedgerEntryType.INSTALLMENT_PAYMENT,
                "INSTALLMENT_FLEXIBLE_PAYMENT",
                agreement.getAgreementId(),
                description,
                customer
        );

        log.info("✓ Ledger entry created: {}", ledgerEntry.getEntryNumber());

        // 5. Create a transaction history
        transactionHistoryService.createTransaction(
                customer,
                TransactionType.INSTALLMENT_PAYMENT,
                TransactionDirection.DEBIT,
                request.getAmount(),
                "Flexible Installment Payment",
                description,
                ledgerEntry.getId(),
                "INSTALLMENT_AGREEMENT",
                agreement.getAgreementId()
        );

        log.info("✓ Transaction history created");

        // 6. Distribute payment to installments
        List<FlexiblePaymentResponse.PaymentApplicationDetail> applications =
                distributePaymentToInstallments(
                        agreement,
                        request.getAmount(),
                        ledgerEntry.getEntryNumber()
                );

        // 7. Update agreement tracking
        updateAgreementAfterFlexiblePayment(agreement);

        // 8. Check if the agreement is completed
        if (agreement.getPaymentsRemaining() == 0) {

            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║  🎉 FINAL PAYMENT COMPLETED - AGREEMENT FULLY PAID! 🎉     ║");
            log.info("╚════════════════════════════════════════════════════════════╝");
            log.info("Agreement Number: {}", agreement.getAgreementNumber());
            log.info("Product: {}", agreement.getProductName());
            log.info("Total Paid: {} TZS", String.format("%,.2f", agreement.getAmountPaid()));
            log.info("Total Payments: {}", agreement.getNumberOfPayments());


            //Todo: Here is critical place bro.. we handle when installment is completed depend in fulfilment (IMMEDIATE, OR AFTER_FIRST_PAYMENT)
            installmentService.handleAgreementCompletion(agreement);

        }else {
            log.info("Agreement Progress:");
            log.info("  Status: IN PROGRESS");
            log.info("  Completed: {} of {} payments",
                    agreement.getPaymentsCompleted(),
                    agreement.getNumberOfPayments());
            log.info("  Remaining: {} payments ({} TZS)",
                    agreement.getPaymentsRemaining(),
                    String.format("%,.2f", agreement.getAmountRemaining()));
            log.info("  Progress: {}%",
                    String.format("%.1f", agreement.getProgressPercentage()));
        }


        // ========================================
        // SEND SUCCESS NOTIFICATIONS
        // ========================================
        log.info("Triggering notifications...");

        // TODO: Integrate with NotificationService
        // notificationService.sendPaymentSuccessNotification(
        //     customer,
        //     agreement,
        //     payment
        // );


        // 9. Build response
        String message = buildPaymentSuccessMessage(applications);

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         FLEXIBLE PAYMENT COMPLETE ✓                        ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info(message);

        return FlexiblePaymentResponse.builder()
                .agreementId(agreement.getAgreementId())
                .agreementNumber(agreement.getAgreementNumber())
                .totalAmountPaid(request.getAmount())
                .currency(agreement.getCurrency())
                .transactionId(ledgerEntry.getEntryNumber())
                .processedAt(LocalDateTime.now())
                .paymentsAffected(applications)
                .agreementUpdate(buildAgreementUpdateSummary(agreement))
                .message(message)
                .build();
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

    // HELPER: Simulate payment distribution (for preview)
    private List<FlexiblePaymentPreviewResponse.PaymentImpactPreview>
    simulatePaymentDistribution(
            List<InstallmentPaymentEntity> unpaidPayments,
            BigDecimal amount
    ) {
        List<FlexiblePaymentPreviewResponse.PaymentImpactPreview> impacts =
                new ArrayList<>();

        BigDecimal remaining = amount;

        for (InstallmentPaymentEntity payment : unpaidPayments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal currentPaid = payment.getPaidAmount() != null ?
                    payment.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal amountNeeded = payment.getScheduledAmount()
                    .subtract(currentPaid);
            BigDecimal willApply = remaining.min(amountNeeded);
            BigDecimal willRemain = amountNeeded.subtract(willApply);

            String resultStatus = willRemain.compareTo(BigDecimal.ZERO) == 0 ?
                    "Will be COMPLETED" : "Will be PARTIALLY_PAID";

            impacts.add(
                    FlexiblePaymentPreviewResponse.PaymentImpactPreview.builder()
                            .paymentNumber(payment.getPaymentNumber())
                            .dueDate(payment.getDueDate())
                            .scheduledAmount(payment.getScheduledAmount())
                            .currentPaid(currentPaid)
                            .willApply(willApply)
                            .willRemain(willRemain)
                            .resultStatus(resultStatus)
                            .build()
            );

            remaining = remaining.subtract(willApply);
        }

        return impacts;
    }

    // HELPER: Actually distribute payment to installments
    private List<FlexiblePaymentResponse.PaymentApplicationDetail>
    distributePaymentToInstallments(
            InstallmentAgreementEntity agreement,
            BigDecimal totalAmount,
            String transactionId
    ) {
        log.info("Distributing {} TZS across installments...", totalAmount);

        List<FlexiblePaymentResponse.PaymentApplicationDetail> applications =
                new ArrayList<>();

        List<InstallmentPaymentEntity> unpaidPayments =
                paymentRepo.findUnpaidPaymentsInOrder(agreement);

        BigDecimal remaining = totalAmount;

        for (InstallmentPaymentEntity payment : unpaidPayments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal previousPaid = payment.getPaidAmount() != null ?
                    payment.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal amountNeeded = payment.getRemainingAmount();
            BigDecimal amountToApply = remaining.min(amountNeeded);

            // Apply payment
            payment.recordPartialPayment(amountToApply, transactionId, "WALLET");

            boolean wasCompleted = payment.getPaymentStatus() == PaymentStatus.COMPLETED;

            // Update agreement if payment completed
            if (wasCompleted) {
                agreement.recordPayment(payment.getScheduledAmount());
                log.info("  ✓ Payment #{} COMPLETED (applied {} TZS)",
                        payment.getPaymentNumber(), amountToApply);
            } else {
                log.info("  ◐ Payment #{} PARTIALLY_PAID (applied {} TZS, {} TZS remaining)",
                        payment.getPaymentNumber(), amountToApply,
                        payment.getRemainingAmount());
            }

            paymentRepo.save(payment);

            // Record application
            applications.add(
                    FlexiblePaymentResponse.PaymentApplicationDetail.builder()
                            .paymentId(payment.getPaymentId())
                            .paymentNumber(payment.getPaymentNumber())
                            .dueDate(payment.getDueDate())
                            .scheduledAmount(payment.getScheduledAmount())
                            .amountApplied(amountToApply)
                            .previouslyPaid(previousPaid)
                            .newPaidAmount(payment.getPaidAmount())
                            .remaining(payment.getRemainingAmount())
                            .status(payment.getPaymentStatus().name())
                            .wasCompleted(wasCompleted)
                            .build()
            );

            remaining = remaining.subtract(amountToApply);
        }

        log.info("✓ Distribution complete - {} payments affected", applications.size());

        return applications;
    }

    // HELPER: Update agreement after flexible payment
    private void updateAgreementAfterFlexiblePayment(
            InstallmentAgreementEntity agreement
    ) {
        // Update next payment info
        Optional<InstallmentPaymentEntity> nextIncomplete =
                paymentRepo.findNextIncompletePayment(agreement);

        if (nextIncomplete.isPresent()) {
            InstallmentPaymentEntity next = nextIncomplete.get();
            agreement.setNextPaymentDate(next.getDueDate());
            agreement.setNextPaymentAmount(next.getRemainingAmount());
        } else {
            agreement.setNextPaymentDate(null);
            agreement.setNextPaymentAmount(null);
        }

        agreement.setUpdatedAt(LocalDateTime.now());
        agreementRepo.save(agreement);

        log.info("✓ Agreement updated");
    }

    // HELPER: Build success message
    private String buildPaymentSuccessMessage(
            List<FlexiblePaymentResponse.PaymentApplicationDetail> applications
    ) {
        long completed = applications.stream()
                .filter(FlexiblePaymentResponse.PaymentApplicationDetail::isWasCompleted)
                .count();

        long partial = applications.stream()
                .filter(a -> !a.isWasCompleted())
                .count();

        if (partial == 0) {
            return String.format("Successfully paid %d installment%s",
                    completed, completed == 1 ? "" : "s");
        } else {
            return String.format(
                    "Successfully paid %d installment%s and partially paid %d more",
                    completed, completed == 1 ? "" : "s", partial
            );
        }
    }

    // HELPER: Build agreement update summary
    private FlexiblePaymentResponse.AgreementUpdateSummary
    buildAgreementUpdateSummary(InstallmentAgreementEntity agreement) {

        long partialCount = paymentRepo.countByAgreementAndPaymentStatus(
                agreement, PaymentStatus.PARTIALLY_PAID
        );

        return FlexiblePaymentResponse.AgreementUpdateSummary.builder()
                .paymentsCompleted(agreement.getPaymentsCompleted())
                .paymentsPartial((int) partialCount)
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