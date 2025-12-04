package org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.payload.CreateProductCategoryRequest;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.service.ProductCategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/products/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    //@PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public GlobeSuccessResponseBuilder createCategory(@Valid @RequestBody CreateProductCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {
        return productCategoryService.createProductCategory(request);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder updateCategory(@PathVariable UUID categoryId,
                                                      @Valid @RequestBody CreateProductCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {
        return productCategoryService.updateProductCategory(categoryId, request);
    }

    @GetMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder getCategoryById(@PathVariable UUID categoryId)
            throws ItemNotFoundException {
        return productCategoryService.getProductCategory(categoryId);
    }

    @GetMapping("/all")
    public GlobeSuccessResponseBuilder getAllCategories(
            @RequestParam(required = false) Boolean isActive) {
        return productCategoryService.getAllProductCategories(isActive);
    }

    @GetMapping("/all-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllCategoriesPaged(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductCategoryEntity> response = productCategoryService.getAllProductCategoriesPaged(isActive, page, size);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success("Product categories retrieved successfully", response));
    }

    @GetMapping("/parent-categories")
    public GlobeSuccessResponseBuilder getParentCategories() {
        return productCategoryService.getParentCategories();
    }

    @GetMapping("/parent/{parentId}/children")
    public GlobeSuccessResponseBuilder getChildCategories(@PathVariable UUID parentId)
            throws ItemNotFoundException {
        return productCategoryService.getChildCategories(parentId);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder deleteCategory(@PathVariable UUID categoryId)
            throws ItemNotFoundException {
        return productCategoryService.deleteProductCategory(categoryId);
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{categoryId}/activate")
    public GlobeSuccessResponseBuilder activateCategory(@PathVariable UUID categoryId)
            throws ItemNotFoundException {
        return productCategoryService.activateProductCategory(categoryId);
    }
}