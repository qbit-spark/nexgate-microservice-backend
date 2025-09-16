package org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.entity.ShopReviewEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.CreateReviewRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.ShopReviewSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.reviews.paylaod.UpdateReviewRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ShopReviewService {

    ShopReviewEntity createReview(UUID shopId, CreateReviewRequest request) throws ItemNotFoundException, ItemReadyExistException, RandomExceptions;

    ShopReviewEntity updateReview(UUID shopId, UpdateReviewRequest request) throws ItemNotFoundException, RandomExceptions;

    void deleteReview(UUID shopId) throws ItemNotFoundException, RandomExceptions;

    List<ShopReviewEntity> getActiveReviewsByShop(UUID shopId) throws ItemNotFoundException;

    Page<ShopReviewEntity> getActiveReviewsByShopPaged(UUID shopId, int page, int size) throws ItemNotFoundException;

    ShopReviewEntity getUserReviewForShop(UUID shopId) throws ItemNotFoundException;

    ShopReviewSummaryResponse getShopReviewSummary(UUID shopId) throws ItemNotFoundException;
}