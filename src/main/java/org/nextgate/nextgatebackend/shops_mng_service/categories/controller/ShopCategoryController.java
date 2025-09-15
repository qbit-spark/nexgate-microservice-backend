package org.nextgate.nextgatebackend.shops_mng_service.categories.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod.CreateShopCategoryRequest;
import org.nextgate.nextgatebackend.shops_mng_service.categories.service.ShopCategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops/categories")
@RequiredArgsConstructor
public class ShopCategoryController {

    private final ShopCategoryService shopCategoryService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public GlobeSuccessResponseBuilder create(@RequestBody CreateShopCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {
        return shopCategoryService.createShopCategory(request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder update(@PathVariable UUID categoryId,
                                              @RequestBody CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {
        return shopCategoryService.updateShopCategory(categoryId, request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder getCategoryById(@PathVariable UUID categoryId) throws ItemNotFoundException {
        return shopCategoryService.getShopCategory(categoryId);
    }

    @GetMapping("/all")
    public GlobeSuccessResponseBuilder getAllCategories(
            @RequestParam(required = false) Boolean isActive) {

        return shopCategoryService.getAllShopCategories(isActive);
    }

    @GetMapping("/all-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllCategoriesPaged(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ShopCategoryEntity> response = shopCategoryService.getAllShopCategoriesPaged(isActive, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Shop categories retrieved successfully", response));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder deleteCategoryById(@PathVariable UUID categoryId) throws ItemNotFoundException {
        return shopCategoryService.deleteShopCategory(categoryId);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{categoryId}/activate")
    public GlobeSuccessResponseBuilder activateCategoryById(@PathVariable UUID categoryId) throws ItemNotFoundException {
        return shopCategoryService.activateShopCategory(categoryId);
    }

}