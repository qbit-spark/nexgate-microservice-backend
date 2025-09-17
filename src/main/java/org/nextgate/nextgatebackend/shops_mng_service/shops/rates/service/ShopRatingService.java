package org.nextgate.nextgatebackend.shops_mng_service.shops.rates.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.entity.ShopRatingEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.CreateRatingRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.ShopRatingSummaryResponse;
import org.nextgate.nextgatebackend.shops_mng_service.shops.rates.payloads.UpdateRatingRequest;


import java.util.List;
import java.util.UUID;

public interface ShopRatingService {

    ShopRatingEntity createRating(UUID shopId, CreateRatingRequest request) throws ItemNotFoundException, ItemReadyExistException, RandomExceptions;

    ShopRatingEntity updateRating(UUID shopId, UpdateRatingRequest request) throws ItemNotFoundException, RandomExceptions;

    void deleteRating(UUID shopId) throws ItemNotFoundException, RandomExceptions;

    List<ShopRatingEntity> getRatingsByShop(UUID shopId) throws ItemNotFoundException;

    ShopRatingEntity getUserRatingForShop(UUID shopId) throws ItemNotFoundException;

    ShopRatingSummaryResponse getShopRatingSummary(UUID shopId) throws ItemNotFoundException;
}