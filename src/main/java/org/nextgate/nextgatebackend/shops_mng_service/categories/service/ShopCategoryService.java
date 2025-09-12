package org.nextgate.nextgatebackend.shops_mng_service.categories.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod.CreateShopCategoryRequest;

import java.util.UUID;

public interface ShopCategoryService {

    // CRUD OPERATIONS
    GlobeSuccessResponseBuilder createShopCategory(CreateShopCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException;

    GlobeSuccessResponseBuilder deleteShopCategory(String categoryName)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder updateShopCategory(String categoryName, CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException;

    // SINGLE ITEM RETRIEVAL
    GlobeSuccessResponseBuilder getShopCategory(UUID shopCategoryId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder getShopCategoryByName(String categoryName)
            throws ItemNotFoundException;

    // NON-PAGEABLE LIST METHODS
    GlobeSuccessResponseBuilder getAllShopCategories();

    GlobeSuccessResponseBuilder getAllActiveShopCategories();

    // PAGEABLE LIST METHODS
    GlobeSuccessResponseBuilder getAllShopCategoriesPaged(int page, int size);

    GlobeSuccessResponseBuilder getAllActiveShopCategoriesPaged(int page, int size);

    GlobeSuccessResponseBuilder getAllShopCategoriesOrderedByName(int page, int size);

    GlobeSuccessResponseBuilder searchShopCategoriesByName(String searchTerm, int page, int size);
}