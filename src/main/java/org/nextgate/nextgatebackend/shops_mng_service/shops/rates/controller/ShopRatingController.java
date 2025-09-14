package org.nextgate.nextgatebackend.shops_mng_service.shops.rates.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.entity.ShopRatingEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.CreateRatingRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.RatingResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.ShopRatingSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.UpdateRatingRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.service.ShopRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops/ratings")
@RequiredArgsConstructor
public class ShopRatingController {

    private final ShopRatingService shopRatingService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createRating(
            @Valid @RequestBody CreateRatingRequest request)
            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        ShopRatingEntity rating = shopRatingService.createRating(request);
        RatingResponse response = buildRatingResponse(rating);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Rating created successfully", response)
        );
    }

    @PutMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateRating(
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateRatingRequest request)
            throws ItemNotFoundException, RandomExceptions {

        ShopRatingEntity rating = shopRatingService.updateRating(shopId, request);
        RatingResponse response = buildRatingResponse(rating);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Rating updated successfully", response)
        );
    }

    @DeleteMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteRating(
            @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        shopRatingService.deleteRating(shopId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Rating deleted successfully")
        );
    }

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getRatingsByShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        List<ShopRatingEntity> ratings = shopRatingService.getRatingsByShop(shopId);
        List<RatingResponse> responses = ratings.stream()
                .map(this::buildRatingResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop ratings retrieved successfully", responses)
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getRatingsByUser(
            @PathVariable UUID userId) throws ItemNotFoundException {

        List<ShopRatingEntity> ratings = shopRatingService.getRatingsByUser(userId);
        List<RatingResponse> responses = ratings.stream()
                .map(this::buildRatingResponse)
                .toList();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("User ratings retrieved successfully", responses)
        );
    }

    @GetMapping("/my-rating/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyRatingForShop(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopRatingEntity rating = shopRatingService.getUserRatingForShop(shopId);
        RatingResponse response = buildRatingResponse(rating);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Your rating retrieved successfully", response)
        );
    }

    @GetMapping("/summary/shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopRatingSummary(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        ShopRatingSummaryResponse summary = shopRatingService.getShopRatingSummary(shopId);

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Shop rating summary retrieved successfully", summary)
        );
    }

    private RatingResponse buildRatingResponse(ShopRatingEntity rating) {
        return RatingResponse.builder()
                .ratingId(rating.getRatingId())
                .shopId(rating.getShop().getShopId())
                .shopName(rating.getShop().getShopName())
                .userId(rating.getUser().getId())
                .userName(rating.getUser().getFirstName() + " " + rating.getUser().getLastName())
                .ratingValue(rating.getRatingValue())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}