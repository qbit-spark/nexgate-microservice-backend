package org.nextgate.nextgatebackend.installment_purchase.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
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
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPaymentEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentStatus;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.events.InstallmentAgreementCompletedEvent;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPaymentRepo;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.installment_purchase.service.InstallmentService;
import org.nextgate.nextgatebackend.installment_purchase.utils.InstallmentValidator;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentServiceImpl implements InstallmentService {

    private final InstallmentAgreementRepo agreementRepo;
    private final InstallmentPaymentRepo paymentRepo;
    private final InstallmentPlanRepo planRepo;
    private final ProductRepo productRepo;
    private final AccountRepo accountRepo;
    private final InstallmentValidator validator;
    private final WalletService walletService;
    private final LedgerService ledgerService;
    private final TransactionHistoryService transactionHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderService orderService;

    // ========================================
    // AGREEMENT CREATION
    // ========================================
    @Override
    @Transactional
    public InstallmentAgreementEntity createInstallmentAgreement(
            CheckoutSessionEntity checkoutSession
    ) throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     CREATING INSTALLMENT AGREEMENT                         ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Checkout Session ID: {}", checkoutSession.getSessionId());

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        log.debug("Authenticated user: {}", authenticatedUser.getUserName());

        // ========================================
        // 2. VALIDATE CHECKOUT SESSION
        // ========================================
        validator.validateCheckoutSessionForAgreementCreation(checkoutSession, authenticatedUser);

        // ========================================
        // 3. EXTRACT DATA FROM CHECKOUT SESSION
        // ========================================
        CheckoutSessionEntity.CheckoutItem item = checkoutSession.getItems().get(0);
        CheckoutSessionEntity.InstallmentConfiguration config =
                checkoutSession.getInstallmentConfig();

        UUID productId = item.getProductId();
        Integer quantity = item.getQuantity();
        UUID customerId = checkoutSession.getCustomer().getAccountId();
        UUID planId = checkoutSession.getSelectedInstallmentPlanId();

        log.debug("Product: {}, Qty: {}, Plan: {}", productId, quantity, planId);

        // ========================================
        // 4. FETCH ENTITIES
        // ========================================
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        InstallmentPlanEntity plan = planRepo.findById(planId)
                .orElseThrow(() -> new ItemNotFoundException("Installment plan not found"));

        AccountEntity customer = accountRepo.findById(customerId)
                .orElseThrow(() -> new ItemNotFoundException("Customer not found"));

        log.debug("Entities fetched - Product: {}, Plan: {}",
                product.getProductName(), plan.getPlanName());

        // ========================================
        // 5. CREATE INSTALLMENT AGREEMENT ENTITY
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        InstallmentAgreementEntity agreement = InstallmentAgreementEntity.builder()
                // Relationships
                .customer(customer)
                .product(product)
                .shop(product.getShop())
                .selectedPlan(plan)
                .checkoutSessionId(checkoutSession.getSessionId())
                .payments(new ArrayList<>())

                // Product snapshot
                .productName(product.getProductName())
                .productImage(product.getProductImages() != null &&
                        !product.getProductImages().isEmpty()
                        ? product.getProductImages().get(0) : null)
                .productPrice(product.getPrice())
                .quantity(quantity)

                // Payment terms snapshot
                .paymentFrequency(plan.getPaymentFrequency())
                .customFrequencyDays(plan.getCustomFrequencyDays())
                .numberOfPayments(plan.getNumberOfPayments())
                .termMonths(config.getTermMonths())
                .apr(plan.getApr())
                .gracePeriodDays(plan.getGracePeriodDays())

                // Financial breakdown
                .downPaymentPercent(config.getDownPaymentPercent())
                .downPaymentAmount(config.getDownPaymentAmount())
                .financedAmount(config.getFinancedAmount())
                .monthlyPaymentAmount(config.getMonthlyPaymentAmount())
                .totalInterestAmount(config.getTotalInterest())
                .totalAmount(config.getTotalAmount())
                .currency("TZS")

                // Payment tracking
                .paymentsCompleted(0)
                .paymentsRemaining(plan.getNumberOfPayments())
                .amountPaid(config.getDownPaymentAmount()) // Down payment already paid
                .amountRemaining(config.getTotalAmount().subtract(config.getDownPaymentAmount()))
                .nextPaymentDate(config.getFirstPaymentDate())
                .nextPaymentAmount(config.getMonthlyPaymentAmount())

                // Status
                .agreementStatus(AgreementStatus.PENDING_FIRST_PAYMENT)
                .defaultCount(0)
                .consecutiveLatePayments(0)

                // Timing
                .createdAt(now)
                .firstPaymentDate(config.getFirstPaymentDate())
                .lastPaymentDate(calculateLastPaymentDate(config))
                .updatedAt(now)

                // Fulfillment
                .fulfillmentTiming(plan.getFulfillmentTiming())
                .orderId(null) // Will be set when order is created

                // Addresses
                .shippingAddress(serializeAddress(checkoutSession.getShippingAddress()))
                .billingAddress(serializeAddress(checkoutSession.getBillingAddress()))

                // Metadata - store payment method
                .metadata(new java.util.HashMap<>())

                // Soft delete
                .isDeleted(false)

                .build();

        // Store payment method in metadata
        agreement.getMetadata().put("paymentMethod", "WALLET");

        // ========================================
        // 6. SAVE AGREEMENT
        // ========================================
        InstallmentAgreementEntity savedAgreement = agreementRepo.save(agreement);

        log.info("✓ Agreement created: {} ({})",
                savedAgreement.getAgreementId(),
                savedAgreement.getAgreementNumber());

        // ========================================
        // 7. GENERATE ALL PAYMENT RECORDS UPFRONT
        // ========================================
        List<InstallmentPaymentEntity> payments = generatePaymentRecords(
                savedAgreement,
                config.getSchedule()
        );

        paymentRepo.saveAll(payments);

        log.info("✓ Generated {} payment records", payments.size());

        // ========================================
        // 8. HANDLE FULFILLMENT
        // ========================================
        if (savedAgreement.getFulfillmentTiming() == FulfillmentTiming.IMMEDIATE) {
            log.info("Fulfillment: IMMEDIATE - Product will ship after down payment");

            // TODO: Create order via OrderService
            // OrderEntity order = orderService.createInstallmentOrder(savedAgreement);
            // savedAgreement.setOrderId(order.getOrderId());
            // savedAgreement.setShippedAt(now);
            // agreementRepo.save(savedAgreement);

            log.warn("[TODO] Order creation not yet implemented");

        } else {
            log.info("Fulfillment: AFTER_PAYMENT - Product ships after final payment");

            // TODO: Hold inventory
            // inventoryService.holdInventory(
            //     product.getProductId(),
            //     quantity,
            //     savedAgreement.getAgreementId()
            // );

            log.warn("[TODO] Inventory hold not yet implemented");
        }

        // ========================================
        // 9. UPDATE AGREEMENT STATUS TO ACTIVE
        // ========================================
        savedAgreement.setAgreementStatus(AgreementStatus.ACTIVE);
        agreementRepo.save(savedAgreement);

        log.info("✓ Agreement {} is now ACTIVE", savedAgreement.getAgreementNumber());

        // ========================================
        // 10. SEND CONFIRMATION
        // ========================================
        // TODO: Send email to customer
        // emailService.sendAgreementConfirmation(savedAgreement);
        log.info("[TODO] Send agreement confirmation email");

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║     AGREEMENT CREATION COMPLETE                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        return savedAgreement;
    }


    // ========================================
    // PAYMENT PROCESSING (WITH REAL LEDGER)
    // ========================================
    @Override
    @Transactional
    public InstallmentPaymentEntity processInstallmentPayment(UUID paymentId)
            throws ItemNotFoundException, BadRequestException {

        /*
         * Processes a scheduled installment payment
         *
         * Flow:
         * 1. Validate payment can be processed
         * 2. Check wallet balance
         * 3. Move money via ledger (wallet → platform)
         * 4. Create transaction history
         * 5. Update payment and agreement records
         * 6. Handle completion if final payment
         */

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         PROCESSING INSTALLMENT PAYMENT                     ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Payment ID: {}", paymentId);

        // ========================================
        // STEP 1: FETCH AND VALIDATE PAYMENT
        // ========================================
        InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Payment not found with ID: " + paymentId));

        log.info("Payment Details:");
        log.info("  Payment Number: #{}", payment.getPaymentNumber());
        log.info("  Agreement: {}", payment.getAgreement().getAgreementNumber());
        log.info("  Amount: {} TZS", payment.getScheduledAmount());
        log.info("  Due Date: {}", payment.getDueDate());
        log.info("  Current Status: {}", payment.getPaymentStatus());

        // Validate payment can be processed
        validator.validatePaymentCanBeProcessed(payment);

        InstallmentAgreementEntity agreement = payment.getAgreement();
        AccountEntity customer = agreement.getCustomer();

        log.info("Customer: {} ({})", customer.getUserName(), customer.getAccountId());


        // ========================================
        // STEP 3: GET WALLET AND CHECK BALANCE WE DEAL WITH WALLET PAYMENTS ONLY
        // ========================================
        BigDecimal requiredAmount = payment.getScheduledAmount();

        log.info("Fetching customer wallet...");
        WalletEntity customerWallet = walletService.getWalletByAccountId(customer.getAccountId());

        // Check wallet is active
        if (!customerWallet.getIsActive()) {
            String errorMsg = "Wallet is not active. Please contact support.";
            log.error(errorMsg);
            handlePaymentFailure(payment, errorMsg);
            throw new BadRequestException(errorMsg);
        }

        log.info("✓ Wallet found and active: {}", customerWallet.getId());

        // Get current balance
        BigDecimal walletBalance = walletService.getWalletBalance(customerWallet);


        log.info("Balance Check:");
        log.info("  Required: {} TZS", requiredAmount);
        log.info("  Available: {} TZS", walletBalance);
        log.info("  Sufficient: {}", walletBalance.compareTo(requiredAmount) >= 0);

        // Check sufficient balance
        if (walletBalance.compareTo(requiredAmount) < 0) {
            String errorMsg = String.format(
                    "Insufficient wallet balance. Required: %s TZS, Available: %s TZS. " +
                            "Please top up your wallet before the next payment attempt.",
                    requiredAmount, walletBalance);

            log.warn("✗ Insufficient balance");
            handlePaymentFailure(payment, errorMsg);

            throw new BadRequestException(errorMsg);
        }

        log.info("✓ Sufficient balance available");

        // ========================================
        // STEP 4: UPDATE PAYMENT STATUS TO PROCESSING
        // ========================================
        payment.setPaymentStatus(PaymentStatus.PROCESSING);
        payment.setAttemptedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        log.info("✓ Payment status updated to PROCESSING");

        // ========================================
        // STEP 5: EXECUTE PAYMENT VIA LEDGER SYSTEM
        // ========================================

        log.info("Initiating ledger transaction...");

        // Get ledger accounts
        LedgerAccountEntity walletLedger =
                ledgerService.getOrCreateWalletAccount(customerWallet);

        LedgerAccountEntity platformRevenue =
                ledgerService.getPlatformRevenueAccount();

        log.info("Ledger Accounts:");
        log.info("  Source: {} (Balance: {} TZS)",
                walletLedger.getAccountNumber(),
                walletLedger.getCurrentBalance());
        log.info("  Destination: {} (Balance: {} TZS)",
                platformRevenue.getAccountNumber(),
                platformRevenue.getCurrentBalance());

        // Create description
        String description = String.format(
                "Installment payment #%d of %d for agreement %s (%s)",
                payment.getPaymentNumber(),
                agreement.getNumberOfPayments(),
                agreement.getAgreementNumber(),
                agreement.getProductName()
        );

        log.info("Creating ledger entry...");

        // Execute atomic money transfer via double-entry bookkeeping
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                walletLedger,                           // DEBIT (money leaves)
                platformRevenue,                        // CREDIT (money enters)
                requiredAmount,                         // AMOUNT
                LedgerEntryType.INSTALLMENT_PAYMENT,    // TYPE
                "INSTALLMENT_PAYMENT",                  // REFERENCE TYPE
                payment.getPaymentId(),                 // REFERENCE ID
                description,                            // DESCRIPTION
                customer                                // CREATED BY
        );

        log.info("✓ Ledger entry created successfully");
        log.info("  Entry Number: {}", ledgerEntry.getEntryNumber());
        log.info("  Amount: {} TZS", ledgerEntry.getAmount());
        log.info("  Entry Type: {}", ledgerEntry.getEntryType());

        // ========================================
        // STEP 6: CREATE TRANSACTION HISTORY
        // ========================================
        log.info("Creating transaction history record...");

        String txHistoryDescription = String.format(
                "Payment #%d of %d for %s",
                payment.getPaymentNumber(),
                agreement.getNumberOfPayments(),
                agreement.getProductName()
        );

        transactionHistoryService.createTransaction(
                customer,                               // ACCOUNT
                TransactionType.INSTALLMENT_PAYMENT,    // TYPE
                TransactionDirection.DEBIT,             // DIRECTION (money out)
                requiredAmount,                         // AMOUNT
                "Installment Payment",                  // TITLE
                txHistoryDescription,                   // DESCRIPTION
                ledgerEntry.getId(),                    // LEDGER ENTRY ID
                "INSTALLMENT_AGREEMENT",                // REFERENCE TYPE
                agreement.getAgreementId()              // REFERENCE ID
        );

        log.info("✓ Transaction history created");

        // ========================================
        // STEP 7: UPDATE PAYMENT RECORD
        // ========================================
        LocalDateTime now = LocalDateTime.now();
        String transactionId = ledgerEntry.getEntryNumber();

        payment.recordSuccessfulPayment(transactionId, "WALLET");
        payment.setPaymentMethod("WALLET");
        paymentRepo.save(payment);

        log.info("✓ Payment record updated to COMPLETED");
        log.info("  Transaction ID: {}", transactionId);
        log.info("  Paid At: {}", payment.getPaidAt());

        // ========================================
        // STEP 8: UPDATE AGREEMENT
        // ========================================
        log.info("Updating agreement record...");

        BigDecimal previousAmountPaid = agreement.getAmountPaid();
        BigDecimal previousAmountRemaining = agreement.getAmountRemaining();
        Integer previousPaymentsCompleted = agreement.getPaymentsCompleted();

        // Update agreement financials
        agreement.recordPayment(payment.getScheduledAmount());

        // Update next payment info
        updateNextPaymentInfo(agreement);

        agreement.setUpdatedAt(now);
        agreementRepo.save(agreement);

        log.info("✓ Agreement updated:");
        log.info("  Payments Completed: {} → {} of {}",
                previousPaymentsCompleted,
                agreement.getPaymentsCompleted(),
                agreement.getNumberOfPayments());
        log.info("  Amount Paid: {} → {} TZS",
                previousAmountPaid,
                agreement.getAmountPaid());
        log.info("  Amount Remaining: {} → {} TZS",
                previousAmountRemaining,
                agreement.getAmountRemaining());

        if (agreement.getNextPaymentDate() != null) {
            log.info("  Next Payment Due: {} (Amount: {} TZS)",
                    agreement.getNextPaymentDate(),
                    agreement.getNextPaymentAmount());
        } else {
            log.info("  Next Payment: NONE (Final payment completed)");
        }

        // ========================================
        // STEP 9: CHECK IF AGREEMENT IS COMPLETE
        // ========================================
        if (agreement.getPaymentsRemaining() == 0) {
            log.info("🎉 FINAL PAYMENT COMPLETED - Agreement fully paid!");
            handleAgreementCompletion(agreement);
        } else {
            log.info("Agreement still in progress: {} of {} payments completed",
                    agreement.getPaymentsCompleted(),
                    agreement.getNumberOfPayments());
        }

        // ========================================
        // STEP 10: UPDATE ORDER PAYMENT PROGRESS (if exists)
        // ========================================
        if (agreement.getOrderId() != null) {
            log.info("Order exists for this agreement: {}", agreement.getOrderId());

            // TODO: Integrate with OrderService
            // orderService.updateInstallmentPaymentProgress(
            //     agreement.getOrderId(),
            //     agreement.getAmountPaid(),
            //     agreement.getTotalAmount(),
            //     agreement.getPaymentsCompleted(),
            //     agreement.getNumberOfPayments()
            // );

            log.info("[TODO] Update order payment progress:");
            log.info("  Order ID: {}", agreement.getOrderId());
            log.info("  Amount Paid: {} TZS", agreement.getAmountPaid());
            log.info("  Total Amount: {} TZS", agreement.getTotalAmount());
            log.info("  Progress: {}%", agreement.getAmountPaidPercentage());
        } else {
            log.info("No order created yet (AFTER_PAYMENT fulfillment)");
        }

        // ========================================
        // STEP 11: SEND SUCCESS NOTIFICATIONS
        // ========================================
        log.info("Triggering notifications...");

        // TODO: Integrate with NotificationService
        // notificationService.sendPaymentSuccessNotification(
        //     customer,
        //     agreement,
        //     payment
        // );

        log.info("[TODO] Send notifications:");
        log.info("  ✉ Email to: {}", customer.getEmail());
        log.info("  📱 SMS to: {}", customer.getPhoneNumber());
        log.info("  🔔 In-app notification");

        // ========================================
        // STEP 12: RECORD SUCCESS METRICS
        // ========================================
        log.info("Payment processing completed successfully:");
        log.info("  Duration: ~{} ms",
                java.time.Duration.between(payment.getAttemptedAt(), now).toMillis());
        log.info("  Retry Count: {}", payment.getRetryCount());

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         PAYMENT PROCESSING COMPLETE ✓                      ║");
        log.info("╚════════════════════════════════════════════════════════════╝");

        return payment;
    }


    // ========================================
    //        HANDLING MISSED PAYMENT
    // ========================================
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMissedPayment(UUID paymentId, String failureReason)
            throws ItemNotFoundException {

        /**
         * Handles a missed/failed payment
         *
         * Called when:
         * - Payment processing fails (insufficient balance, wallet error, etc.)
         * - Scheduled job detects payment was not made on time
         *
         * Actions:
         * - Updates payment status to FAILED or LATE
         * - Increments agreement's default count
         * - May trigger DEFAULTED status after 2 missed payments
         * - Sends notification to customer
         *
         * Transaction:
         * - Uses REQUIRES_NEW to ensure failure is recorded even if parent transaction fails
         */

        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║         HANDLING MISSED PAYMENT                            ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
        log.warn("Payment ID: {}", paymentId);
        log.warn("Failure Reason: {}", failureReason);

        // ========================================
        // STEP 1: FETCH PAYMENT
        // ========================================
        InstallmentPaymentEntity payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Payment not found with ID: " + paymentId));

        log.warn("Payment Details:");
        log.warn("  Payment Number: #{}", payment.getPaymentNumber());
        log.warn("  Agreement: {}", payment.getAgreement().getAgreementNumber());
        log.warn("  Amount: {} TZS", payment.getScheduledAmount());
        log.warn("  Due Date: {}", payment.getDueDate());
        log.warn("  Current Status: {}", payment.getPaymentStatus());
        log.warn("  Current Retry Count: {}", payment.getRetryCount());

        InstallmentAgreementEntity agreement = payment.getAgreement();

        // ========================================
        // STEP 2: UPDATE PAYMENT STATUS
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        // Record failure on payment
        payment.recordFailedPayment(failureReason);

        // Check if payment is overdue
        if (payment.isOverdue()) {
            payment.setPaymentStatus(PaymentStatus.LATE);
            log.warn("Payment is OVERDUE - marking as LATE");
        } else {
            log.warn("Payment marked as FAILED");
        }

        paymentRepo.save(payment);

        log.warn("✓ Payment record updated:");
        log.warn("  Status: {}", payment.getPaymentStatus());
        log.warn("  Retry Count: {}", payment.getRetryCount());
        log.warn("  Can Retry: {}", payment.canRetry());
        log.warn("  Attempted At: {}", payment.getAttemptedAt());

        // ========================================
        // STEP 3: UPDATE AGREEMENT
        // ========================================
        Integer previousDefaultCount = agreement.getDefaultCount();
        AgreementStatus previousStatus = agreement.getAgreementStatus();

        // Record missed payment on agreement
        // This increments defaultCount and may change status to DEFAULTED
        agreement.recordMissedPayment();
        agreement.setUpdatedAt(now);
        agreementRepo.save(agreement);

        log.warn("✓ Agreement updated:");
        log.warn("  Default Count: {} → {}", previousDefaultCount, agreement.getDefaultCount());
        log.warn("  Status: {} → {}", previousStatus, agreement.getAgreementStatus());
        log.warn("  Consecutive Late Payments: {}", agreement.getConsecutiveLatePayments());

        // ========================================
        // STEP 4: CHECK FOR DEFAULTED STATUS
        // ========================================
        if (agreement.getAgreementStatus() == AgreementStatus.DEFAULTED) {
            log.error("╔════════════════════════════════════════════════════════════╗");
            log.error("║         ⚠  AGREEMENT DEFAULTED ⚠                          ║");
            log.error("╚════════════════════════════════════════════════════════════╝");
            log.error("Agreement Number: {}", agreement.getAgreementNumber());
            log.error("Customer: {} ({})",
                    agreement.getCustomer().getUserName(),
                    agreement.getCustomer().getEmail());
            log.error("Product: {}", agreement.getProductName());
            log.error("Total Defaults: {}", agreement.getDefaultCount());
            log.error("Amount Paid: {} TZS", agreement.getAmountPaid());
            log.error("Amount Remaining: {} TZS", agreement.getAmountRemaining());

            // TODO: Escalate to collections
            // collectionsService.escalateDefaultedAgreement(agreement);
            log.error("[TODO] Escalate to collections service");

            // TODO: Freeze order/shipment if not yet delivered
            if (agreement.getOrderId() != null && agreement.getDeliveredAt() == null) {
                // orderService.freezeOrder(agreement.getOrderId(), "Installment defaulted");
                log.error("[TODO] Freeze order: {}", agreement.getOrderId());
            }
        }

        // ========================================
        // STEP 5: DETERMINE RETRY STRATEGY
        // ========================================
        if (payment.canRetry()) {
            log.warn("Payment can be retried:");
            log.warn("  Retry attempts remaining: {}", 5 - payment.getRetryCount());
            log.warn("  Suggested retry date: {}", now.plusDays(3));

            // TODO: Schedule retry job via JobRunr
            // BackgroundJob.schedule(
            //     () -> processInstallmentPayment(payment.getPaymentId()),
            //     now.plusDays(3)
            // );
            log.warn("[TODO] Schedule retry via JobRunr (3 days from now)");
        } else {
            log.error("Payment cannot be retried (max attempts reached)");
            payment.setPaymentStatus(PaymentStatus.SKIPPED);
            paymentRepo.save(payment);
        }

        // ========================================
        // STEP 6: SEND NOTIFICATIONS
        // ========================================
        AccountEntity customer = agreement.getCustomer();

        log.warn("Sending notifications...");

        // TODO: Send email notification
        // notificationService.sendPaymentFailedNotification(
        //     customer,
        //     agreement,
        //     payment,
        //     failureReason
        // );

        log.warn("[TODO] Email notification:");
        log.warn("  To: {}", customer.getEmail());
        log.warn("  Subject: Payment Failed - {} (Agreement {})",
                agreement.getProductName(),
                agreement.getAgreementNumber());
        log.warn("  Content: Payment #{} failed. Reason: {}",
                payment.getPaymentNumber(),
                failureReason);

        // TODO: Send SMS if critical
        if (agreement.getDefaultCount() >= 2) {
            // smsService.sendUrgentNotification(customer.getPhoneNumber(), ...);
            log.warn("[TODO] Send URGENT SMS notification");
        }

        // TODO: Create in-app notification
        // notificationService.createInAppNotification(
        //     customer.getAccountId(),
        //     "Payment Failed",
        //     failureReason
        // );
        log.warn("[TODO] Create in-app notification");

        // ========================================
        // STEP 7: LOG ANALYTICS EVENT
        // ========================================
        // TODO: Track payment failure for analytics
        // analyticsService.trackEvent("installment_payment_failed", Map.of(
        //     "agreement_id", agreement.getAgreementId(),
        //     "payment_number", payment.getPaymentNumber(),
        //     "failure_reason", failureReason,
        //     "default_count", agreement.getDefaultCount(),
        //     "amount", payment.getScheduledAmount()
        // ));
        log.warn("[TODO] Track analytics event: installment_payment_failed");

        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║         MISSED PAYMENT HANDLING COMPLETE                   ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
    }


    // ========================================
    //        CALCULATING EARLY PAYOFF AMOUNT
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateEarlyPayoffAmount(UUID agreementId)
            throws ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         CALCULATING EARLY PAYOFF AMOUNT                    ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", agreementId);

        // ========================================
        // STEP 1: FETCH AGREEMENT
        // ========================================
        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found with ID: " + agreementId));

        log.info("Agreement Details:");
        log.info("  Agreement Number: {}", agreement.getAgreementNumber());
        log.info("  Customer: {}", agreement.getCustomer().getUserName());
        log.info("  Product: {}", agreement.getProductName());
        log.info("  Status: {}", agreement.getAgreementStatus());
        log.info("  Payments Completed: {}/{}",
                agreement.getPaymentsCompleted(),
                agreement.getNumberOfPayments());

        // ========================================
        // STEP 2: GET ALL REMAINING SCHEDULED PAYMENTS
        // ========================================
        List<InstallmentPaymentEntity> remainingPayments = paymentRepo
                .findByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
                        agreement, PaymentStatus.SCHEDULED);

        log.info("Found {} remaining scheduled payments", remainingPayments.size());

        if (remainingPayments.isEmpty()) {
            log.info("No remaining payments - agreement is already complete");
            return BigDecimal.ZERO;
        }

        // ========================================
        // STEP 3: CALCULATE PRINCIPAL AND INTEREST BREAKDOWN
        // ========================================
        BigDecimal remainingPrincipal = BigDecimal.ZERO;
        BigDecimal remainingInterest = BigDecimal.ZERO;

        log.info("Payment Breakdown:");
        log.info("  Payment# | Scheduled Amt | Principal | Interest | Balance");
        log.info("  ---------|---------------|-----------|----------|--------");

        for (InstallmentPaymentEntity payment : remainingPayments) {
            remainingPrincipal = remainingPrincipal.add(payment.getPrincipalPortion());
            remainingInterest = remainingInterest.add(payment.getInterestPortion());

            log.info("  #{:2d}      | {:13} | {:9} | {:8} | {:7}",
                    payment.getPaymentNumber(),
                    payment.getScheduledAmount(),
                    payment.getPrincipalPortion(),
                    payment.getInterestPortion(),
                    payment.getRemainingBalance());
        }

        log.info("  ---------|---------------|-----------|----------|--------");
        log.info("  TOTALS   |               | {:9} | {:8} |",
                remainingPrincipal,
                remainingInterest);

        // ========================================
        // STEP 4: APPLY EARLY PAYOFF DISCOUNT
        // ========================================
        // Policy: Give 75% discount on remaining interest as incentive
        BigDecimal interestDiscountPercent = BigDecimal.valueOf(0.75);
        BigDecimal interestDiscount = remainingInterest.multiply(interestDiscountPercent);
        BigDecimal discountedInterest = remainingInterest.subtract(interestDiscount);

        log.info("Early Payoff Discount:");
        log.info("  Original Interest: {} TZS", remainingInterest);
        log.info("  Discount (75%): {} TZS", interestDiscount);
        log.info("  Discounted Interest (25%): {} TZS", discountedInterest);

        // ========================================
        // STEP 5: CALCULATE FINAL EARLY PAYOFF AMOUNT
        // ========================================
        BigDecimal earlyPayoffAmount = remainingPrincipal.add(discountedInterest);

        // Round to 2 decimal places
        earlyPayoffAmount = earlyPayoffAmount.setScale(2, RoundingMode.HALF_UP);

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         EARLY PAYOFF CALCULATION COMPLETE                  ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Calculation Summary:");
        log.info("  Remaining Principal: {} TZS", remainingPrincipal);
        log.info("  Remaining Interest: {} TZS", remainingInterest);
        log.info("  Interest Discount (75%): {} TZS", interestDiscount);
        log.info("  ─────────────────────────────────────");
        log.info("  EARLY PAYOFF AMOUNT: {} TZS", earlyPayoffAmount);
        log.info("  ─────────────────────────────────────");
        log.info("  Customer Savings: {} TZS", interestDiscount);
        log.info("  Savings Percentage: {:.1f}%",
                interestDiscount.multiply(BigDecimal.valueOf(100))
                        .divide(remainingInterest, 1, RoundingMode.HALF_UP));

        // ========================================
        // STEP 6: COMPARE TO FULL PAYMENT
        // ========================================
        BigDecimal fullRemainingAmount = agreement.getAmountRemaining();
        BigDecimal savings = fullRemainingAmount.subtract(earlyPayoffAmount);

        log.info("Comparison:");
        log.info("  If paying normally: {} TZS", fullRemainingAmount);
        log.info("  If paying off early: {} TZS", earlyPayoffAmount);
        log.info("  Total savings: {} TZS ({:.1f}%)",
                savings,
                savings.multiply(BigDecimal.valueOf(100))
                        .divide(fullRemainingAmount, 1, RoundingMode.HALF_UP));

        return earlyPayoffAmount;
    }


    // ========================================
    //        PROCESSING EARLY PAYOFF
    // ========================================

    @Override
    @Transactional
    public InstallmentAgreementEntity processEarlyPayoff(UUID agreementId)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         PROCESSING EARLY PAYOFF                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Agreement ID: {}", agreementId);

        // ========================================
        // STEP 1: FETCH AND VALIDATE AGREEMENT
        // ========================================
        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found with ID: " + agreementId));

        log.info("Agreement Details:");
        log.info("  Agreement Number: {}", agreement.getAgreementNumber());
        log.info("  Customer: {}", agreement.getCustomer().getUserName());
        log.info("  Product: {}", agreement.getProductName());
        log.info("  Status: {}", agreement.getAgreementStatus());
        log.info("  Payments: {}/{}",
                agreement.getPaymentsCompleted(),
                agreement.getNumberOfPayments());
        log.info("  Amount Remaining: {} TZS", agreement.getAmountRemaining());

        // Validate agreement can be paid off early
        validator.validateCanMakeEarlyPayoff(agreement);

        log.info("✓ Agreement validated for early payoff");

        // ========================================
        // STEP 2: CALCULATE EARLY PAYOFF AMOUNT
        // ========================================
        BigDecimal payoffAmount = calculateEarlyPayoffAmount(agreementId);
        BigDecimal savings = agreement.getAmountRemaining().subtract(payoffAmount);

        log.info("Early Payoff Calculation:");
        log.info("  Normal remaining: {} TZS", agreement.getAmountRemaining());
        log.info("  Early payoff amount: {} TZS", payoffAmount);
        log.info("  Customer savings: {} TZS ({:.1f}%)",
                savings,
                savings.multiply(BigDecimal.valueOf(100))
                        .divide(agreement.getAmountRemaining(), 1, RoundingMode.HALF_UP));

        // ========================================
        // STEP 3: VERIFY WALLET BALANCE
        // ========================================
        AccountEntity customer = agreement.getCustomer();

        WalletEntity customerWallet = walletService.getWalletByAccountId(
                customer.getAccountId());

        if (!customerWallet.getIsActive()) {
            throw new BadRequestException(
                    "Wallet is not active. Please contact support.");
        }

        BigDecimal walletBalance = walletService.getWalletBalance(customerWallet);

        log.info("Wallet Verification:");
        log.info("  Required: {} TZS", payoffAmount);
        log.info("  Available: {} TZS", walletBalance);

        if (walletBalance.compareTo(payoffAmount) < 0) {
            throw new BadRequestException(String.format(
                    "Insufficient wallet balance for early payoff. " +
                            "Required: %s TZS, Available: %s TZS",
                    payoffAmount, walletBalance));
        }

        log.info("✓ Sufficient wallet balance");

        // ========================================
        // STEP 4: EXECUTE PAYMENT VIA LEDGER
        // ========================================
        log.info("Executing early payoff payment via ledger...");

        // Get ledger accounts
        LedgerAccountEntity walletLedger =
                ledgerService.getOrCreateWalletAccount(customerWallet);

        LedgerAccountEntity platformRevenue =
                ledgerService.getPlatformRevenueAccount();

        // Create description
        String description = String.format(
                "Early payoff for agreement %s - %s (saved %s TZS)",
                agreement.getAgreementNumber(),
                agreement.getProductName(),
                savings
        );

        log.info("Creating ledger entry...");

        // Execute atomic money transfer
        LedgerEntryEntity ledgerEntry = ledgerService.createEntry(
                walletLedger,                           // DEBIT (from wallet)
                platformRevenue,                        // CREDIT (to platform)
                payoffAmount,                           // AMOUNT
                LedgerEntryType.INSTALLMENT_PAYMENT,    // TYPE
                "INSTALLMENT_EARLY_PAYOFF",             // REFERENCE TYPE
                agreement.getAgreementId(),             // REFERENCE ID
                description,                            // DESCRIPTION
                customer                                // CREATED BY
        );

        log.info("✓ Ledger entry created: {}", ledgerEntry.getEntryNumber());

        // ========================================
        // STEP 5: CREATE TRANSACTION HISTORY
        // ========================================
        log.info("Creating transaction history...");

        transactionHistoryService.createTransaction(
                customer,
                TransactionType.INSTALLMENT_PAYMENT,
                TransactionDirection.DEBIT,
                payoffAmount,
                "Early Payoff",
                String.format("Early payoff for %s (saved %s TZS)",
                        agreement.getProductName(), savings),
                ledgerEntry.getId(),
                "INSTALLMENT_AGREEMENT",
                agreement.getAgreementId()
        );

        log.info("✓ Transaction history created");

        // ========================================
        // STEP 6: MARK ALL REMAINING PAYMENTS AS COMPLETED
        // ========================================
        log.info("Marking all remaining payments as completed...");

        List<InstallmentPaymentEntity> remainingPayments = paymentRepo
                .findByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
                        agreement, PaymentStatus.SCHEDULED);

        LocalDateTime now = LocalDateTime.now();
        String transactionId = ledgerEntry.getEntryNumber();

        int paymentCount = 0;
        for (InstallmentPaymentEntity payment : remainingPayments) {
            payment.recordSuccessfulPayment(transactionId, "EARLY_PAYOFF");
            payment.setPaymentMethod("WALLET");
            payment.setNotes(String.format("Completed via early payoff (saved %s TZS)", savings));
            paymentRepo.save(payment);
            paymentCount++;
        }

        log.info("✓ Marked {} payments as COMPLETED", paymentCount);

        // ========================================
        // STEP 7: UPDATE AGREEMENT TO COMPLETED
        // ========================================
        log.info("Updating agreement to COMPLETED status...");

        Integer previousPaymentsCompleted = agreement.getPaymentsCompleted();
        BigDecimal previousAmountPaid = agreement.getAmountPaid();

        agreement.setPaymentsCompleted(agreement.getNumberOfPayments());
        agreement.setPaymentsRemaining(0);
        agreement.setAmountPaid(agreement.getTotalAmount());
        agreement.setAmountRemaining(BigDecimal.ZERO);
        agreement.setAgreementStatus(AgreementStatus.COMPLETED);
        agreement.setCompletedAt(now);
        agreement.setNextPaymentDate(null);
        agreement.setNextPaymentAmount(null);
        agreement.setUpdatedAt(now);

        // Store early payoff info in metadata
        agreement.getMetadata().put("earlyPayoff", true);
        agreement.getMetadata().put("earlyPayoffAmount", payoffAmount.toString());
        agreement.getMetadata().put("earlyPayoffSavings", savings.toString());
        agreement.getMetadata().put("earlyPayoffDate", now.toString());

        agreementRepo.save(agreement);

        log.info("✓ Agreement updated:");
        log.info("  Status: {}", agreement.getAgreementStatus());
        log.info("  Payments: {} → {} of {}",
                previousPaymentsCompleted,
                agreement.getPaymentsCompleted(),
                agreement.getNumberOfPayments());
        log.info("  Amount Paid: {} → {} TZS",
                previousAmountPaid,
                agreement.getAmountPaid());
        log.info("  Completed At: {}", agreement.getCompletedAt());

        // ========================================
        // STEP 8: HANDLE FULFILLMENT
        // ========================================
        if (agreement.getFulfillmentTiming() == FulfillmentTiming.AFTER_PAYMENT &&
                agreement.getOrderId() == null) {

            log.info("Fulfillment type: AFTER_PAYMENT");
            log.info("Order should be created now (via event)");

            try {
                InstallmentAgreementCompletedEvent event =
                        new InstallmentAgreementCompletedEvent(
                                this,
                                agreement.getAgreementId(),
                                agreement,
                                now,
                                true  // IS early payoff
                        );

                eventPublisher.publishEvent(event);

                log.info("✓ InstallmentAgreementCompletedEvent published (early payoff)");

            } catch (Exception e) {
                log.error("Failed to publish event", e);
            }

        } else if (agreement.getOrderId() != null) {
            log.info("Order already exists: {}", agreement.getOrderId());
            log.info("[TODO] Mark order as fully paid");
        }


        // ========================================
        // STEP 10: LOG ANALYTICS
        // ========================================
        // TODO: Track event
        // analyticsService.trackEvent("installment_early_payoff", Map.of(
        //     "agreement_id", agreement.getAgreementId(),
        //     "payoff_amount", payoffAmount,
        //     "savings", savings,
        //     "payments_remaining", remainingPayments.size()
        // ));

        log.info("[TODO] Track analytics: installment_early_payoff");

        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║         EARLY PAYOFF COMPLETE ✓                            ║");
        log.info("╚════════════════════════════════════════════════════════════╝");
        log.info("Summary:");
        log.info("  Customer: {}", customer.getUserName());
        log.info("  Product: {}", agreement.getProductName());
        log.info("  Amount Paid: {} TZS", payoffAmount);
        log.info("  Amount Saved: {} TZS", savings);
        log.info("  Payments Completed: {} of {}",
                paymentCount,
                agreement.getNumberOfPayments());
        log.info("  Agreement Status: COMPLETED");
        log.info("  Completed At: {}", now);

        return agreement;
    }


    // ========================================
    // QUERY OPERATIONS - AGREEMENTS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public InstallmentAgreementEntity getAgreementById(UUID agreementId)
            throws ItemNotFoundException {

        return agreementRepo.findById(agreementId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found with ID: " + agreementId));
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentAgreementEntity getAgreementByNumber(String agreementNumber)
            throws ItemNotFoundException {

        return agreementRepo.findByAgreementNumber(agreementNumber)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found: " + agreementNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentAgreementEntity> getMyAgreements(AccountEntity customer) {

        return agreementRepo.findByCustomerOrderByCreatedAtDesc(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentAgreementEntity> getMyAgreementsByStatus(
            AccountEntity customer,
            AgreementStatus status) {

        return agreementRepo.findByCustomerAndAgreementStatusOrderByCreatedAtDesc(
                customer, status);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentAgreementEntity getAgreementByCheckoutSession(UUID checkoutSessionId)
            throws ItemNotFoundException {

        return agreementRepo.findByCheckoutSessionId(checkoutSessionId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found for checkout session: " + checkoutSessionId));
    }


    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentEntity> getAgreementPayments(UUID agreementId)
            throws ItemNotFoundException {

        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found with ID: " + agreementId));

        return paymentRepo.findByAgreementOrderByPaymentNumberAsc(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public InstallmentPaymentEntity getNextPayment(UUID agreementId)
            throws ItemNotFoundException {

        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Agreement not found with ID: " + agreementId));

        return paymentRepo.findFirstByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
                        agreement, PaymentStatus.SCHEDULED)
                .orElseThrow(() -> new ItemNotFoundException(
                        "No upcoming payments found for agreement: " + agreementId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentEntity> getPaymentsDueToday() {

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        List<PaymentStatus> statuses = List.of(
                PaymentStatus.SCHEDULED,
                PaymentStatus.PENDING,
                PaymentStatus.LATE
        );

        return paymentRepo.findByPaymentStatusInAndDueDateLessThanEqual(
                statuses, endOfDay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentPaymentEntity> getOverduePayments() {

        LocalDateTime now = LocalDateTime.now();

        return paymentRepo.findByPaymentStatusAndDueDateBefore(
                PaymentStatus.LATE, now);
    }



   // ========================================
   // VALIDATION OPERATIONS
  // ========================================

    @Override
    @Transactional(readOnly = true)
    public boolean canMakeEarlyPayment(UUID agreementId) {

        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElse(null);

        if (agreement == null) return false;

        if (agreement.getAgreementStatus() != AgreementStatus.ACTIVE) return false;

        if (agreement.getPaymentsRemaining() == 0) return false;

        if (agreement.getAmountRemaining().compareTo(BigDecimal.ZERO) <= 0) return false;

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCancelAgreement(UUID agreementId) {

        InstallmentAgreementEntity agreement = agreementRepo.findById(agreementId)
                .orElse(null);

        if (agreement == null) return false;

        if (agreement.getPaymentsCompleted() > 0) return false;

        if (agreement.getAgreementStatus() != AgreementStatus.PENDING_FIRST_PAYMENT) return false;

        return true;
    }



    // ========================================
    // HELPER METHODS
    // ========================================
    private List<InstallmentPaymentEntity> generatePaymentRecords(
            InstallmentAgreementEntity agreement,
            List<CheckoutSessionEntity.PaymentScheduleItem> schedule) {
        log.debug("Generating payment records for agreement: {}", agreement.getAgreementId());

        List<InstallmentPaymentEntity> payments = new ArrayList<>();

        for (CheckoutSessionEntity.PaymentScheduleItem item : schedule) {
            InstallmentPaymentEntity payment = InstallmentPaymentEntity.builder()
                    .agreement(agreement)
                    .paymentNumber(item.getPaymentNumber())
                    .scheduledAmount(item.getAmount())
                    .paidAmount(null)
                    .principalPortion(item.getPrincipalPortion())
                    .interestPortion(item.getInterestPortion())
                    .remainingBalance(item.getRemainingBalance())
                    .lateFee(null)
                    .currency("TZS")
                    .paymentStatus(PaymentStatus.SCHEDULED)
                    .dueDate(item.getDueDate())
                    .paidAt(null)
                    .attemptedAt(null)
                    .createdAt(LocalDateTime.now())
                    .paymentMethod(null)
                    .transactionId(null)
                    .checkoutSessionId(null)
                    .failureReason(null)
                    .retryCount(0)
                    .notes(null)
                    .metadata(new java.util.HashMap<>())
                    .build();

            payments.add(payment);
        }

        log.debug("Generated {} payment records", payments.size());
        return payments;
    }

    private LocalDateTime calculateLastPaymentDate(
            CheckoutSessionEntity.InstallmentConfiguration config) {
        if (config.getSchedule() == null || config.getSchedule().isEmpty()) {
            return config.getFirstPaymentDate();
        }

        // Get the last item in schedule
        int lastIndex = config.getSchedule().size() - 1;
        return config.getSchedule().get(lastIndex).getDueDate();
    }

    private String serializeAddress(Object address) {
        // Simple serialization for now
        // Can be improved with proper JSON serialization
        return address != null ? address.toString() : null;
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private void handlePaymentFailure(InstallmentPaymentEntity payment, String reason) {
        log.warn("╔════════════════════════════════════════════════════════════╗");
        log.warn("║         HANDLING PAYMENT FAILURE                           ║");
        log.warn("╚════════════════════════════════════════════════════════════╝");
        log.warn("Payment ID: {}", payment.getPaymentId());
        log.warn("Failure Reason: {}", reason);

        try {
            // Record failure on payment
            payment.recordFailedPayment(reason);
            paymentRepo.save(payment);

            log.warn("✓ Payment marked as FAILED");
            log.warn("  Retry Count: {}", payment.getRetryCount());
            log.warn("  Can Retry: {}", payment.canRetry());

            // Update agreement
            InstallmentAgreementEntity agreement = payment.getAgreement();
            agreement.recordMissedPayment();
            agreementRepo.save(agreement);

            log.warn("✓ Agreement updated with missed payment");
            log.warn("  Default Count: {}", agreement.getDefaultCount());
            log.warn("  Agreement Status: {}", agreement.getAgreementStatus());

            // Check if defaulted
            if (agreement.getAgreementStatus() == AgreementStatus.DEFAULTED) {
                log.error("⚠ AGREEMENT DEFAULTED - Escalation required!");
                log.error("  Agreement: {}", agreement.getAgreementNumber());
                log.error("  Customer: {}", agreement.getCustomer().getUserName());
                log.error("  Default Count: {}", agreement.getDefaultCount());

                // TODO: Escalate to collections
                // collectionsService.escalateDefaultedAgreement(agreement);
                log.error("[TODO] Escalate to collections service");
            }

            // TODO: Send failure notification
            // notificationService.sendPaymentFailedNotification(
            //     agreement.getCustomer(),
            //     agreement,
            //     payment
            // );
            log.warn("[TODO] Send payment failure notification");

            log.warn("╚════════════════════════════════════════════════════════════╝");

        } catch (Exception e) {
            log.error("Error while handling payment failure", e);
            // Don't throw - failure is already being handled
        }
    }


    private void updateNextPaymentInfo(InstallmentAgreementEntity agreement) {
        log.debug("Updating next payment info for agreement: {}",
                agreement.getAgreementId());

        // Find next scheduled payment
        List<InstallmentPaymentEntity> upcomingPayments = paymentRepo
                .findByAgreementAndPaymentStatusOrderByPaymentNumberAsc(
                        agreement, PaymentStatus.SCHEDULED);

        if (!upcomingPayments.isEmpty()) {
            InstallmentPaymentEntity nextPayment = upcomingPayments.get(0);
            agreement.setNextPaymentDate(nextPayment.getDueDate());
            agreement.setNextPaymentAmount(nextPayment.getScheduledAmount());

            log.debug("Next payment: #{} due on {} (Amount: {} TZS)",
                    nextPayment.getPaymentNumber(),
                    nextPayment.getDueDate(),
                    nextPayment.getScheduledAmount());
        } else {
            // No more payments
            agreement.setNextPaymentDate(null);
            agreement.setNextPaymentAmount(null);

            log.debug("No more upcoming payments");
        }
    }


    private void handleAgreementCompletion(InstallmentAgreementEntity agreement) {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         AGREEMENT COMPLETION                           ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Agreement: {}", agreement.getAgreementNumber());

        LocalDateTime now = LocalDateTime.now();

        agreement.setAgreementStatus(AgreementStatus.COMPLETED);
        agreement.setCompletedAt(now);

        log.info("✓ Agreement marked as COMPLETED");

        if (agreement.getFulfillmentTiming() == FulfillmentTiming.AFTER_PAYMENT &&
                agreement.getOrderId() == null) {

            log.info("Fulfillment Type: AFTER_PAYMENT");
            log.info("Order should be created now (via event)");

            // Publish event for order creation
            try {
                InstallmentAgreementCompletedEvent event =
                        new InstallmentAgreementCompletedEvent(
                                this,
                                agreement.getAgreementId(),
                                agreement,
                                now,
                                false  // Not early payoff
                        );

                eventPublisher.publishEvent(event);

                log.info("✓ InstallmentAgreementCompletedEvent published");

            } catch (Exception e) {
                log.error("Failed to publish InstallmentAgreementCompletedEvent", e);
            }

        } else if (agreement.getOrderId() != null) {
            log.info("Order already exists: {}", agreement.getOrderId());
            // TODO: Mark order as fully paid
            log.info("[TODO] Mark order as fully paid");
        }

        agreementRepo.save(agreement);

        log.info("╚════════════════════════════════════════════════════════╝");
    }

}