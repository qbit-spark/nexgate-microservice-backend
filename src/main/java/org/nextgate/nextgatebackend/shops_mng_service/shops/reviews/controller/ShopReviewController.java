package org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.CreateReviewRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.ReviewResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.ShopReviewSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.UpdateReviewRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.service.ShopReviewService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops/reviews")
@RequiredArgsConstructor
public class ShopReviewController {

    private final ShopReviewService shopReviewService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createReview(
            @Valid @RequestBody CreateReviewRequest request)
            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        ShopReviewEntity review = shopReviewService.createReview(request);
        ReviewResponse response = buildReviewResponse(review);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Review created successfully", response)
        );
    }

    @PutMapping("/shop/{shopId}")
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

    @DeleteMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteReview(
            @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        shopReviewService.deleteReview(shopId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Review deleted successfully")
        );
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveReviewsByShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        List<ShopReviewEntity> reviews = shopReviewService.getActiveReviewsByShop(shopId);
        List<ReviewResponse> responses = reviews.stream()
                .map(this::buildReviewResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop reviews retrieved successfully", responses)
        );
    }

    @GetMapping("/shop/{shopId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveReviewsByShopPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopReviewEntity> reviewPage = shopReviewService.getActiveReviewsByShopPaged(shopId, page, size);
        return ResponseEntity.ok(buildPagedResponse(reviewPage, "Shop reviews retrieved successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getReviewsByUser(
            @PathVariable UUID userId) throws ItemNotFoundException {

        List<ShopReviewEntity> reviews = shopReviewService.getReviewsByUser(userId);
        List<ReviewResponse> responses = reviews.stream()
                .map(this::buildReviewResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("User reviews retrieved successfully", responses)
        );
    }

    @GetMapping("/user/{userId}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getReviewsByUserPaged(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        Page<ShopReviewEntity> reviewPage = shopReviewService.getReviewsByUserPaged(userId, page, size);
        return ResponseEntity.ok(buildPagedResponse(reviewPage, "User reviews retrieved successfully"));
    }

    @GetMapping("/my-review/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyReviewForShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopReviewEntity review = shopReviewService.getUserReviewForShop(shopId);
        ReviewResponse response = buildReviewResponse(review);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Your review retrieved successfully", response)
        );
    }

    @GetMapping("/summary/shop/{shopId}")
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
}