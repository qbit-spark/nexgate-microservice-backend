package org.nextgate.nextgatebackend.products_mng_service.products.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ReqAction;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductFilterCriteria;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateProductRequest;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    GlobeSuccessResponseBuilder createProduct(UUID shopId, CreateProductRequest request, ReqAction action) throws ItemNotFoundException, RandomExceptions, ItemReadyExistException;

    GlobeSuccessResponseBuilder getProductDetailed(UUID shopId, UUID productId) throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getProductsByMyShop(UUID shopId) throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getAllProductsPaged(UUID shopId, int page, int size) throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder updateProduct(UUID shopId, UUID productId, UpdateProductRequest request, ReqAction action)
            throws ItemNotFoundException, RandomExceptions, ItemReadyExistException;


    GlobeSuccessResponseBuilder publishProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder deleteProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder restoreProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getPublicProductsByShop(UUID shopId) throws ItemNotFoundException;

    GlobeSuccessResponseBuilder getPublicProductsByShopPaged(UUID shopId, int page, int size) throws ItemNotFoundException;

    GlobeSuccessResponseBuilder getProductById(UUID shopId, UUID productId) throws ItemNotFoundException;

    GlobeSuccessResponseBuilder searchProducts(UUID shopId, String query, List<ProductStatus> status,
                                               int page, int size, String sortBy, String sortDir) throws ItemNotFoundException;

    GlobeSuccessResponseBuilder filterProducts(UUID shopId, ProductFilterCriteria criteria,
                                               int page, int size, String sortBy, String sortDir)
            throws ItemNotFoundException;


}

