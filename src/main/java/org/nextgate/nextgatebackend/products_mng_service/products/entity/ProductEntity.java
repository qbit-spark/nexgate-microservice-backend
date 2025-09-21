package org.nextgate.nextgatebackend.products_mng_service.products.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;
import org.nextgate.nextgatebackend.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.ColorsJsonConverter;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.InstallmentPlansJsonConverter;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.SpecificationsJsonConverter;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products", indexes = {
        @Index(name = "idx_product_shop", columnList = "shop_id"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_slug", columnList = "productSlug"),
        @Index(name = "idx_product_specifications", columnList = "specifications")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(unique = true, nullable = false, length = 150)
    private String productSlug;

    @Column(length = 1000)
    private String productDescription;

    @Column(length = 200)
    private String shortDescription;

    // Product Images
    @Column(name = "product_images", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> productImages = new ArrayList<>();

    // Pricing - Essential fields
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal comparePrice; // Original price for showing discounts

    // Inventory - Simple but effective
    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    private Integer lowStockThreshold = 5;

    private Boolean trackInventory = true;

    // Product Details
    @Column(length = 100)
    private String brand;

    @Column(length = 50)
    private String sku; // Stock Keeping Unit - must be unique

    @Enumerated(EnumType.STRING)
    private ProductCondition condition = ProductCondition.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    // SEO and Tags
    @Column(name = "tags", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> tags = new ArrayList<>();

    private String metaTitle;
    private String metaDescription;

    // ===============================
    // NEW FIELDS - SPECIFICATIONS
    // ===============================

    @Column(name = "specifications", columnDefinition = "jsonb")
    @Convert(converter = SpecificationsJsonConverter.class)
    private Map<String, String> specifications = new HashMap<>();

    // ===============================
    // NEW FIELDS - COLORS
    // ===============================

    @Column(name = "colors", columnDefinition = "jsonb")
    @Convert(converter = ColorsJsonConverter.class)
    private List<Map<String, Object>> colors = new ArrayList<>();

    // ===============================
    // NEW FIELDS - ORDERING LIMITS
    // ===============================

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity = 1;

    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    @Column(name = "max_per_customer")
    private Integer maxPerCustomer;

    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;

    // ===============================
    // NEW FIELDS - GROUP BUYING
    // ===============================

    @Column(name = "group_buying_enabled")
    private Boolean groupBuyingEnabled = false;

    @Column(name = "group_min_size")
    private Integer groupMinSize;

    @Column(name = "group_max_size")
    private Integer groupMaxSize;

    @Column(name = "group_price", precision = 10, scale = 2)
    private BigDecimal groupPrice;

    @Column(name = "group_time_limit_hours")
    private Integer groupTimeLimitHours;

    @Column(name = "group_requires_minimum")
    private Boolean groupRequiresMinimum = true;

    // ===============================
    // NEW FIELDS - INSTALLMENT OPTIONS
    // ===============================

    @Column(name = "installment_enabled")
    private Boolean installmentEnabled = false;

    @Column(name = "installment_plans", columnDefinition = "jsonb")
    @Convert(converter = InstallmentPlansJsonConverter.class)
    private List<Map<String, Object>> installmentPlans = new ArrayList<>();

    @Column(name = "down_payment_required")
    private Boolean downPaymentRequired = false;

    @Column(name = "min_down_payment", precision = 10, scale = 2)
    private BigDecimal minDownPayment;

    @Column(name = "min_down_payment_percentage", precision = 5, scale = 2)
    private BigDecimal minDownPaymentPercentage;

    // Relationships - The Key Part!
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", referencedColumnName = "shopId", nullable = false)
    @JsonIgnoreProperties({"products", "hibernateLazyInitializer", "handler"})
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "categoryId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductCategoryEntity category;

    // Features
    private Boolean isFeatured = false;
    private Boolean isDigital = false;
    private Boolean requiresShipping = true;

    // System Fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private UUID createdBy;
    private UUID editedBy;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (productSlug == null && productName != null) {
            productSlug = generateSlugFromName(productName);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSlugFromName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // Business Logic Methods - Very Useful!
    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    public boolean isOnSale() {
        return comparePrice != null && comparePrice.compareTo(price) > 0;
    }

    public BigDecimal getDiscountAmount() {
        if (isOnSale()) {
            return comparePrice.subtract(price);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getDiscountPercentage() {
        if (isOnSale()) {
            return getDiscountAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(comparePrice, 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // ===============================
    // NEW BUSINESS LOGIC METHODS
    // ===============================

    public boolean hasSpecifications() {
        return specifications != null && !specifications.isEmpty();
    }

    public int getSpecificationCount() {
        return specifications != null ? specifications.size() : 0;
    }

    public boolean hasMultipleColors() {
        return colors != null && colors.size() > 1;
    }

    public int getColorCount() {
        return colors != null ? colors.size() : 0;
    }

    public boolean isGroupBuyingAvailable() {
        return groupBuyingEnabled != null && groupBuyingEnabled;
    }

    public boolean isInstallmentAvailable() {
        return installmentEnabled != null && installmentEnabled;
    }

    public BigDecimal getGroupDiscount() {
        if (isGroupBuyingAvailable() && groupPrice != null) {
            return price.subtract(groupPrice);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getGroupDiscountPercentage() {
        if (isGroupBuyingAvailable() && groupPrice != null) {
            return getGroupDiscount()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(price, 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public boolean canOrderQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) return false;
        if (minOrderQuantity != null && quantity < minOrderQuantity) return false;
        if (maxOrderQuantity != null && quantity > maxOrderQuantity) return false;
        return true;
    }

    public Integer getMaxAllowedQuantity() {
        if (maxOrderQuantity != null) {
            return Math.min(maxOrderQuantity, stockQuantity);
        }
        return stockQuantity;
    }
}