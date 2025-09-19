package org.nextgate.nextgatebackend.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopProductsSummary {
    private UUID shopId;
    private String shopName;
    private String shopSlug;
    private int totalProducts;
    private int activeProducts;
    private int inactiveProducts;
    private int draftProducts;
    private int outOfStockProducts;
    private int archivedProducts;
    private int lowStockProducts;
    private List<RecentProduct> recentProducts;
}

