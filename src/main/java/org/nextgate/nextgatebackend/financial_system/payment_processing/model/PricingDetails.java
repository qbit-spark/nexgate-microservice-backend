package com.qbitspark.jikoexpress.financial_system.payment_processing.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingDetails {
    private BigDecimal subtotal;
    private BigDecimal shippingFee;  // Nullable for events
    private BigDecimal taxAmount;    // Nullable for events
    private BigDecimal discount;     // Nullable
    private BigDecimal total;
    @Builder.Default
    private String currency = "TZS";
}