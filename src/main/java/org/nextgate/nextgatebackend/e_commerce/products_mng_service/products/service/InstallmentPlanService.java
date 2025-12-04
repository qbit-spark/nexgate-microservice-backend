package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.CreateInstallmentPlanRequest;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.payload.UpdateInstallmentPlanRequest;


import java.util.UUID;

public interface InstallmentPlanService {

    GlobeSuccessResponseBuilder createInstallmentPlan(UUID shopId, UUID productId,
                                                      CreateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getProductInstallmentPlans(UUID shopId, UUID productId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder getInstallmentPlanById(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder updateInstallmentPlan(UUID shopId, UUID productId, UUID planId,
                                                      UpdateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder deleteInstallmentPlan(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder togglePlanStatus(UUID shopId, UUID productId, UUID planId, Boolean isActive)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder setFeaturedPlan(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder toggleProductInstallments(UUID shopId, UUID productId, Boolean enabled)
            throws ItemNotFoundException, RandomExceptions;
}