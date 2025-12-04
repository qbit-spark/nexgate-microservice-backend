package org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPlanResponse;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPreviewRequest;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads.InstallmentPreviewResponse;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.service.PublicInstallmentService;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.utils.InstallmentCalculator;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicInstallmentServiceImpl implements PublicInstallmentService {

    private final InstallmentPlanRepo planRepo;
    private final ProductRepo productRepo;
    private final InstallmentCalculator calculator;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final Integer MAX_DOWN_PAYMENT_PERCENT = 50; // Platform limit

    @Override
    @Transactional
    public List<InstallmentPlanResponse> getAvailablePlans(UUID productId) throws ItemNotFoundException {
        log.info("Fetching available installment plans for product: {}", productId);

        // 1. Fetch product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Product not found with ID: " + productId));

        // 2. Check if installment is enabled
        if (!product.isInstallmentAvailable()) {
            log.info("Installment not available for product: {}", productId);
            return List.of(); // Return empty list
        }

        // 3. Fetch only ACTIVE plans
        List<InstallmentPlanEntity> activePlans = planRepo
                .findByProductAndIsActiveTrueOrderByDisplayOrderAsc(product);

        if (activePlans.isEmpty()) {
            log.info("No active installment plans found for product: {}", productId);
            return List.of();
        }

        log.info("Found {} active plans for product", activePlans.size());

        // 4. Convert to response DTOs
        return activePlans.stream()
                .map(plan -> buildPlanResponse(plan, product.getPrice()))
                .collect(Collectors.toList());    }

    @Override
    public InstallmentPreviewResponse calculatePreview(InstallmentPreviewRequest request) throws ItemNotFoundException, BadRequestException {
        log.info("Calculating installment preview for plan: {}", request.getPlanId());
        log.debug("Request details - Price: {}, Quantity: {}, Down: {}%",
                request.getProductPrice(), request.getQuantity(), request.getDownPaymentPercent());

        // 1. Fetch plan
        InstallmentPlanEntity plan = planRepo.findById(request.getPlanId())
                .orElseThrow(() -> new ItemNotFoundException(
                        "Installment plan not found with ID: " + request.getPlanId()));

        // 2. Validate plan is active
        if (!plan.getIsActive()) {
            throw new BadRequestException("This installment plan is not currently available");
        }

        // 3. Validate down payment percentage
        validateDownPaymentPercent(request.getDownPaymentPercent(), plan);

        // 4. Calculate financial breakdown
        BigDecimal totalProductCost = request.getProductPrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        BigDecimal downPaymentAmount = calculateDownPayment(
                totalProductCost, request.getDownPaymentPercent());

        BigDecimal financedAmount = totalProductCost.subtract(downPaymentAmount);

        // 5. Calculate period rate
        BigDecimal periodRate = calculator.calculatePeriodRate(
                plan.getApr(),
                plan.getPaymentFrequency(),
                plan.getCustomFrequencyDays()
        );

        // 6. Calculate monthly payment using amortization
        BigDecimal monthlyPayment = calculator.calculateMonthlyPayment(
                financedAmount,
                periodRate,
                plan.getNumberOfPayments()
        );

        // 7. Calculate totals
        BigDecimal totalPayments = monthlyPayment.multiply(
                BigDecimal.valueOf(plan.getNumberOfPayments()));
        BigDecimal totalInterest = totalPayments.subtract(financedAmount);
        BigDecimal grandTotal = downPaymentAmount.add(totalPayments);

        // 8. Calculate dates
        LocalDateTime firstPaymentDate = LocalDateTime.now()
                .plusDays(plan.getPaymentStartDelayDays());
        LocalDateTime lastPaymentDate = calculateLastPaymentDate(
                firstPaymentDate, plan);

        // 9. Generate payment schedule
        List<InstallmentPreviewResponse.PaymentSchedulePreview> schedule =
                generatePaymentSchedule(
                        financedAmount,
                        monthlyPayment,
                        periodRate,
                        plan.getNumberOfPayments(),
                        firstPaymentDate,
                        plan
                );

        // 10. Build comparison info
        InstallmentPreviewResponse.ComparisonInfo comparison =
                buildComparisonInfo(totalProductCost, grandTotal, totalInterest);

        // 11. Build response
        InstallmentPreviewResponse response = InstallmentPreviewResponse.builder()
                // Plan info
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .planDescription(buildPlanDescription(plan))

                // Schedule info
                .paymentFrequency(plan.getPaymentFrequency().getDisplayName())
                .numberOfPayments(plan.getNumberOfPayments())
                .durationDisplay(plan.getCalculatedDurationDisplay())
                .apr(plan.getApr())
                .paymentStartDelayDays(plan.getPaymentStartDelayDays())

                // Product info
                .productPrice(request.getProductPrice())
                .quantity(request.getQuantity())
                .totalProductCost(totalProductCost)

                // Down payment
                .downPaymentPercent(request.getDownPaymentPercent())
                .downPaymentAmount(downPaymentAmount)
                .minDownPaymentPercent(plan.getMinDownPaymentPercent())
                .maxDownPaymentPercent(MAX_DOWN_PAYMENT_PERCENT)
                .minDownPaymentAmount(calculateDownPayment(totalProductCost,
                        plan.getMinDownPaymentPercent()))
                .maxDownPaymentAmount(calculateDownPayment(totalProductCost,
                        MAX_DOWN_PAYMENT_PERCENT))

                // Financial breakdown
                .financedAmount(financedAmount)
                .monthlyPaymentAmount(monthlyPayment)
                .totalInterestAmount(totalInterest)
                .totalAmount(grandTotal)
                .currency("TZS")

                // Dates
                .firstPaymentDate(firstPaymentDate)
                .lastPaymentDate(lastPaymentDate)

                // Schedule
                .schedule(schedule)

                // Comparison
                .comparison(comparison)

                // Fulfillment
                .fulfillmentTiming(plan.getFulfillmentTiming().name())
                .fulfillmentDescription(buildFulfillmentDescription(plan))

                .build();

        log.info("Preview calculated successfully:");
        log.info("  Down Payment: {} TZS", downPaymentAmount);
        log.info("  Monthly Payment: {} TZS", monthlyPayment);
        log.info("  Total Interest: {} TZS", totalInterest);
        log.info("  Grand Total: {} TZS", grandTotal);

        return response;    }


    // ========================================
    // HELPER METHODS
    // ========================================

    private InstallmentPlanResponse buildPlanResponse(
            InstallmentPlanEntity plan,
            BigDecimal productPrice) {

        // Calculate preview at minimum down payment
        BigDecimal minDownAmount = calculateDownPayment(
                productPrice, plan.getMinDownPaymentPercent());
        BigDecimal financedAmount = productPrice.subtract(minDownAmount);

        BigDecimal periodRate = calculator.calculatePeriodRate(
                plan.getApr(),
                plan.getPaymentFrequency(),
                plan.getCustomFrequencyDays()
        );

        BigDecimal monthlyPayment = calculator.calculateMonthlyPayment(
                financedAmount,
                periodRate,
                plan.getNumberOfPayments()
        );

        BigDecimal totalPayments = monthlyPayment.multiply(
                BigDecimal.valueOf(plan.getNumberOfPayments()));
        BigDecimal totalInterest = totalPayments.subtract(financedAmount);
        BigDecimal grandTotal = productPrice.add(totalInterest);

        LocalDateTime firstPaymentDate = LocalDateTime.now()
                .plusDays(plan.getPaymentStartDelayDays());
        LocalDateTime lastPaymentDate = calculateLastPaymentDate(firstPaymentDate, plan);

        // Build preview
        InstallmentPlanResponse.InstallmentPreview preview =
                new InstallmentPlanResponse.InstallmentPreview();
        preview.setProductPrice(productPrice);
        preview.setMinDownPaymentAmount(minDownAmount);
        preview.setMaxDownPaymentAmount(calculateDownPayment(productPrice,
                MAX_DOWN_PAYMENT_PERCENT));
        preview.setFinancedAmountExample(financedAmount);
        preview.setPaymentAmountExample(monthlyPayment);
        preview.setTotalInterestExample(totalInterest);
        preview.setTotalCostExample(grandTotal);
        preview.setFirstPaymentDateExample(firstPaymentDate);
        preview.setLastPaymentDateExample(lastPaymentDate);

        // Build response
        InstallmentPlanResponse response = new InstallmentPlanResponse();
        response.setPlanId(plan.getPlanId());
        response.setPlanName(plan.getPlanName());
        response.setPaymentFrequency(plan.getPaymentFrequency());
        response.setPaymentFrequencyDisplay(plan.getPaymentFrequency().getDisplayName());
        response.setCustomFrequencyDays(plan.getCustomFrequencyDays());
        response.setNumberOfPayments(plan.getNumberOfPayments());
        response.setDuration(plan.getCalculatedDurationDisplay());
        response.setApr(plan.getApr());
        response.setMinDownPaymentPercent(plan.getMinDownPaymentPercent());
        response.setPaymentStartDelayDays(plan.getPaymentStartDelayDays());
        response.setFulfillmentTiming(plan.getFulfillmentTiming());
        response.setIsActive(plan.getIsActive());
        response.setIsFeatured(plan.getIsFeatured());
        response.setDisplayOrder(plan.getDisplayOrder());
        response.setPreview(preview);

        return response;
    }

    private void validateDownPaymentPercent(
            Integer percent,
            InstallmentPlanEntity plan) throws BadRequestException {

        if (percent < plan.getMinDownPaymentPercent()) {
            throw new BadRequestException(String.format(
                    "Down payment must be at least %d%% for this plan",
                    plan.getMinDownPaymentPercent()
            ));
        }

        if (percent > MAX_DOWN_PAYMENT_PERCENT) {
            throw new BadRequestException(String.format(
                    "Down payment cannot exceed %d%%",
                    MAX_DOWN_PAYMENT_PERCENT
            ));
        }
    }

    private BigDecimal calculateDownPayment(BigDecimal totalCost, Integer percent) {
        return totalCost.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    private LocalDateTime calculateLastPaymentDate(
            LocalDateTime firstPaymentDate,
            InstallmentPlanEntity plan) {

        int paymentsElapsed = plan.getNumberOfPayments() - 1;

        return switch (plan.getPaymentFrequency()) {
            case DAILY -> firstPaymentDate.plusDays(paymentsElapsed);
            case WEEKLY -> firstPaymentDate.plusWeeks(paymentsElapsed);
            case BI_WEEKLY -> firstPaymentDate.plusDays(paymentsElapsed * 14L);
            case SEMI_MONTHLY -> firstPaymentDate.plusDays(paymentsElapsed * 15L);
            case MONTHLY -> firstPaymentDate.plusMonths(paymentsElapsed);
            case QUARTERLY -> firstPaymentDate.plusMonths(paymentsElapsed * 3L);
            case CUSTOM_DAYS -> firstPaymentDate.plusDays(
                    paymentsElapsed * (long) plan.getCustomFrequencyDays());
        };
    }

    private List<InstallmentPreviewResponse.PaymentSchedulePreview> generatePaymentSchedule(
            BigDecimal principal,
            BigDecimal paymentAmount,
            BigDecimal periodRate,
            Integer numberOfPayments,
            LocalDateTime firstPaymentDate,
            InstallmentPlanEntity plan) {

        List<InstallmentPreviewResponse.PaymentSchedulePreview> schedule = new ArrayList<>();
        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= numberOfPayments; i++) {
            // Calculate interest on remaining balance
            BigDecimal interestPortion = remainingBalance.multiply(periodRate)
                    .setScale(SCALE, ROUNDING);

            // Principal is payment minus interest
            BigDecimal principalPortion = paymentAmount.subtract(interestPortion);

            // Handle last payment rounding
            if (i == numberOfPayments) {
                principalPortion = remainingBalance;
                interestPortion = paymentAmount.subtract(principalPortion);
            }

            // Update remaining balance
            remainingBalance = remainingBalance.subtract(principalPortion);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            // Calculate due date
            LocalDateTime dueDate = calculatePaymentDueDate(
                    firstPaymentDate, i, plan);

            // Create schedule item
            InstallmentPreviewResponse.PaymentSchedulePreview item =
                    InstallmentPreviewResponse.PaymentSchedulePreview.builder()
                            .paymentNumber(i)
                            .dueDate(dueDate)
                            .amount(paymentAmount)
                            .principalPortion(principalPortion)
                            .interestPortion(interestPortion)
                            .remainingBalance(remainingBalance)
                            .description(buildPaymentDescription(plan, i))
                            .build();

            schedule.add(item);
        }

        return schedule;
    }

    private LocalDateTime calculatePaymentDueDate(
            LocalDateTime firstPaymentDate,
            Integer paymentNumber,
            InstallmentPlanEntity plan) {

        if (paymentNumber == 1) {
            return firstPaymentDate;
        }

        int paymentsElapsed = paymentNumber - 1;

        return switch (plan.getPaymentFrequency()) {
            case DAILY -> firstPaymentDate.plusDays(paymentsElapsed);
            case WEEKLY -> firstPaymentDate.plusWeeks(paymentsElapsed);
            case BI_WEEKLY -> firstPaymentDate.plusDays(paymentsElapsed * 14L);
            case SEMI_MONTHLY -> calculateSemiMonthlyDate(firstPaymentDate, paymentsElapsed);
            case MONTHLY -> firstPaymentDate.plusMonths(paymentsElapsed);
            case QUARTERLY -> firstPaymentDate.plusMonths(paymentsElapsed * 3L);
            case CUSTOM_DAYS -> firstPaymentDate.plusDays(
                    paymentsElapsed * (long) plan.getCustomFrequencyDays());
        };
    }

    private LocalDateTime calculateSemiMonthlyDate(LocalDateTime start, int paymentsElapsed) {
        int day = start.getDayOfMonth();
        boolean startsOnFirst = day <= 15;

        int monthsToAdd = paymentsElapsed / 2;
        boolean isOddPayment = paymentsElapsed % 2 == 1;

        LocalDateTime result = start.plusMonths(monthsToAdd);

        if (startsOnFirst) {
            return isOddPayment ? result.withDayOfMonth(15) : result.withDayOfMonth(1);
        } else {
            return isOddPayment ? result.plusMonths(1).withDayOfMonth(1) :
                    result.withDayOfMonth(15);
        }
    }

    private InstallmentPreviewResponse.ComparisonInfo buildComparisonInfo(
            BigDecimal productPrice,
            BigDecimal totalWithInstallment,
            BigDecimal interestAmount) {

        BigDecimal additionalCostPercent = interestAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(productPrice, SCALE, ROUNDING);

        return InstallmentPreviewResponse.ComparisonInfo.builder()
                .payingUpfront(productPrice)
                .payingWithInstallment(totalWithInstallment)
                .additionalCost(interestAmount)
                .additionalCostPercent(additionalCostPercent)
                .build();
    }

    private String buildPlanDescription(InstallmentPlanEntity plan) {
        return String.format("Pay in %d %s installments at %s%% APR",
                plan.getNumberOfPayments(),
                plan.getPaymentFrequency().getDisplayName().toLowerCase(),
                plan.getApr());
    }

    private String buildPaymentDescription(InstallmentPlanEntity plan, int paymentNumber) {
        if (paymentNumber == plan.getNumberOfPayments()) {
            return "Final payment";
        }

        return switch (plan.getPaymentFrequency()) {
            case DAILY -> "Day " + paymentNumber + " payment";
            case WEEKLY -> "Week " + paymentNumber + " payment";
            case BI_WEEKLY -> "Bi-weekly payment " + paymentNumber;
            case SEMI_MONTHLY -> "Semi-monthly payment " + paymentNumber;
            case MONTHLY -> "Month " + paymentNumber + " payment";
            case QUARTERLY -> "Quarter " + paymentNumber + " payment";
            case CUSTOM_DAYS -> "Payment " + paymentNumber + " of " +
                    plan.getNumberOfPayments();
        };
    }

    private String buildFulfillmentDescription(InstallmentPlanEntity plan) {
        return switch (plan.getFulfillmentTiming()) {
            case IMMEDIATE -> "Product ships immediately after down payment";
            case AFTER_PAYMENT -> "Product ships after final payment is completed";
        };
    }

}
