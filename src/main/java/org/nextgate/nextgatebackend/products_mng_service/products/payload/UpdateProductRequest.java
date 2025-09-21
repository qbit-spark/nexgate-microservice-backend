package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class UpdateProductRequest {

    // BASIC FIELDS
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String productName;

    @Size(min = 10, max = 1000, message = "Product description must be between 10 and 1000 characters")
    private String productDescription;

    @Size(max = 200, message = "Short description must not exceed 200 characters")
    private String shortDescription;

    // PRICING
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Compare price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Compare price must have at most 8 digits and 2 decimal places")
    private BigDecimal comparePrice;

    // INVENTORY
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1, message = "Low stock threshold must be at least 1")
    @Max(value = 1000, message = "Low stock threshold cannot exceed 1000")
    private Integer lowStockThreshold;

    private Boolean trackInventory;

    // PRODUCT DETAILS
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    private ProductCondition condition;
    private ProductStatus status;

    private UUID categoryId;

    // COLLECTIONS
    private List<@URL(message = "Product image URLs must be valid") String> productImages;
    private List<@Size(max = 50, message = "Each tag must not exceed 50 characters") String> tags;

    // FEATURES
    private Boolean isFeatured;
    private Boolean isDigital;
    private Boolean requiresShipping;

    // SEO FIELDS
    @Size(max = 100, message = "Meta title must not exceed 100 characters")
    private String metaTitle;

    @Size(max = 200, message = "Meta description must not exceed 200 characters")
    private String metaDescription;

    // ===============================
    // SPECIFICATIONS
    // ===============================
    private Map<@NotBlank @Size(max = 100) String, @NotBlank @Size(max = 500) String> specifications;

    // ===============================
    // COLORS
    // ===============================
    private List<ColorRequest> colors;

    // ===============================
    // ORDERING LIMITS
    // ===============================
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minOrderQuantity;

    @Min(value = 1, message = "Maximum order quantity must be at least 1")
    private Integer maxOrderQuantity;

    private Boolean requiresApproval;

    // ===============================
    // GROUP BUYING
    // ===============================
    private Boolean groupBuyingEnabled;

    @Min(value = 2, message = "Group minimum size must be at least 2")
    private Integer groupMinSize;

    @Min(value = 2, message = "Group maximum size must be at least 2")
    private Integer groupMaxSize;

    @DecimalMin(value = "0.01", message = "Group price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Group price must have at most 8 digits and 2 decimal places")
    private BigDecimal groupPrice;

    @Min(value = 1, message = "Group time limit must be at least 1 hour")
    @Max(value = 8760, message = "Group time limit cannot exceed 1 year (8760 hours)")
    private Integer groupTimeLimitHours;

    private Boolean groupRequiresMinimum;

    // ===============================
    // INSTALLMENT OPTIONS
    // ===============================
    private Boolean installmentEnabled;

    private List<InstallmentPlanRequest> installmentPlans;

    private Boolean downPaymentRequired;

    @DecimalMin(value = "0.0", message = "Down payment percentage cannot be negative")
    @DecimalMax(value = "100.0", message = "Down payment percentage cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Down payment percentage must have at most 3 digits and 2 decimal places")
    private BigDecimal minDownPaymentPercentage;

    // ===============================
    // NESTED CLASSES (same as CreateProductRequest)
    // ===============================
    @Data
    public static class ColorRequest {
        @Size(max = 50, message = "Color name must not exceed 50 characters")
        private String name;

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Invalid hex color code format")
        private String hex;

        private List<@URL(message = "Color image URLs must be valid") String> images;

        @DecimalMin(value = "0.0", message = "Price adjustment cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Price adjustment must have at most 8 digits and 2 decimal places")
        private BigDecimal priceAdjustment;
    }

    @Data
    public static class InstallmentPlanRequest {
        @Min(value = 1, message = "Duration must be at least 1")
        private Integer duration;

        @Pattern(regexp = "^(DAYS|WEEKS|MONTHS)$", message = "Interval must be DAYS, WEEKS, or MONTHS")
        private String interval;

        @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
        @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
        @Digits(integer = 3, fraction = 2, message = "Interest rate must have at most 3 digits and 2 decimal places")
        private BigDecimal interestRate;

        @Size(max = 200, message = "Description must not exceed 200 characters")
        private String description;
    }

    // ===============================
    // VALIDATION METHODS (same logic as create, but all optional)
    // ===============================
    @AssertTrue(message = "Group maximum size must be greater than minimum size")
    public boolean isValidGroupSizes() {
        if (groupBuyingEnabled != null && groupBuyingEnabled &&
                groupMinSize != null && groupMaxSize != null) {
            return groupMaxSize >= groupMinSize;
        }
        return true;
    }

    @AssertTrue(message = "Group price must be less than regular price")
    public boolean isValidGroupPrice() {
        if (groupBuyingEnabled != null && groupBuyingEnabled &&
                groupPrice != null && price != null) {
            return groupPrice.compareTo(price) < 0;
        }
        return true;
    }

    @AssertTrue(message = "Maximum order quantity must be greater than minimum order quantity")
    public boolean isValidOrderQuantities() {
        if (minOrderQuantity != null && maxOrderQuantity != null) {
            return maxOrderQuantity >= minOrderQuantity;
        }
        return true;
    }

    @AssertTrue(message = "Compare price must be greater than regular price")
    public boolean isValidComparePrice() {
        if (comparePrice != null && price != null) {
            return comparePrice.compareTo(price) > 0;
        }
        return true;
    }
}