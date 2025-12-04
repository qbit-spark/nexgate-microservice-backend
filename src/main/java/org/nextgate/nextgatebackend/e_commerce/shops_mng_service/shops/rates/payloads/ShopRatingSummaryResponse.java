package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.rates.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopRatingSummaryResponse {

    private UUID shopId;
    private String shopName;
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution;
}