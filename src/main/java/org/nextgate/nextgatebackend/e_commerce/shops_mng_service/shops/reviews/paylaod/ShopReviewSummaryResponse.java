package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopReviewSummaryResponse {

    private UUID shopId;
    private String shopName;
    private Long totalReviews;
    private Long activeReviews;
    private Long hiddenReviews;
    private Long flaggedReviews;
}
