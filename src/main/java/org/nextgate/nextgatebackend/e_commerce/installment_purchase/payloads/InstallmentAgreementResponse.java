package org.nextgate.nextgatebackend.e_commerce.installment_purchase.payloads;

import lombok.Data;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.AgreementStatus;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class InstallmentAgreementResponse {

    // Identification
    private UUID agreementId;
    private String agreementNumber;

    // Customer
    private UUID customerId;
    private String customerName;
    private String customerEmail;

    // Product
    private UUID productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Integer quantity;

    // Shop
    private UUID shopId;
    private String shopName;

    // Plan reference
    private UUID selectedPlanId;
    private String planName;

    // Payment terms
    private PaymentFrequency paymentFrequency;
    private String paymentFrequencyDisplay; // "Monthly", "Bi-weekly"
    private Integer customFrequencyDays;
    private Integer numberOfPayments;
    private String duration; // "6 months", "12 weeks"
    private BigDecimal apr;
    private Integer paymentStartDelayDays;

    // Financial breakdown
    private BigDecimal downPaymentAmount;
    private BigDecimal financedAmount;
    private BigDecimal monthlyPaymentAmount;
    private BigDecimal totalInterestAmount;
    private BigDecimal totalAmount;
    private String currency;

    // Progress
    private Integer paymentsCompleted;
    private Integer paymentsRemaining;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private Double progressPercentage;

    // Next payment
    private LocalDateTime nextPaymentDate;
    private BigDecimal nextPaymentAmount;

    // Status
    private AgreementStatus agreementStatus;
    private Integer defaultCount;

    // Timing
    private LocalDateTime createdAt;
    private LocalDateTime firstPaymentDate;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime completedAt;

    // Fulfillment
    private FulfillmentTiming fulfillmentTiming;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private UUID orderId;

    // Addresses
    private CheckoutSessionResponse.ShippingAddressResponse shippingAddress;
    private CheckoutSessionResponse.BillingAddressResponse billingAddress;

    // Payment schedule (full)
    private List<InstallmentPaymentResponse> payments;

    // Actions available
    private Boolean canMakeEarlyPayment;
    private Boolean canCancel;
    private Boolean canUpdatePaymentMethod;

    private Boolean canMakeFlexiblePayment;
    private BigDecimal minimumFlexiblePayment;  // Next incomplete payment amount
    private BigDecimal maximumFlexiblePayment;  // Total remaining
    private Integer paymentsPartiallyPaid;      // Count of partial payments
}