package org.nextgate.nextgatebackend.installment_purchase.utils;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentFrequency;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class InstallmentCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // ========================================
    // MAIN CALCULATION METHOD
    // ========================================

    public CheckoutSessionEntity.InstallmentConfiguration calculateInstallmentConfig(
            InstallmentPlanEntity plan,
            BigDecimal productPrice,
            Integer quantity,
            Integer downPaymentPercent
    ) {
        log.debug("Calculating installment config for plan: {}, price: {}, qty: {}, down: {}%",
                plan.getPlanId(), productPrice, quantity, downPaymentPercent);

        // 1. Calculate base amounts
        BigDecimal totalProductCost = productPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal downPaymentAmount = calculateDownPayment(totalProductCost, downPaymentPercent);
        BigDecimal financedAmount = totalProductCost.subtract(downPaymentAmount);

        // 2. Calculate period interest rate
        BigDecimal periodRate = calculatePeriodRate(plan.getApr(), plan.getPaymentFrequency(),
                plan.getCustomFrequencyDays());

        // 3. Calculate monthly payment using amortization formula
        BigDecimal monthlyPayment = calculateMonthlyPayment(
                financedAmount, periodRate, plan.getNumberOfPayments());

        // 4. Calculate total amounts
        BigDecimal totalPayments = monthlyPayment.multiply(
                BigDecimal.valueOf(plan.getNumberOfPayments()));
        BigDecimal totalInterest = totalPayments.subtract(financedAmount);
        BigDecimal grandTotal = downPaymentAmount.add(totalPayments);

        // 5. Calculate first payment date (after grace period)
        LocalDateTime firstPaymentDate = LocalDateTime.now()
                .plusDays(plan.getGracePeriodDays());

        // 6. Generate full payment schedule
        List<CheckoutSessionEntity.PaymentScheduleItem> schedule =
                generatePaymentSchedule(
                        financedAmount,
                        monthlyPayment,
                        periodRate,
                        plan.getNumberOfPayments(),
                        firstPaymentDate,
                        plan.getPaymentFrequency(),
                        plan.getCustomFrequencyDays()
                );

        // 7. Build configuration object
        return CheckoutSessionEntity.InstallmentConfiguration.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .termMonths(plan.getNumberOfPayments()) // Approximate
                .apr(plan.getApr())
                .downPaymentPercent(downPaymentPercent)
                .downPaymentAmount(downPaymentAmount)
                .financedAmount(financedAmount)
                .monthlyPaymentAmount(monthlyPayment)
                .totalInterest(totalInterest)
                .totalAmount(grandTotal)
                .firstPaymentDate(firstPaymentDate)
                .gracePeriodDays(plan.getGracePeriodDays())
                .fulfillmentTiming(plan.getFulfillmentTiming().name())
                .schedule(schedule)
                .build();
    }

    // ========================================
    // HELPER CALCULATIONS
    // ========================================

    private BigDecimal calculateDownPayment(BigDecimal totalCost, Integer percent) {
        return totalCost.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    private BigDecimal calculatePeriodRate(
            BigDecimal apr,
            PaymentFrequency frequency,
            Integer customDays
    ) {
        // APR is annual, we need rate per payment period
        BigDecimal annualRate = apr.divide(BigDecimal.valueOf(100), 6, ROUNDING);

        BigDecimal periodsPerYear = switch (frequency) {
            case DAILY -> BigDecimal.valueOf(365);
            case WEEKLY -> BigDecimal.valueOf(52);
            case BI_WEEKLY -> BigDecimal.valueOf(26);
            case SEMI_MONTHLY -> BigDecimal.valueOf(24);
            case MONTHLY -> BigDecimal.valueOf(12);
            case QUARTERLY -> BigDecimal.valueOf(4);
            case CUSTOM_DAYS -> BigDecimal.valueOf(365)
                    .divide(BigDecimal.valueOf(customDays), 6, ROUNDING);
        };

        return annualRate.divide(periodsPerYear, 6, ROUNDING);
    }

    private BigDecimal calculateMonthlyPayment(
            BigDecimal principal,
            BigDecimal periodRate,
            Integer numberOfPayments
    ) {
        // Formula: M = P × [r(1+r)^n] / [(1+r)^n - 1]
        // Where: M = monthly payment, P = principal, r = period rate, n = number of payments

        if (periodRate.compareTo(BigDecimal.ZERO) == 0) {
            // No interest, simple division
            return principal.divide(BigDecimal.valueOf(numberOfPayments), SCALE, ROUNDING);
        }

        BigDecimal onePlusRate = BigDecimal.ONE.add(periodRate);
        BigDecimal powerTerm = onePlusRate.pow(numberOfPayments);

        BigDecimal numerator = principal.multiply(periodRate).multiply(powerTerm);
        BigDecimal denominator = powerTerm.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    private List<CheckoutSessionEntity.PaymentScheduleItem> generatePaymentSchedule(
            BigDecimal principal,
            BigDecimal paymentAmount,
            BigDecimal periodRate,
            Integer numberOfPayments,
            LocalDateTime firstPaymentDate,
            PaymentFrequency frequency,
            Integer customDays
    ) {
        List<CheckoutSessionEntity.PaymentScheduleItem> schedule = new ArrayList<>();
        BigDecimal remainingBalance = principal;

        for (int i = 1; i <= numberOfPayments; i++) {
            // Calculate interest on remaining balance
            BigDecimal interestPortion = remainingBalance.multiply(periodRate)
                    .setScale(SCALE, ROUNDING);

            // Principal is payment minus interest
            BigDecimal principalPortion = paymentAmount.subtract(interestPortion);

            // Handle last payment rounding
            if (i == numberOfPayments) {
                principalPortion = remainingBalance; // Pay off exactly
                interestPortion = paymentAmount.subtract(principalPortion);
            }

            // Update remaining balance
            remainingBalance = remainingBalance.subtract(principalPortion);
            if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
                remainingBalance = BigDecimal.ZERO;
            }

            // Calculate due date for this payment
            LocalDateTime dueDate = calculateDueDate(firstPaymentDate, i, frequency, customDays);

            // Create schedule item
            CheckoutSessionEntity.PaymentScheduleItem item =
                    CheckoutSessionEntity.PaymentScheduleItem.builder()
                            .paymentNumber(i)
                            .dueDate(dueDate)
                            .amount(paymentAmount)
                            .principalPortion(principalPortion)
                            .interestPortion(interestPortion)
                            .remainingBalance(remainingBalance)
                            .build();

            schedule.add(item);
        }

        return schedule;
    }

    private LocalDateTime calculateDueDate(
            LocalDateTime firstPaymentDate,
            Integer paymentNumber,
            PaymentFrequency frequency,
            Integer customDays
    ) {
        if (paymentNumber == 1) {
            return firstPaymentDate;
        }

        int paymentsElapsed = paymentNumber - 1;

        return switch (frequency) {
            case DAILY -> firstPaymentDate.plusDays(paymentsElapsed);
            case WEEKLY -> firstPaymentDate.plusWeeks(paymentsElapsed);
            case BI_WEEKLY -> firstPaymentDate.plusDays(paymentsElapsed * 14L);
            case SEMI_MONTHLY -> calculateSemiMonthlyDate(firstPaymentDate, paymentsElapsed);
            case MONTHLY -> firstPaymentDate.plusMonths(paymentsElapsed);
            case QUARTERLY -> firstPaymentDate.plusMonths(paymentsElapsed * 3L);
            case CUSTOM_DAYS -> firstPaymentDate.plusDays(paymentsElapsed * customDays);
        };
    }

    private LocalDateTime calculateSemiMonthlyDate(LocalDateTime start, int paymentsElapsed) {
        // Semi-monthly: 1st and 15th of each month
        int day = start.getDayOfMonth();
        boolean startsOnFirst = day <= 15;

        int monthsToAdd = paymentsElapsed / 2;
        boolean isOddPayment = paymentsElapsed % 2 == 1;

        LocalDateTime result = start.plusMonths(monthsToAdd);

        if (startsOnFirst) {
            return isOddPayment ? result.withDayOfMonth(15) : result.withDayOfMonth(1);
        } else {
            return isOddPayment ? result.plusMonths(1).withDayOfMonth(1) : result.withDayOfMonth(15);
        }
    }

    // ========================================
    // VALIDATION HELPERS
    // ========================================

    public boolean isValidDownPaymentPercent(Integer percent, Integer minPercent, Integer maxPercent) {
        if (percent == null) return false;
        return percent >= minPercent && percent <= maxPercent;
    }

    public BigDecimal calculateMinDownPaymentAmount(BigDecimal totalCost, Integer minPercent) {
        return calculateDownPayment(totalCost, minPercent);
    }

    public BigDecimal calculateMaxDownPaymentAmount(BigDecimal totalCost, Integer maxPercent) {
        return calculateDownPayment(totalCost, maxPercent);
    }
}