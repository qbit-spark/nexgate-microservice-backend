package org.nextgate.nextgatebackend.products_mng_service.products.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ReqAction;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;

import java.util.UUID;

public interface ProductService {
    GlobeSuccessResponseBuilder createProduct(UUID shopId, CreateProductRequest request, ReqAction action) throws ItemNotFoundException, RandomExceptions, ItemReadyExistException;

    GlobeSuccessResponseBuilder getProductDetailed(UUID shopId, UUID productId) throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getProductsByMyShop(UUID shopId) throws ItemNotFoundException, RandomExceptions;
}

