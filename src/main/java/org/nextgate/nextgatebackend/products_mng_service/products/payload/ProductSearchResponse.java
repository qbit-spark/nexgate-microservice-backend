// Create ProductSearchResponse.java with nested DTOs
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
public class ProductSearchResponse {
    private SearchContents contents;
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
    public static class SearchContents {
        private ProductSummaryResponse.ShopSummaryForProducts shop;
        private List<ProductSummaryResponse> products;
        private Integer totalProducts;
        private SearchMetadata searchMetadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private String searchQuery;
        private List<ProductStatus> searchedStatuses;
        private String userType;
        private boolean canSearchAllStatuses;
    }
}