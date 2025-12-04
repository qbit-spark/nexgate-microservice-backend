package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.payload.UpdateShopRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ShopService {

    ShopEntity createShop(CreateShopRequest request) throws ItemReadyExistException, ItemNotFoundException;

    List<ShopEntity> getAllShops();

    Page<ShopEntity> getAllShopsPaged(int page, int size);

    ShopEntity updateShop(UUID shopId, UpdateShopRequest request) throws ItemNotFoundException, RandomExceptions;
    ShopEntity getShopById(UUID shopId) throws ItemNotFoundException;
    ShopEntity getShopByIdDetailed(UUID shopId) throws ItemNotFoundException, RandomExceptions;
    List<ShopEntity> getShopsByCategory(UUID categoryId) throws ItemNotFoundException;
    Page<ShopEntity> getShopsByCategoryPaged(UUID categoryId, int page, int size) throws ItemNotFoundException;

    ShopEntity approveShop(UUID shopId, boolean approve) throws ItemNotFoundException;

    List<ShopEntity> getMyShops() throws ItemNotFoundException;
    Page<ShopEntity> getMyShopsPaged( int page, int size) throws ItemNotFoundException;

    List<ShopEntity> getFeaturedShops();
    Page<ShopEntity> getFeaturedShopsPaged(int page, int size);
}