package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterResponse {
    private FilterContents contents;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterContents {
        private ProductSummaryResponse.ShopSummaryForProducts shop;
        private List<ProductSummaryResponse> products;
        private Integer totalProducts;
        private FilterMetadata filterMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterMetadata {
        private ProductFilterCriteria appliedFilters;
        private String userType;
        private List<ProductStatus> searchedStatuses;
        private boolean hasActiveFilters;
    }
}