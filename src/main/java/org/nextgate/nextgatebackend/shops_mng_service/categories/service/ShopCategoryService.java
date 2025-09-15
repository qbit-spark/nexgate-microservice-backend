package org.nextgate.nextgatebackend.shops_mng_service.categories.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod.CreateShopCategoryRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ShopCategoryService {


    GlobeSuccessResponseBuilder createShopCategory(CreateShopCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException;


    GlobeSuccessResponseBuilder updateShopCategory(UUID categoryId, CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException;

    // SINGLE ITEM RETRIEVAL
    GlobeSuccessResponseBuilder getShopCategory(UUID shopCategoryId)
            throws ItemNotFoundException;



    // NON-PAGEABLE LIST METHODS
    GlobeSuccessResponseBuilder getAllShopCategories(Boolean isActive);

    // PAGEABLE LIST METHODS
    Page<ShopCategoryEntity> getAllShopCategoriesPaged(Boolean isActive, int page, int size);

    GlobeSuccessResponseBuilder deleteShopCategory(UUID shopCategoryId) throws ItemNotFoundException;

    GlobeSuccessResponseBuilder activateShopCategory(UUID shopCategoryId) throws ItemNotFoundException;
}