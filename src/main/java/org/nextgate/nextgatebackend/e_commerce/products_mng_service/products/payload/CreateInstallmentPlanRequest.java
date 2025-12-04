package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;

@Data
public class CreateInstallmentPlanRequest {

    @NotBlank(message = "Plan name is required")
    @Size(min = 3, max = 100, message = "Plan name must be between 3 and 100 characters")
    private String planName;

    @NotNull(message = "Payment frequency is required")
    private PaymentFrequency paymentFrequency;

    @Min(value = 1, message = "Custom frequency days must be at least 1")
    private Integer customFrequencyDays; // Only used when paymentFrequency = CUSTOM_DAYS

    @NotNull(message = "Number of payments is required")
    @Min(value = 2, message = "Number of payments must be at least 2")
    @Max(value = 120, message = "Number of payments cannot exceed 120")
    private Integer numberOfPayments;

    @NotNull(message = "APR is required")
    @DecimalMin(value = "0.0", message = "APR cannot be negative")
    @DecimalMax(value = "36.0", message = "APR cannot exceed 36%")
    @Digits(integer = 2, fraction = 2, message = "APR must have at most 2 digits and 2 decimal places")
    private BigDecimal apr;

    @NotNull(message = "Minimum down payment percentage is required")
    @Min(value = 10, message = "Minimum down payment must be at least 10%")
    @Max(value = 50, message = "Minimum down payment cannot exceed 50%")
    private Integer minDownPaymentPercent;

    @NotNull(message = "Payment start delay days is required")
    @Min(value = 0, message = "Payment start delay days cannot be negative")
    @Max(value = 60, message = "Payment start delay days cannot exceed 60")
    private Integer paymentStartDelayDays;

    @NotNull(message = "Fulfillment timing is required")
    private FulfillmentTiming fulfillmentTiming;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder = 0;

    private Boolean isFeatured = false;

    private Boolean isActive = true;

    // Validation
    @AssertTrue(message = "Custom frequency days is required when payment frequency is CUSTOM_DAYS")
    public boolean isValidCustomFrequency() {
        if (paymentFrequency == PaymentFrequency.CUSTOM_DAYS) {
            return customFrequencyDays != null && customFrequencyDays > 0;
        }
        return true;
    }
}