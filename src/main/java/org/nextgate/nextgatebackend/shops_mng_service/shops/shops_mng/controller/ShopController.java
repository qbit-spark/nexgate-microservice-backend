package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.ShopResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.ShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createShop(
            @Valid @RequestBody CreateShopRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        ShopEntity savedShop = shopService.createShop(request);

        ShopResponse shopResponse = buildShopResponse(savedShop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .message("Shop created successfully")
                        .data(shopResponse)
                        .build()
        );
    }

    private ShopResponse buildShopResponse(ShopEntity shop) {
        ShopResponse shopResponse = ShopResponse.builder()
                .shopId(shop.getShopId())
                .shopName(shop.getShopName())
                .shopSlug(shop.getShopSlug())
                .shopDescription(shop.getShopDescription())
                .tagline(shop.getTagline())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .shopImages(shop.getShopImages())
                .ownerId(shop.getOwner().getId())
                .ownerName(shop.getOwner().getFirstName() + " " + shop.getOwner().getLastName())
                .categoryId(shop.getCategory().getCategoryId())
                .categoryName(shop.getCategory().getCategoryName())
                .shopType(shop.getShopType())
                .status(shop.getStatus())
                .phoneNumber(shop.getPhoneNumber())
                .email(shop.getEmail())
                .websiteUrl(shop.getWebsiteUrl())
                .socialMediaLinks(shop.getSocialMediaLinks())
                .address(shop.getAddress())
                .city(shop.getCity())
                .region(shop.getRegion())
                .postalCode(shop.getPostalCode())
                .countryCode(shop.getCountryCode())
                .latitude(shop.getLatitude())
                .longitude(shop.getLongitude())
                .locationNotes(shop.getLocationNotes())
                .businessRegistrationNumber(shop.getBusinessRegistrationNumber())
                .taxNumber(shop.getTaxNumber())
                .licenseNumber(shop.getLicenseNumber())
                .establishedYear(shop.getEstablishedYear())
                .isVerified(shop.getIsVerified())
                .verificationBadge(shop.getVerificationBadge())
                .trustScore(shop.getTrustScore())
                .lastSeenTime(shop.getLastSeenTime())
                .isFeatured(shop.getIsFeatured())
                .featuredUntil(shop.getFeaturedUntil())
                .promotionText(shop.getPromotionText())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .approvedAt(shop.getApprovedAt())
                .build();

        return shopResponse;
    }
}