package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod.CreateReviewRequest;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod.ReviewResponse;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod.ShopReviewSummaryResponse;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.paylaod.UpdateReviewRequest;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.reviews.service.ShopReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops/reviews/{shopId}")
@RequiredArgsConstructor
public class ShopReviewController {

    private final ShopReviewService shopReviewService;
    private final AccountRepo accountRepo;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createReview(
            @Valid @RequestBody CreateReviewRequest request, @PathVariable UUID shopId)
            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        ShopReviewEntity review = shopReviewService.createReview(shopId, request);
        ReviewResponse response = buildReviewResponse(review);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Review created successfully", response)
        );
    }

    @PutMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> updateReview(
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateReviewRequest request)
            throws ItemNotFoundException, RandomExceptions {

        ShopReviewEntity review = shopReviewService.updateReview(shopId, request);
        ReviewResponse response = buildReviewResponse(review);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Review updated successfully", response)
        );
    }

    @DeleteMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteReview(
            @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        shopReviewService.deleteReview(shopId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Review deleted successfully")
        );
    }

    @GetMapping("/active-reviews-by-shop")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveReviewsByShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        AccountEntity currentUser = getAuthenticatedAccount();
        List<ShopReviewEntity> reviews = shopReviewService.getActiveReviewsByShop(shopId);

        List<ReviewResponse> responses = reviews.stream()
                .map(review -> buildReviewResponse(review, currentUser.getId())) // Use the overloaded method
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop reviews retrieved successfully", responses)
        );
    }

    @GetMapping("/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveReviewsByShopPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopReviewEntity> reviewPage = shopReviewService.getActiveReviewsByShopPaged(shopId, page, size);
        return ResponseEntity.ok(buildPagedResponse(reviewPage, "Shop reviews retrieved successfully"));
    }



    @GetMapping("/my-review")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyReviewForShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopReviewEntity review = shopReviewService.getUserReviewForShop(shopId);

        ReviewResponse response = null;
        if (review != null) {
            response = buildReviewResponse(review);
        }

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Your review retrieved successfully", response)
        );
    }

    @GetMapping("/summary-shop-review")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopReviewSummary(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopReviewSummaryResponse summary = shopReviewService.getShopReviewSummary(shopId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop review summary retrieved successfully", summary)
        );
    }

    private GlobeSuccessResponseBuilder buildPagedResponse(Page<ShopReviewEntity> reviewPage, String message) {
        List<ReviewResponse> reviewResponses = reviewPage.getContent().stream()
                .map(this::buildReviewResponse)
                .toList();

        var responseData = new Object() {
            public final List<ReviewResponse> reviews = reviewResponses;
            public final int currentPage = reviewPage.getNumber();
            public final int pageSize = reviewPage.getSize();
            public final long totalElements = reviewPage.getTotalElements();
            public final int totalPages = reviewPage.getTotalPages();
            public final boolean hasNext = reviewPage.hasNext();
            public final boolean hasPrevious = reviewPage.hasPrevious();
            public final boolean isFirst = reviewPage.isFirst();
            public final boolean isLast = reviewPage.isLast();
        };

        return GlobeSuccessResponseBuilder.success(message, responseData);
    }

    // Existing method - keep as is for backward compatibility
    private ReviewResponse buildReviewResponse(ShopReviewEntity review) {
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

    // New overloaded method with current user ID
    private ReviewResponse buildReviewResponse(ShopReviewEntity review, UUID currentUserId) {
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
                .isMyReview(review.getUser().getId().equals(currentUserId))
                .build();
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
}