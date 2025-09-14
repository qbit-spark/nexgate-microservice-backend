package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.UpdateShopRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ShopService {

    ShopEntity createShop(CreateShopRequest request) throws ItemReadyExistException, ItemNotFoundException;

    List<ShopEntity> getAllShops();
    List<ShopEntity> getAllShopsByStatus(ShopStatus status);
    List<ShopEntity> getAllFeaturedShops();

    Page<ShopEntity> getAllShopsPaged(int page, int size);
    Page<ShopEntity> getAllShopsByStatusPaged(ShopStatus status, int page, int size);
    Page<ShopEntity> getAllFeaturedShopsPaged(int page, int size);
    Page<ShopEntity> searchShopsByName(String searchTerm, int page, int size);

    List<ShopEntity> getAllShopsSummary();
    Page<ShopEntity> getAllShopsSummaryPaged(int page, int size);

    ShopEntity updateShop(UUID shopId, UpdateShopRequest request) throws ItemNotFoundException, RandomExceptions;
    ShopEntity getShopById(UUID shopId) throws ItemNotFoundException;
    List<ShopEntity> getShopsByCategory(UUID categoryId) throws ItemNotFoundException;
    Page<ShopEntity> getShopsByCategoryPaged(UUID categoryId, int page, int size) throws ItemNotFoundException;
}