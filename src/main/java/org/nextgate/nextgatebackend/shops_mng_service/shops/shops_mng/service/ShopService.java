package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.payload.CreateShopRequest;

public interface ShopService {
    GlobeSuccessResponseBuilder createShop(CreateShopRequest request)
            throws ItemReadyExistException, ItemNotFoundException;
}
