package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload;

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
public class UserActivitySummary {

    private UUID userId;
    private String userName;

    // Review info (if they wrote a review)
    private UUID reviewId;
    private String reviewText;
    private ReviewStatus reviewStatus;
    private LocalDateTime reviewDate;

    // Rating info (if they gave a rating)
    private UUID ratingId;
    private Integer ratingValue;
    private LocalDateTime ratingDate;

    // Combined flags
    private boolean hasReview;
    private boolean hasRating;
}