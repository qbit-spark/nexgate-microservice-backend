package org.nextgate.nextgatebackend.products_mng_service.products.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
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
            @Valid @RequestBody CreateProductRequest request)
            throws ItemReadyExistException, ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.createProduct(shopId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProduct(
            @PathVariable UUID productId,
            @PathVariable UUID shopId,
            @Valid @RequestBody UpdateProductRequest request)
            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.updateProduct(productId, request);

        return ResponseEntity.ok(response);
    }

}