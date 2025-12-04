package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPlansListResponse {

    // Product info
    private UUID productId;
    private String productName;
    private BigDecimal productPrice;
    private Boolean installmentEnabled;

    // Plan statistics
    private Integer totalPlans;
    private Long activePlans;

    // Plans list
    private List<InstallmentPlanResponse> plans;
}