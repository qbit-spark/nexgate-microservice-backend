package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload;

import lombok.Data;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateShopRequest {
    private String shopName;
    private String shopDescription;
    private String tagline;
    private String logoUrl;
    private String bannerUrl;
    private List<String> shopImages;
    private UUID categoryId;
    private ShopType shopType;
    private String phoneNumber;
    private String email;
    private String websiteUrl;
    private List<String> socialMediaLinks;
    private String address;
    private String city;
    private String region;
    private String postalCode;
    private String countryCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String locationNotes;
    private String promotionText;
}