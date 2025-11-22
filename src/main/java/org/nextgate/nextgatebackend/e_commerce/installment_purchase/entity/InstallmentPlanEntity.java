package org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;
import org.nextgate.nextgatebackend.payment_methods.utils.MetadataJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "installment_plans", indexes = {
        @Index(name = "idx_plan_product", columnList = "product_id"),
        @Index(name = "idx_plan_shop", columnList = "shop_id"),
        @Index(name = "idx_plan_active", columnList = "isActive"),
        @Index(name = "idx_plan_featured", columnList = "isFeatured")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstallmentPlanEntity {

    // ========================================
    // PRIMARY IDENTIFICATION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID planId;

    @Column(nullable = false, length = 100)
    private String planName;  // "Quick Payment", "Standard", "Budget Plan"

    @Column(nullable = false)
    private Integer displayOrder = 0;  // For sorting on product page

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isFeatured = false;  // "Most Popular" badge

    // ========================================
    // RELATIONSHIPS
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    @JsonIgnoreProperties({"installmentPlans", "hibernateLazyInitializer", "handler"})
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", referencedColumnName = "shopId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ShopEntity shop;

    // ========================================
    // PAYMENT SCHEDULE CONFIGURATION
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency paymentFrequency;

    @Column(name = "custom_frequency_days")
    private Integer customFrequencyDays;  // Only used when paymentFrequency = CUSTOM_DAYS

    @Column(nullable = false)
    private Integer numberOfPayments;  // How many payments (2-120)

    // CALCULATED FIELDS (auto-generated, can be null initially)
    @Column(name = "calculated_duration_days")
    private Integer calculatedDurationDays;

    @Column(name = "calculated_duration_display", length = 50)
    private String calculatedDurationDisplay;  // "6 weeks", "3 months", "2 years"

    // ========================================
    // INTEREST & DOWN PAYMENT
    // ========================================

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal apr;  // Annual Percentage Rate (0-36%)

    @Column(name = "min_down_payment_percent", nullable = false)
    private Integer minDownPaymentPercent;  // Seller sets minimum (e.g., 20%)

    // Platform sets max (stored in application config, not here)

    // ========================================
    // GRACE PERIOD
    // ========================================

    @Column(name = "payment_start_delay_days", nullable = false)
    private Integer paymentStartDelayDays;  // Days before first payment due (0-60)


    // ========================================
    // FULFILLMENT
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentTiming fulfillmentTiming;

    // ========================================
    // TIMESTAMPS
    // ========================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========================================
    // METADATA
    // ========================================

    @Column(name = "metadata", columnDefinition = "jsonb")
    @Convert(converter = MetadataJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();

    // ========================================
    // LIFECYCLE HOOKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }

        // Auto-calculate duration if not set
        if (calculatedDurationDays == null) {
            calculateDuration();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        // Recalculate duration on update
        calculateDuration();
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    private void calculateDuration() {
        if (paymentFrequency == null || numberOfPayments == null) {
            return;
        }

        int daysPerPayment = getDaysPerPayment();
        this.calculatedDurationDays = daysPerPayment * numberOfPayments;
        this.calculatedDurationDisplay = generateDurationDisplay();
    }

    private int getDaysPerPayment() {
        if (paymentFrequency == PaymentFrequency.CUSTOM_DAYS && customFrequencyDays != null) {
            return customFrequencyDays;
        }

        if (paymentFrequency == PaymentFrequency.SEMI_MONTHLY) {
            return 15;  // Approximately
        }

        return paymentFrequency.getBaseDays();
    }

    private String generateDurationDisplay() {
        if (calculatedDurationDays == null) return null;

        int days = calculatedDurationDays;

        if (days < 7) {
            return days + (days == 1 ? " day" : " days");
        } else if (days < 30) {
            int weeks = days / 7;
            return weeks + (weeks == 1 ? " week" : " weeks");
        } else if (days < 365) {
            int months = days / 30;
            return months + (months == 1 ? " month" : " months");
        } else {
            int years = days / 365;
            return years + (years == 1 ? " year" : " years");
        }
    }

    public boolean isCustomFrequency() {
        return paymentFrequency == PaymentFrequency.CUSTOM_DAYS;
    }

    public boolean requiresCustomDays() {
        return isCustomFrequency() && customFrequencyDays == null;
    }

    public boolean isValid() {
        if (planName == null || planName.trim().isEmpty()) return false;
        if (paymentFrequency == null) return false;
        if (numberOfPayments == null || numberOfPayments < 2 || numberOfPayments > 120) return false;
        if (apr == null || apr.compareTo(BigDecimal.ZERO) < 0 || apr.compareTo(BigDecimal.valueOf(36)) > 0) return false;
        if (minDownPaymentPercent == null || minDownPaymentPercent < 10 || minDownPaymentPercent > 50) return false;
        if (paymentStartDelayDays == null || paymentStartDelayDays < 0 || paymentStartDelayDays > 60) return false;
        if (fulfillmentTiming == null) return false;
        if (requiresCustomDays()) return false;

        return true;
    }
}