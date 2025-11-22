package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {
    private UUID reviewId;
    private String userName;
    private String reviewText;
    private LocalDateTime createdAt;
}
