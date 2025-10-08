package org.nextgate.nextgatebackend.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutSessionResponse {

    private UUID sessionId;
    private CheckoutSessionType sessionType;
    private CheckoutSessionStatus status;

    // Customer info
    private UUID customerId;
    private String customerUserName;

    // Items
    private List<CheckoutItemResponse> items;

    // Pricing
    private PricingSummaryResponse pricing;

    // Addresses
    private ShippingAddressResponse shippingAddress;
    private BillingAddressResponse billingAddress;

    // Shipping
    private ShippingMethodResponse shippingMethod;

    // Payment
    private PaymentIntentResponse paymentIntent;
    private List<PaymentAttemptResponse> paymentAttempts;

    // Inventory
    private Boolean inventoryHeld;
    private LocalDateTime inventoryHoldExpiresAt;

    // Metadata
    private Map<String, Object> metadata;

    // Timestamps
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // References
    private UUID createdOrderId;
    private UUID cartId;
    private UUID groupIdToBeJoined; // ← EXISTING (for group purchase)

    // NEW: Installment fields
    private UUID selectedInstallmentPlanId;
    private InstallmentConfigResponse installmentConfig;

    // Nested response classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CheckoutItemResponse {
        private UUID itemId;
        private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
        private BigDecimal subtotal;
        private BigDecimal tax;
        private BigDecimal total;
        private UUID shopId;
        private String shopName;
        private String shopLogo;
        private Boolean availableForCheckout;
        private Integer availableQuantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PricingSummaryResponse {
        private BigDecimal subtotal;
        private BigDecimal discount;
        private BigDecimal shippingCost;
        private BigDecimal tax;
        private BigDecimal total;
        private String currency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShippingAddressResponse {
        private String fullName;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BillingAddressResponse {
        private Boolean sameAsShipping;
        private String fullName;
        private String addressLine1;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ShippingMethodResponse {
        private String id;
        private String name;
        private String carrier;
        private BigDecimal cost;
        private String estimatedDays;
        private String estimatedDelivery;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentIntentResponse {
        private String provider;
        private String clientSecret;
        private List<String> paymentMethods;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentAttemptResponse {
        private Integer attemptNumber;
        private String paymentMethod;
        private String status;
        private String errorMessage;
        private LocalDateTime attemptedAt;
        private String transactionId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InstallmentConfigResponse {
        private UUID planId;
        private String planName;
        private Integer termMonths;
        private BigDecimal apr;
        private Integer downPaymentPercent;
        private BigDecimal downPaymentAmount;
        private BigDecimal financedAmount;
        private BigDecimal monthlyPaymentAmount;
        private BigDecimal totalInterest;
        private BigDecimal totalAmount;
        private LocalDateTime firstPaymentDate;
        private Integer gracePeriodDays;
        private String fulfillmentTiming;
        private List<PaymentScheduleItemResponse> schedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentScheduleItemResponse {
        private Integer paymentNumber;
        private LocalDateTime dueDate;
        private BigDecimal amount;
        private BigDecimal principalPortion;
        private BigDecimal interestPortion;
        private BigDecimal remainingBalance;
    }
}