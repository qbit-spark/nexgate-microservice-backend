package org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.payload.CreateProductCategoryRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ProductCategoryService {

    GlobeSuccessResponseBuilder createProductCategory(CreateProductCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException;

    GlobeSuccessResponseBuilder updateProductCategory(UUID categoryId, CreateProductCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException;

    GlobeSuccessResponseBuilder getProductCategory(UUID categoryId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder getAllProductCategories(Boolean isActive);

    Page<ProductCategoryEntity> getAllProductCategoriesPaged(Boolean isActive, int page, int size);

    GlobeSuccessResponseBuilder getParentCategories();

    GlobeSuccessResponseBuilder getChildCategories(UUID parentId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder deleteProductCategory(UUID categoryId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder activateProductCategory(UUID categoryId)
            throws ItemNotFoundException;
}