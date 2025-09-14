package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.ShopResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.ShopSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.UpdateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    // CREATE SHOP
    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createShop(
            @Valid @RequestBody CreateShopRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        ShopEntity savedShop = shopService.createShop(request);
        ShopResponse shopResponse = buildShopResponse(savedShop);

        System.out.println("Hello here its "+ savedShop.getOwner().getFirstName());

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop created successfully", shopResponse)
        );
    }

    // NON-PAGEABLE ENDPOINTS
    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShops() {
        List<ShopEntity> shops = shopService.getAllShops();
        List<ShopResponse> shopResponses = shops.stream()
                .map(this::buildShopResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("All shops retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsByStatus(
            @PathVariable ShopStatus status) {
        List<ShopEntity> shops = shopService.getAllShopsByStatus(status);
        List<ShopResponse> shopResponses = shops.stream()
                .map(this::buildShopResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shops with status " + status + " retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/featured")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllFeaturedShops() {
        List<ShopEntity> shops = shopService.getAllFeaturedShops();
        List<ShopResponse> shopResponses = shops.stream()
                .map(this::buildShopResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Featured shops retrieved successfully", shopResponses)
        );
    }

    // PAGEABLE ENDPOINTS
    @GetMapping("/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.getAllShopsPaged(page, size);
        return ResponseEntity.ok(buildPagedResponse(shopPage, "Shops retrieved successfully"));
    }

    @GetMapping("/status/{status}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsByStatusPaged(
            @PathVariable ShopStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.getAllShopsByStatusPaged(status, page, size);
        return ResponseEntity.ok(buildPagedResponse(shopPage, "Shops with status " + status + " retrieved successfully"));
    }

    @GetMapping("/featured/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllFeaturedShopsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.getAllFeaturedShopsPaged(page, size);
        return ResponseEntity.ok(buildPagedResponse(shopPage, "Featured shops retrieved successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<GlobeSuccessResponseBuilder> searchShopsByName(
            @RequestParam String term,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.searchShopsByName(term, page, size);
        return ResponseEntity.ok(buildPagedResponse(shopPage, "Shop search completed successfully"));
    }

    // UTILITY METHODS
    private GlobeSuccessResponseBuilder buildPagedResponse(Page<ShopEntity> shopPage, String message) {
        List<ShopResponse> shopResponses = shopPage.getContent().stream()
                .map(this::buildShopResponse)
                .toList();

        var responseData = new Object() {
            public final List<ShopResponse> shops = shopResponses;
            public final int currentPage = shopPage.getNumber();
            public final int pageSize = shopPage.getSize();
            public final long totalElements = shopPage.getTotalElements();
            public final int totalPages = shopPage.getTotalPages();
            public final boolean hasNext = shopPage.hasNext();
            public final boolean hasPrevious = shopPage.hasPrevious();
            public final boolean isFirst = shopPage.isFirst();
            public final boolean isLast = shopPage.isLast();
        };

        return GlobeSuccessResponseBuilder.success(message, responseData);
    }

    @GetMapping("/summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsSummary() {
        List<ShopEntity> shops = shopService.getAllShopsSummary();
        List<ShopSummaryResponse> summaryResponses = shops.stream()
                .map(this::buildShopSummaryResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop summaries retrieved successfully", summaryResponses)
        );
    }

    @GetMapping("/summary/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsSummaryPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.getAllShopsSummaryPaged(page, size);
        return ResponseEntity.ok(buildSummaryPagedResponse(shopPage, "Shop summaries retrieved successfully"));
    }

    private GlobeSuccessResponseBuilder buildSummaryPagedResponse(Page<ShopEntity> shopPage, String message) {
        List<ShopSummaryResponse> summaryResponses = shopPage.getContent().stream()
                .map(this::buildShopSummaryResponse)
                .toList();

        var responseData = new Object() {
            public final List<ShopSummaryResponse> shops = summaryResponses;
            public final int currentPage = shopPage.getNumber();
            public final int pageSize = shopPage.getSize();
            public final long totalElements = shopPage.getTotalElements();
            public final int totalPages = shopPage.getTotalPages();
            public final boolean hasNext = shopPage.hasNext();
            public final boolean hasPrevious = shopPage.hasPrevious();
            public final boolean isFirst = shopPage.isFirst();
            public final boolean isLast = shopPage.isLast();
        };

        return GlobeSuccessResponseBuilder.success(message, responseData);
    }

    @PutMapping("/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateShop(
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateShopRequest request)
            throws ItemNotFoundException, RandomExceptions {

        ShopEntity updatedShop = shopService.updateShop(shopId, request);
        ShopResponse shopResponse = buildShopResponse(updatedShop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop updated successfully", shopResponse)
        );
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopById(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopEntity shop = shopService.getShopById(shopId);
        ShopResponse shopResponse = buildShopResponse(shop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop retrieved successfully", shopResponse)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopsByCategory(
            @PathVariable UUID categoryId) throws ItemNotFoundException {

        List<ShopEntity> shops = shopService.getShopsByCategory(categoryId);
        List<ShopResponse> shopResponses = shops.stream()
                .map(this::buildShopResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shops by category retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/category/{categoryId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopsByCategoryPaged(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopEntity> shopPage = shopService.getShopsByCategoryPaged(categoryId, page, size);
        return ResponseEntity.ok(buildPagedResponse(shopPage, "Shops by category retrieved successfully"));
    }

    private ShopSummaryResponse buildShopSummaryResponse(ShopEntity shop) {
        return ShopSummaryResponse.builder()
                .shopId(shop.getShopId())
                .shopName(shop.getShopName())
                .shopSlug(shop.getShopSlug())
                .shopDescription(shop.getShopDescription())
                .tagline(shop.getTagline())
                .logoUrl(shop.getLogoUrl())
                .shopType(shop.getShopType())
                .status(shop.getStatus())
                .city(shop.getCity())
                .region(shop.getRegion())
                .categoryName(shop.getCategory().getCategoryName())
                .ownerName(shop.getOwner().getFirstName() + " " + shop.getOwner().getLastName())
                .isVerified(shop.getIsVerified())
                .isFeatured(shop.getIsFeatured())
                .build();
    }

    private ShopResponse buildShopResponse(ShopEntity shop) {
        return ShopResponse.builder()
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
    }
}