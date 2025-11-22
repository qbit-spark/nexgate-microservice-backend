package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentProduct {
    private UUID productId;
    private String productName;
    private String productSlug;
    private BigDecimal price;
    private Integer stockQuantity;
    private ProductStatus status;
    private LocalDateTime createdAt;
}
