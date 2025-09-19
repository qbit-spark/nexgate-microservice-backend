package org.nextgate.nextgatebackend.products_mng_service.products.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/{productId}/detailed")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProductDetailed(
            @PathVariable UUID productId, @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.getProductDetailed(productId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteProduct(
            @PathVariable UUID productId, @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.deleteProduct(productId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/status")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProductStatus(
            @PathVariable UUID productId,
            @PathVariable UUID shopId,
            @RequestParam ProductStatus status)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.updateProductStatus(productId, status);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{productId}/stock")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateProductStock(
            @PathVariable UUID productId,
            @RequestParam Integer stock, @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.updateProductStock(productId, stock);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getProductsByShop(
            @PathVariable UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = productService.getProductsByShop(shopId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getProductsByShopPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size)
            throws ItemNotFoundException, RandomExceptions {

        Page<ProductEntity> productPage = productService.getProductsByShopPaged(shopId, page, size);

        // Build paginated response
        List<ProductResponse> productResponses = productPage.getContent().stream()
                .map(this::buildProductResponse)
                .toList();

        var responseData = new Object() {
            public final List<ProductResponse> products = productResponses;
            public final int currentPage = productPage.getNumber() + 1; // Convert back to 1-based
            public final int pageSize = productPage.getSize();
            public final long totalElements = productPage.getTotalElements();
            public final int totalPages = productPage.getTotalPages();
            public final boolean hasNext = productPage.hasNext();
            public final boolean hasPrevious = productPage.hasPrevious();
            public final boolean isFirst = productPage.isFirst();
            public final boolean isLast = productPage.isLast();
        };

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.success("Products retrieved successfully", responseData)
        );
    }


    @GetMapping("/my-shops-summary")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyShopsProductsSummary()
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = productService.getMyShopsProductsSummary();

        return ResponseEntity.ok(response);
    }


    private ProductResponse buildProductResponse(ProductEntity product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productDescription(product.getProductDescription())
                .shortDescription(product.getShortDescription())
                .productImages(product.getProductImages())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage())
                .isOnSale(product.isOnSale())
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .isInStock(product.isInStock())
                .isLowStock(product.isLowStock())
                .trackInventory(product.getTrackInventory())
                .brand(product.getBrand())
                .sku(product.getSku())
                .weight(product.getWeight())
                .length(product.getLength())
                .width(product.getWidth())
                .height(product.getHeight())
                .condition(product.getCondition())
                .status(product.getStatus())
                .tags(product.getTags())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())
                .shopId(product.getShop().getShopId())
                .shopName(product.getShop().getShopName())
                .shopSlug(product.getShop().getShopSlug())
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())
                .isFeatured(product.getIsFeatured())
                .isDigital(product.getIsDigital())
                .requiresShipping(product.getRequiresShipping())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .editedBy(product.getEditedBy())
                .build();
    }

}