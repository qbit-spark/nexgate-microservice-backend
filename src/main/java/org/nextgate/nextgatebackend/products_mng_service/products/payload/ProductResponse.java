package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    // BASIC INFO
    private UUID productId;
    private String productName;
    private String productSlug;
    private String productDescription;
    private String shortDescription;
    private List<String> productImages;

    // PRICING INFO
    private BigDecimal price;
    private BigDecimal comparePrice;
    private BigDecimal discountPercentage;
    private Boolean isOnSale;

    // INVENTORY INFO
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private Boolean isInStock;
    private Boolean isLowStock;
    private Boolean trackInventory;

    // PRODUCT DETAILS
    private String brand;
    private String sku;

    // PHYSICAL PROPERTIES
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    private ProductCondition condition;
    private ProductStatus status;

    // SEO AND TAGS
    private List<String> tags;
    private String metaTitle;
    private String metaDescription;

    // SHOP INFO (minimal to avoid circular references)
    private UUID shopId;
    private String shopName;
    private String shopSlug;

    // CATEGORY INFO (minimal to avoid circular references)
    private UUID categoryId;
    private String categoryName;

    // FEATURES
    private Boolean isFeatured;
    private Boolean isDigital;
    private Boolean requiresShipping;

    // SYSTEM FIELDS
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID createdBy;
    private UUID editedBy;
}
