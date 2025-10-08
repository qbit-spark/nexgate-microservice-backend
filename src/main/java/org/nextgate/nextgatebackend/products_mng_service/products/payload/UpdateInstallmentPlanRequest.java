package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;

@Data
public class UpdateInstallmentPlanRequest {

    @Size(min = 3, max = 100, message = "Plan name must be between 3 and 100 characters")
    private String planName;

    private PaymentFrequency paymentFrequency;

    @Min(value = 1, message = "Custom frequency days must be at least 1")
    private Integer customFrequencyDays;

    @Min(value = 2, message = "Number of payments must be at least 2")
    @Max(value = 120, message = "Number of payments cannot exceed 120")
    private Integer numberOfPayments;

    @DecimalMin(value = "0.0", message = "APR cannot be negative")
    @DecimalMax(value = "36.0", message = "APR cannot exceed 36%")
    @Digits(integer = 2, fraction = 2, message = "APR must have at most 2 digits and 2 decimal places")
    private BigDecimal apr;

    @Min(value = 10, message = "Minimum down payment must be at least 10%")
    @Max(value = 50, message = "Minimum down payment cannot exceed 50%")
    private Integer minDownPaymentPercent;

    @Min(value = 0, message = "Grace period days cannot be negative")
    @Max(value = 60, message = "Grace period days cannot exceed 60")
    private Integer gracePeriodDays;

    private FulfillmentTiming fulfillmentTiming;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;

    private Boolean isFeatured;

    private Boolean isActive;
}