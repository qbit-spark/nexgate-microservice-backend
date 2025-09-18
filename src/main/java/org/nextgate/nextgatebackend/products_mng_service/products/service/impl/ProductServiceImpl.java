package org.nextgate.nextgatebackend.products_mng_service.products.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.products_mng_service.categories.repo.ProductCategoryRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.ProductResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.service.ProductService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepo productRepo;
    private final ShopRepo shopRepo;
    private final ProductCategoryRepo productCategoryRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder createProduct(UUID shopId, CreateProductRequest request)
            throws ItemReadyExistException, ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Validate shop exists and user owns it
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new RandomExceptions("Cannot create products for a deleted shop");
        }

        // Check if user owns this shop
        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only create products for shops you own");
        }

        // Validate shop is approved and active
        if (!shop.isApproved()) {
            throw new RandomExceptions("Cannot create products for unapproved shops");
        }

        // Check if product name already exists in this shop
        if (productRepo.existsByProductNameAndBrandAndPriceAndShopAndIsDeletedFalse(
                request.getProductName(),
                request.getBrand(),
                request.getPrice(),
                shop)) {
            throw new ItemReadyExistException("A product with the same name, brand, and price already exists in this shop");
        }

        // Check SKU uniqueness if provided
        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            if (productRepo.existsBySkuAndIsDeletedFalse(request.getSku())) {
                throw new ItemReadyExistException("A product with this SKU already exists");
            }
        }

        // Validate category
        ProductCategoryEntity category = productCategoryRepo.findById(request.getCategoryId())
                .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

        if (!category.getIsActive()) {
            throw new RandomExceptions("Cannot create products with inactive categories");
        }

        // Create new product
        ProductEntity product = new ProductEntity();

        // Basic Information
        product.setProductName(request.getProductName());
        product.setProductDescription(request.getProductDescription());
        product.setShortDescription(request.getShortDescription());
        product.setProductImages(request.getProductImages());

        // Pricing
        product.setPrice(request.getPrice());
        product.setComparePrice(request.getComparePrice());

        // Inventory
        product.setStockQuantity(request.getStockQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setTrackInventory(request.getTrackInventory());

        // Product Details
        product.setBrand(request.getBrand());
        product.setSku(request.getSku());

        // Physical Properties
        product.setWeight(request.getWeight());
        product.setLength(request.getLength());
        product.setWidth(request.getWidth());
        product.setHeight(request.getHeight());

        // Status and Condition
        product.setCondition(request.getCondition());
        product.setStatus(request.getStatus());

        // SEO and Tags
        product.setTags(request.getTags());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());

        // Relationships
        product.setShop(shop);
        product.setCategory(category);

        // Features
        product.setIsFeatured(request.getIsFeatured());
        product.setIsDigital(request.getIsDigital());
        product.setRequiresShipping(request.getRequiresShipping());

        // System fields
        product.setCreatedBy(user.getId());
        product.setEditedBy(user.getId());
        product.setIsDeleted(false);

        // Save product
        ProductEntity savedProduct = productRepo.save(product);
        ProductResponse response = buildProductResponse(savedProduct);

        log.info("Product created successfully: {} for shop: {} by user: {}",
                savedProduct.getProductName(), shop.getShopName(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product created successfully",
                response
        );
    }

    // HELPER METHODS

    private ProductResponse buildProductResponse(ProductEntity product) {
        return ProductResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productDescription(product.getProductDescription())
                .shortDescription(product.getShortDescription())
                .productImages(product.getProductImages())

                // Pricing
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage())
                .isOnSale(product.isOnSale())

                // Inventory
                .stockQuantity(product.getStockQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .isInStock(product.isInStock())
                .isLowStock(product.isLowStock())
                .trackInventory(product.getTrackInventory())

                // Product Details
                .brand(product.getBrand())
                .sku(product.getSku())
                .weight(product.getWeight())
                .length(product.getLength())
                .width(product.getWidth())
                .height(product.getHeight())
                .condition(product.getCondition())
                .status(product.getStatus())

                // SEO and Tags
                .tags(product.getTags())
                .metaTitle(product.getMetaTitle())
                .metaDescription(product.getMetaDescription())

                // Shop Info
                .shopId(product.getShop().getShopId())
                .shopName(product.getShop().getShopName())
                .shopSlug(product.getShop().getShopSlug())

                // Category Info
                .categoryId(product.getCategory().getCategoryId())
                .categoryName(product.getCategory().getCategoryName())

                // Features
                .isFeatured(product.getIsFeatured())
                .isDigital(product.getIsDigital())
                .requiresShipping(product.getRequiresShipping())

                // System Fields
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .editedBy(product.getEditedBy())
                .build();
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

}