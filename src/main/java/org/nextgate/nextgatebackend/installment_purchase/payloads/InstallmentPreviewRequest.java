package org.nextgate.nextgatebackend.installment_purchase.payloads;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for calculating installment preview
 * Used when customer wants to see payment breakdown before checkout
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPreviewRequest {

    /**
     * The installment plan ID customer selected
     */
    @NotNull(message = "Plan ID is required")
    private UUID planId;

    /**
     * Product price (for calculation)
     */
    @NotNull(message = "Product price is required")
    @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
    private BigDecimal productPrice;

    /**
     * Quantity (default 1 for installments)
     */
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    //@Max(value = 1, message = "Maximum quantity for installment is 1")
    private Integer quantity ;

    /**
     * Down payment percentage customer chooses
     * Must be between plan's min and platform's max (50%)
     */
    @NotNull(message = "Down payment percentage is required")
    @Min(value = 10, message = "Minimum down payment is 10%")
    @Max(value = 50, message = "Maximum down payment is 50%")
    private Integer downPaymentPercent;
}