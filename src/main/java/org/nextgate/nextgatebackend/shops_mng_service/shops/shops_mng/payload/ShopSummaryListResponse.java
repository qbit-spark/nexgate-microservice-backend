package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopType;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.VerificationBadge;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSummaryListResponse {
    private UUID shopId;
    private String shopName;
    private String shopSlug;
    private String shopDescription;
    private String tagline;
    private String logoUrl;
    private String bannerUrl;
    private List<String> shopImages;

    // Owner info (minimal to avoid loops)
    private UUID ownerId;
    private String ownerName;

    // Category info (minimal to avoid loops)
    private UUID categoryId;
    private String categoryName;

    private ShopType shopType;
    private ShopStatus status;

    // Contact
    private String phoneNumber;
    private String email;
    private String websiteUrl;
    private List<String> socialMediaLinks;

    // Location
    private String address;
    private String city;
    private String region;


    // Trust & verification
    private Boolean isVerified;
    private VerificationBadge verificationBadge;
    private BigDecimal trustScore;

    // Activity
    private LocalDateTime lastSeenTime;

    // System fields
    private Boolean isApproved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
}
