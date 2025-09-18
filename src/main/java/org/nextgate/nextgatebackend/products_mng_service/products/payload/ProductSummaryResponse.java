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
public class ProductSummaryResponse {

    // ESSENTIAL INFO FOR LISTS
    private UUID productId;
    private String productName;
    private String productSlug;
    private String shortDescription;
    private List<String> productImages; // Usually just first image

    // PRICING
    private BigDecimal price;
    private BigDecimal comparePrice;
    private Boolean isOnSale;
    private BigDecimal discountPercentage;

    // INVENTORY STATUS
    private Boolean isInStock;
    private Boolean isLowStock;

    // BASIC DETAILS
    private String brand;
    private ProductCondition condition;
    private ProductStatus status;

    // SHOP INFO
    private UUID shopId;
    private String shopName;

    // CATEGORY INFO
    private UUID categoryId;
    private String categoryName;

    // FEATURES
    private Boolean isFeatured;
    private Boolean isDigital;

    // TIMESTAMPS
    private LocalDateTime createdAt;
}
