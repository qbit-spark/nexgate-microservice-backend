package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSummaryStatsResponse {

    private UUID shopId;
    private String shopName;

    // Rating Summary
    private Double averageRating;
    private Long totalRatings;
    private Map<Integer, Long> ratingDistribution;

    // Review Summary
    private Long totalReviews;
    private Long activeReviews;
    private Long hiddenReviews;
    private Long flaggedReviews;

    // User Activity Summary
    private List<UserActivitySummary> userActivities;
}