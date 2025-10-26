package org.nextgate.nextgatebackend.products_mng_service.products.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateInstallmentPlanRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateInstallmentPlanRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.service.InstallmentPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/products/{shopId}/{productId}/installment-plans")
@RequiredArgsConstructor
public class InstallmentPlanController {

    private final InstallmentPlanService installmentPlanService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createInstallmentPlan(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = installmentPlanService.createInstallmentPlan(
                shopId, productId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getProductInstallmentPlans(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = installmentPlanService.getProductInstallmentPlans(
                shopId, productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getInstallmentPlanById(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId)
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = installmentPlanService.getInstallmentPlanById(
                shopId, productId, planId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateInstallmentPlan(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId,
            @Valid @RequestBody UpdateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = installmentPlanService.updateInstallmentPlan(
                shopId, productId, planId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteInstallmentPlan(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId)
                                throws ItemNotFoundException, RandomExceptions {

                            GlobeSuccessResponseBuilder response = installmentPlanService.deleteInstallmentPlan(
                                    shopId, productId, planId);

                            return ResponseEntity.ok(response);
                        }

    @PatchMapping("/{planId}/activate")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateInstallmentPlan(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = installmentPlanService.togglePlanStatus(
                shopId, productId, planId, true);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{planId}/deactivate")
    public ResponseEntity<GlobeSuccessResponseBuilder> deactivateInstallmentPlan(
                                            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = installmentPlanService.togglePlanStatus(
                shopId, productId, planId, false);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{planId}/set-featured")
    public ResponseEntity<GlobeSuccessResponseBuilder> setFeaturedPlan(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID planId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = installmentPlanService.setFeaturedPlan(
                shopId, productId, planId);

        return ResponseEntity.ok(response);
    }

}