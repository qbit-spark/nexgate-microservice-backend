package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.entity.ShopRatingEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.service.ShopRatingService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.ReviewResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.service.ShopReviewService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.*;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final AccountRepo accountRepo;
    private final ShopRatingService shopRatingService;
    private final ShopReviewService shopReviewService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createShop(
            @Valid @RequestBody CreateShopRequest request)
            throws ItemReadyExistException, ItemNotFoundException {

        ShopEntity savedShop = shopService.createShop(request);
        ShopResponse shopResponse = buildShopResponse(savedShop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop created successfully", shopResponse)
        );
    }

    @GetMapping("/all")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShops() {
        List<ShopEntity> shops = shopService.getAllShops();
        List<ShopSummaryListResponse> shopResponses = shops.stream()
                .map(this::buildShopSummaryListResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("All shops retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/all-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllShopsPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ShopEntity> shopPage = shopService.getAllShopsPaged(page, size);
        return ResponseEntity.ok(buildPagedSummaryResponse(shopPage, "Shops retrieved successfully"));
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

    @GetMapping("/{shopId}/detailed")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopDetailedById(
            @PathVariable UUID shopId) throws ItemNotFoundException, RandomExceptions {

        ShopEntity shop = shopService.getShopByIdDetailed(shopId);
        ShopResponse shopResponse = buildShopResponse(shop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop retrieved successfully", shopResponse)
        );
    }

    @GetMapping("/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopById(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopEntity shop = shopService.getShopById(shopId);
        ShopSummaryListResponse shopResponse = buildShopSummaryListResponse(shop);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop retrieved successfully", shopResponse)
        );
    }

    @GetMapping("/my-shops")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyShops() throws ItemNotFoundException {

        List<ShopEntity> shops = shopService.getMyShops();

        List<ShopSummaryListResponse> shopResponses = shops.stream()
                .map(this::buildShopSummaryListResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("My shops retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/my-shops-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyShopsPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopEntity> shopPage = shopService.getMyShopsPaged(page, size);

        return ResponseEntity.ok(buildPagedSummaryResponse(shopPage, "My shops retrieved successfully"));
    }

    @PatchMapping("/{shopId}/approve-shop")
    public ResponseEntity<GlobeSuccessResponseBuilder> approveShop(@PathVariable UUID shopId, @RequestParam boolean approve) throws ItemNotFoundException, AccessDeniedException {

        validateRole(getAuthenticatedAccount(), "ROLE_SUPER_ADMIN","ROLE_STAFF_ADMIN");
        ShopEntity approved = shopService.approveShop(shopId, approve);
        ShopResponse shopResponse = buildShopResponse(approved);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop approval status changed successfully", shopResponse)
        );
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopsByCategory(
            @PathVariable UUID categoryId) throws ItemNotFoundException {

        List<ShopEntity> shops = shopService.getShopsByCategory(categoryId);
        List<ShopSummaryListResponse> shopResponses = shops.stream()
                .map(this::buildShopSummaryListResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shops by category retrieved successfully", shopResponses)
        );
    }

    @GetMapping("/category/{categoryId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopsByCategoryPaged(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopEntity> shopPage = shopService.getShopsByCategoryPaged(categoryId, page, size);
        return ResponseEntity.ok(buildPagedSummaryResponse(shopPage, "Shops by category retrieved successfully"));
    }

    // RESPONSE BUILDERS
    private ShopResponse buildShopResponse(ShopEntity shop) {
        // Get rating data
        Double averageRating = shopRatingService.getShopAverageRating(shop.getShopId());
        Long totalRatings = shopRatingService.getShopTotalRatings(shop.getShopId());

        // Get review data
        Long totalActiveReviews = shopReviewService.getShopActiveReviewCount(shop.getShopId());
        List<ShopReviewEntity> reviewEntities;
        try {
            reviewEntities = shopReviewService.getActiveReviewsByShop(shop.getShopId());
        } catch (ItemNotFoundException e) {
            reviewEntities = List.of(); // Empty list if no reviews found
        }

        // Convert review entities to response objects
        List<ReviewResponse> reviews = reviewEntities.stream()
                .map(this::buildReviewResponseForShop)
                .toList();

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
                .isApproved(shop.isApproved())
                // NEW: Rating and Review Summary
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .totalActiveReviews(totalActiveReviews)
                .reviews(reviews)
                .build();
    }

    private ShopSummaryListResponse buildShopSummaryListResponse(ShopEntity shop) {
        // Get rating and review data for summary
        Double averageRating = shopRatingService.getShopAverageRating(shop.getShopId());
        Long totalRatings = shopRatingService.getShopTotalRatings(shop.getShopId());
        Long totalActiveReviews = shopReviewService.getShopActiveReviewCount(shop.getShopId());
        List<ReviewSummary> topReviews = getTop5ReviewsForShop(shop.getShopId());

        return ShopSummaryListResponse.builder()
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
                .isVerified(shop.getIsVerified())
                .verificationBadge(shop.getVerificationBadge())
                .trustScore(shop.getTrustScore())
                .lastSeenTime(shop.getLastSeenTime())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .approvedAt(shop.getApprovedAt())
                .isApproved(shop.isApproved())
                // NEW: Rating and Review Summary
                .averageRating(averageRating)
                .totalRatings(totalRatings)
                .totalActiveReviews(totalActiveReviews)
                .topReviews(topReviews)
                .build();
    }

    // HELPER METHODS
    private ReviewResponse buildReviewResponseForShop(ShopReviewEntity review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .shopId(review.getShop().getShopId())
                .shopName(review.getShop().getShopName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .reviewText(review.getReviewText())
                .status(review.getStatus())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private List<ReviewSummary> getTop5ReviewsForShop(UUID shopId) {
        try {
            List<ShopReviewEntity> reviews = shopReviewService.getActiveReviewsByShop(shopId)
                    .stream()
                    .limit(5)
                    .toList();

            return reviews.stream()
                    .map(this::buildReviewSummary)
                    .toList();
        } catch (ItemNotFoundException e) {
            return List.of(); // Return empty list if no reviews found
        }
    }

    private ReviewSummary buildReviewSummary(ShopReviewEntity review) {
        return ReviewSummary.builder()
                .reviewId(review.getReviewId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .reviewText(review.getReviewText())
                .createdAt(review.getCreatedAt())
                .build();
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

    private GlobeSuccessResponseBuilder buildPagedSummaryResponse(Page<ShopEntity> shopPage, String message) {
        List<ShopSummaryListResponse> shopResponses = shopPage.getContent().stream()
                .map(this::buildShopSummaryListResponse)
                .toList();

        var responseData = new Object() {
            public final List<ShopSummaryListResponse> shops = shopResponses;
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

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }

        throw new ItemNotFoundException("User not authenticated");
    }

    public void validateRole(AccountEntity account, String... requiredRoles) throws AccessDeniedException {
        if (account == null) {
            throw new AccessDeniedException("Account not found");
        }

        if (account.getRoles() == null || account.getRoles().isEmpty()) {
            throw new AccessDeniedException("Account has no roles assigned");
        }

        // Get account's role names
        Set<String> accountRoleNames = account.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        // Check if an account has any of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(accountRoleNames::contains);

        if (!hasRequiredRole) {
            throw new AccessDeniedException("Access denied. Insufficient permissions.");
        }
    }
}