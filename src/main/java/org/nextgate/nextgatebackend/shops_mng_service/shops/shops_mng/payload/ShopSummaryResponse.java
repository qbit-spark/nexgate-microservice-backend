package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopSummaryResponse {

    private UUID shopId;
    private String shopName;
    private String shopSlug;
    private String shopDescription;
    private String tagline;
    private String logoUrl;
    private ShopType shopType;
    private ShopStatus status;
    private String city;
    private String region;
    private String categoryName;
    private String ownerName;
    private Boolean isVerified;
    private Boolean isFeatured;
}