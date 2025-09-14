package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;
import org.springframework.data.domain.Page;

import java.util.List;

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
}