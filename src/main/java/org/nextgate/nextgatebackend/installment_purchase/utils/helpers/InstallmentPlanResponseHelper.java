package org.nextgate.nextgatebackend.installment_purchase.utils.helpers;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductDetailedResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductPublicResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InstallmentPlanResponseHelper {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ========================================
    // BUILD INSTALLMENT OPTIONS FOR PRODUCT DETAIL
    // ========================================

    /**
     * Build complete installment options response for detailed product view
     * Shows only ACTIVE plans
     */
    public ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse
    buildDetailedInstallmentOptions(ProductEntity product) {

        if (!product.isInstallmentAvailable()) {
            return ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse.builder()
                    .isEnabled(false)
                    .isAvailable(false)
                    .build();
        }

        // Filter only active plans
        List<InstallmentPlanEntity> activePlans = product.getInstallmentPlans().stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()))
                .toList();

        if (activePlans.isEmpty()) {
            return ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse.builder()
                    .isEnabled(true)
                    .isAvailable(false)
                    .build();
        }

        // Build plan responses
        List<ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse> planResponses =
                activePlans.stream()
                        .map(plan -> buildDetailedPlanResponse(plan, product.getPrice()))
                        .collect(Collectors.toList());

        return ProductDetailedResponse.OrderingLimitsResponse.InstallmentOptionsDetailedResponse.builder()
                .isEnabled(true)
                .isAvailable(true)
                .plans(planResponses)
                .eligibilityStatus("ELIGIBLE")
                .creditCheckRequired(false)
                .build();
    }

    // ========================================
    // BUILD INSTALLMENT OPTIONS FOR PUBLIC VIEW
    // ========================================

    /**
     * Build public installment options response
     * Shows only ACTIVE plans with minimal information
     */
    public ProductPublicResponse.InstallmentPublicResponse buildPublicInstallmentOptions(ProductEntity product) {

        if (!product.isInstallmentAvailable()) {
            return ProductPublicResponse.InstallmentPublicResponse.builder()
                    .isAvailable(false)
                    .build();
        }

        // Filter only active plans
        List<InstallmentPlanEntity> activePlans = product.getInstallmentPlans().stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()))
                .collect(Collectors.toList());

        if (activePlans.isEmpty()) {
            return ProductPublicResponse.InstallmentPublicResponse.builder()
                    .isAvailable(false)
                    .build();
        }

        // Build simple plan responses
        List<ProductPublicResponse.InstallmentPlanPublicResponse> planResponses =
                activePlans.stream()
                        .map(this::buildPublicPlanResponse)
                        .collect(Collectors.toList());

        return ProductPublicResponse.InstallmentPublicResponse.builder()
                .isAvailable(true)
                .plans(planResponses)
                .build();
    }

    // ========================================
    // BUILD DETAILED PLAN RESPONSE
    // ========================================

    /**
     * Build detailed installment plan response with calculations and schedule
     */
    private ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse
    buildDetailedPlanResponse(InstallmentPlanEntity plan, BigDecimal productPrice) {

        // Calculate financial details
        InstallmentCalculations calculations = calculateInstallmentFinancials(plan, productPrice);

        // Generate payment schedule
        List<ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse> schedule =
                generatePaymentSchedule(plan, calculations);

        // Build calculation response
        ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse calculationResponse =
                ProductDetailedResponse.OrderingLimitsResponse.InstallmentCalculationResponse.builder()
                        .downPayment(calculations.downPayment)
                        .remainingAmount(calculations.remainingAmount)
                        .totalInterest(calculations.totalInterest)
                        .paymentAmount(calculations.paymentAmount)
                        .totalAmount(calculations.totalAmount)
                        .build();

        return ProductDetailedResponse.OrderingLimitsResponse.InstallmentPlanDetailedResponse.builder()
                .planId(plan.getPlanId().toString())
                .duration(plan.getNumberOfPayments())
                .interval(plan.getPaymentFrequency().getDisplayName())
                .interestRate(plan.getApr())
                .description(plan.getPlanName())
                .calculations(calculationResponse)
                .paymentSchedule(schedule)
                .isPopular(Boolean.TRUE.equals(plan.getIsFeatured()))
                .build();
    }

    // ========================================
    // BUILD PUBLIC PLAN RESPONSE
    // ========================================

    /**
     * Build simple public plan response (no calculations or schedule)
     */
    private ProductPublicResponse.InstallmentPlanPublicResponse buildPublicPlanResponse(InstallmentPlanEntity plan) {
        return ProductPublicResponse.InstallmentPlanPublicResponse.builder()
                .duration(plan.getNumberOfPayments())
                .interval(plan.getPaymentFrequency().getDisplayName())
                .interestRate(plan.getApr())
                .description(plan.getPlanName())
                .build();
    }

    // ========================================
    // FINANCIAL CALCULATIONS
    // ========================================

    /**
     * Calculate all financial details for an installment plan
     */
    private InstallmentCalculations calculateInstallmentFinancials(
            InstallmentPlanEntity plan,
            BigDecimal productPrice) {

        InstallmentCalculations calc = new InstallmentCalculations();

        // 1. Calculate down payment
        calc.downPayment = productPrice
                .multiply(BigDecimal.valueOf(plan.getMinDownPaymentPercent()))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

        // 2. Calculate remaining amount to finance
        calc.remainingAmount = productPrice.subtract(calc.downPayment);

        // 3. Calculate total interest (simple interest: principal × rate)
        calc.totalInterest = calc.remainingAmount
                .multiply(plan.getApr())
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);

        // 4. Total amount to pay in installments (principal + interest)
        BigDecimal totalInstallmentAmount = calc.remainingAmount.add(calc.totalInterest);

        // 5. Calculate payment amount per period
        calc.paymentAmount = totalInstallmentAmount
                .divide(BigDecimal.valueOf(plan.getNumberOfPayments()), SCALE, ROUNDING);

        // 6. Calculate total amount (product price + interest)
        calc.totalAmount = productPrice.add(calc.totalInterest);

        return calc;
    }

    // ========================================
    // PAYMENT SCHEDULE GENERATION
    // ========================================

    /**
     * Generate complete payment schedule for an installment plan
     */
    private List<ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse>
    generatePaymentSchedule(InstallmentPlanEntity plan, InstallmentCalculations calculations) {

        List<ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse> schedule = new ArrayList<>();

        // Start date is today + grace period
        LocalDateTime startDate = LocalDateTime.now().plusDays(plan.getPaymentStartDelayDays());

        for (int paymentNumber = 1; paymentNumber <= plan.getNumberOfPayments(); paymentNumber++) {
            LocalDateTime dueDate = calculatePaymentDueDate(plan, startDate, paymentNumber);

            schedule.add(
                    ProductDetailedResponse.OrderingLimitsResponse.PaymentScheduleResponse.builder()
                            .paymentNumber(paymentNumber)
                            .amount(calculations.paymentAmount)
                            .dueDate(dueDate)
                            .description(buildPaymentDescription(plan, paymentNumber))
                            .build()
            );
        }

        return schedule;
    }

    // ========================================
    // DUE DATE CALCULATION
    // ========================================

    /**
     * Calculate due date for a specific payment based on frequency
     */
    private LocalDateTime calculatePaymentDueDate(
            InstallmentPlanEntity plan,
            LocalDateTime startDate,
            int paymentNumber) {

        if (paymentNumber == 1) {
            return startDate; // First payment is on start date
        }

        int periodsElapsed = paymentNumber - 1;

        return switch (plan.getPaymentFrequency()) {
            case DAILY -> startDate.plusDays(periodsElapsed);
            case WEEKLY -> startDate.plusWeeks(periodsElapsed);
            case BI_WEEKLY -> startDate.plusDays(periodsElapsed * 14L);
            case SEMI_MONTHLY -> calculateSemiMonthlyDueDate(startDate, periodsElapsed);
            case MONTHLY -> startDate.plusMonths(periodsElapsed);
            case QUARTERLY -> startDate.plusMonths(periodsElapsed * 3L);
            case CUSTOM_DAYS -> startDate.plusDays(periodsElapsed * (long) plan.getCustomFrequencyDays());
        };
    }

    /**
     * Calculate semi-monthly due dates (1st and 15th of each month)
     */
    private LocalDateTime calculateSemiMonthlyDueDate(LocalDateTime startDate, int periodsElapsed) {
        int startDay = startDate.getDayOfMonth();
        boolean startsOnFirstHalf = startDay <= 15;

        int monthsToAdd = periodsElapsed / 2;
        boolean isOddPayment = periodsElapsed % 2 == 1;

        LocalDateTime result = startDate.plusMonths(monthsToAdd);

        if (startsOnFirstHalf) {
            // Started on 1-15, so alternate: 15th, 1st, 15th, 1st...
            return isOddPayment ? result.withDayOfMonth(15) : result.withDayOfMonth(1);
        } else {
            // Started on 16-31, so alternate: 1st (next month), 15th, 1st, 15th...
            return isOddPayment ? result.plusMonths(1).withDayOfMonth(1) : result.withDayOfMonth(15);
        }
    }

    // ========================================
    // PAYMENT DESCRIPTION
    // ========================================

    /**
     * Build description for each payment
     */
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
            case CUSTOM_DAYS -> "Payment " + paymentNumber + " of " + plan.getNumberOfPayments();
        };
    }

    // ========================================
    // HELPER CLASS FOR CALCULATIONS
    // ========================================

    /**
     * Internal class to hold all calculation results
     */
    private static class InstallmentCalculations {
        BigDecimal downPayment;
        BigDecimal remainingAmount;
        BigDecimal totalInterest;
        BigDecimal paymentAmount;
        BigDecimal totalAmount;
    }
}