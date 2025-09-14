package org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private UUID reviewId;
    private UUID shopId;
    private String shopName;
    private UUID userId;
    private String userName;
    private String reviewText;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
