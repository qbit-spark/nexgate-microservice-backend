package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateProductRequest {

    // REQUIRED FIELDS
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String productName;

    @NotBlank(message = "Product description is required")
    @Size(min = 10, max = 1000, message = "Product description must be between 10 and 1000 characters")
    private String productDescription;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotNull(message = "Shop is required")
    private UUID shopId;

    // OPTIONAL FIELDS
    @Size(max = 200, message = "Short description must not exceed 200 characters")
    private String shortDescription;

    @DecimalMin(value = "0.01", message = "Compare price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Compare price must have at most 8 digits and 2 decimal places")
    private BigDecimal comparePrice;

    @Min(value = 1, message = "Low stock threshold must be at least 1")
    @Max(value = 1000, message = "Low stock threshold cannot exceed 1000")
    private Integer lowStockThreshold = 5;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    // PHYSICAL PROPERTIES
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    @Digits(integer = 5, fraction = 2, message = "Weight must have at most 5 digits and 2 decimal places")
    private BigDecimal weight;

    @DecimalMin(value = "0.0", message = "Length cannot be negative")
    @Digits(integer = 5, fraction = 2, message = "Length must have at most 5 digits and 2 decimal places")
    private BigDecimal length;

    @DecimalMin(value = "0.0", message = "Width cannot be negative")
    @Digits(integer = 5, fraction = 2, message = "Width must have at most 5 digits and 2 decimal places")
    private BigDecimal width;

    @DecimalMin(value = "0.0", message = "Height cannot be negative")
    @Digits(integer = 5, fraction = 2, message = "Height must have at most 5 digits and 2 decimal places")
    private BigDecimal height;

    // ENUMS WITH DEFAULTS
    private ProductCondition condition = ProductCondition.NEW;
    private ProductStatus status = ProductStatus.ACTIVE;

    // COLLECTIONS
    private List<@URL(message = "Product image URLs must be valid") String> productImages;
    private List<@Size(max = 50, message = "Each tag must not exceed 50 characters") String> tags;

    // FEATURES
    private Boolean trackInventory = true;
    private Boolean isFeatured = false;
    private Boolean isDigital = false;
    private Boolean requiresShipping = true;

    // SEO FIELDS
    @Size(max = 100, message = "Meta title must not exceed 100 characters")
    private String metaTitle;

    @Size(max = 200, message = "Meta description must not exceed 200 characters")
    private String metaDescription;
}
