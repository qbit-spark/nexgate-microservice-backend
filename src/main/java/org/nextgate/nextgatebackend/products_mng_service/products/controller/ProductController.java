package org.nextgate.nextgatebackend.products_mng_service.products.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ReqAction;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/shops/{shopId}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createProduct(
            @PathVariable UUID shopId,
            @Valid @RequestBody CreateProductRequest request,
            @RequestParam ReqAction action)
            throws ItemReadyExistException, ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.createProduct(shopId, request, action);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{productId}/detailed")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProductDetailed(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException, RandomExceptions {
        GlobeSuccessResponseBuilder response = productService.getProductDetailed(shopId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public  ResponseEntity<GlobeSuccessResponseBuilder> getAllProducts(@PathVariable UUID shopId) throws RandomExceptions, ItemNotFoundException {
        GlobeSuccessResponseBuilder response = productService.getProductsByMyShop(shopId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllProductsPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.getAllProductsPaged(shopId, page, size);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProduct(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request,
            @RequestParam ReqAction action)
            throws ItemNotFoundException, RandomExceptions, ItemReadyExistException {

        GlobeSuccessResponseBuilder response = productService.updateProduct(shopId, productId, request, action);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/publish")
    public ResponseEntity<GlobeSuccessResponseBuilder> publishProduct(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.publishProduct(shopId, productId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProduct(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.deleteProduct(shopId, productId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/restore")
    public ResponseEntity<GlobeSuccessResponseBuilder> restoreProduct(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.restoreProduct(shopId, productId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPublicProductById(
            @PathVariable UUID shopId,
            @PathVariable UUID productId)
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = productService.getProductById(shopId, productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public-view/all")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllPublicProducts(
            @PathVariable UUID shopId) throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = productService.getPublicProductsByShop(shopId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public-view/all-paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getAllPublicProductsPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = productService.getPublicProductsByShopPaged(shopId, page, size);
        return ResponseEntity.ok(response);
    }

}