package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductCondition;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterCriteria {

    // Price range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Product attributes
    private List<String> brands;
    private ProductCondition condition;
    private UUID categoryId;
    private List<String> tags;

    // Availability filters
    private Boolean inStock;
    private Boolean onSale;

    // Feature filters
    private Boolean hasGroupBuying;
    private Boolean hasInstallments;
    private Boolean hasMultipleColors;
    private Boolean isFeatured;

    // Status filter (for owners/admins)
    private List<ProductStatus> status;

    // Helper methods to check if filters are applied
    public boolean hasPriceFilter() {
        return minPrice != null || maxPrice != null;
    }

    public boolean hasBrandFilter() {
        return brands != null && !brands.isEmpty();
    }

    public boolean hasTagFilter() {
        return tags != null && !tags.isEmpty();
    }

    public boolean hasAnyFilter() {
        return hasPriceFilter() || hasBrandFilter() || hasTagFilter() ||
                condition != null || categoryId != null || inStock != null ||
                onSale != null || hasGroupBuying != null || hasInstallments != null ||
                hasMultipleColors != null || isFeatured != null;
    }
}
