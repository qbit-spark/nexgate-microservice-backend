package org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads;

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
public class RatingResponse {

    private UUID ratingId;
    private UUID shopId;
    private String shopName;
    private UUID userId;
    private String userName;
    private Integer ratingValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
