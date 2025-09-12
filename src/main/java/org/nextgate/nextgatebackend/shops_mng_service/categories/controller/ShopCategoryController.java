package org.nextgate.nextgatebackend.shops_mng_service.categories.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.shops_mng_service.categories.paylaod.CreateShopCategoryRequest;
import org.nextgate.nextgatebackend.shops_mng_service.categories.service.ShopCategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/shop-categories")
@RequiredArgsConstructor
public class ShopCategoryController {

    private final ShopCategoryService shopCategoryService;

    @PostMapping
    public GlobeSuccessResponseBuilder create(@RequestBody CreateShopCategoryRequest request)
            throws ItemReadyExistException, ItemNotFoundException {
        return shopCategoryService.createShopCategory(request);
    }

    @PutMapping("/{categoryName}")
    public GlobeSuccessResponseBuilder update(@PathVariable String categoryName,
                                              @RequestBody CreateShopCategoryRequest request)
            throws ItemNotFoundException, ItemReadyExistException {
        return shopCategoryService.updateShopCategory(categoryName, request);
    }

    @DeleteMapping("/{categoryName}")
    public GlobeSuccessResponseBuilder delete(@PathVariable String categoryName)
            throws ItemNotFoundException {
        return shopCategoryService.deleteShopCategory(categoryName);
    }

    @GetMapping("/{categoryId}")
    public GlobeSuccessResponseBuilder getById(@PathVariable UUID categoryId)
            throws ItemNotFoundException {
        return shopCategoryService.getShopCategory(categoryId);
    }

    @GetMapping("/by-name/{categoryName}")
    public GlobeSuccessResponseBuilder getByName(@PathVariable String categoryName)
            throws ItemNotFoundException {
        return shopCategoryService.getShopCategoryByName(categoryName);
    }

    @GetMapping
    public GlobeSuccessResponseBuilder getAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return activeOnly ?
                shopCategoryService.getAllActiveShopCategories() :
                shopCategoryService.getAllShopCategories();
    }

    @GetMapping("/paged")
    public GlobeSuccessResponseBuilder getAllPaged(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue = "false") boolean activeOnly,
                                                   @RequestParam(defaultValue = "false") boolean sortByName) {
        if (activeOnly) {
            return shopCategoryService.getAllActiveShopCategoriesPaged(page, size);
        }
        return sortByName ?
                shopCategoryService.getAllShopCategoriesOrderedByName(page, size) :
                shopCategoryService.getAllShopCategoriesPaged(page, size);
    }

    @GetMapping("/search")
    public GlobeSuccessResponseBuilder search(@RequestParam String term,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return shopCategoryService.searchShopCategoriesByName(term, page, size);
    }

}