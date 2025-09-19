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
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.*;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.service.ProductService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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

        //Slug
        String uniqueSlug = generateUniqueSlugForShop(request.getProductName(), shopId);
        product.setProductSlug(uniqueSlug);

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

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateProduct(UUID productId, UpdateProductRequest request)
            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find existing product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Verify user owns the shop that owns this product
        if (!product.getShop().getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only update products from shops you own");
        }

        // Check if shop is still approved and active
        if (product.getShop().getIsDeleted()) {
            throw new RandomExceptions("Cannot update products from deleted shops");
        }

        if (!product.getShop().isApproved()) {
            throw new RandomExceptions("Cannot update products from unapproved shops");
        }

        // Check product name uniqueness if name is being changed
        if (request.getProductName() != null &&
                !product.getProductName().equals(request.getProductName())) {

            if (productRepo.existsByProductNameAndBrandAndPriceAndShopAndIsDeletedFalse(
                    request.getProductName(),
                    request.getBrand() != null ? request.getBrand() : product.getBrand(),
                    request.getPrice() != null ? request.getPrice() : product.getPrice(),
                    product.getShop())) {
                throw new ItemReadyExistException("A product with the same name, brand, and price already exists in this shop");
            }

            // Update name and regenerate slug only if name changed
            product.setProductName(request.getProductName());
            String newSlug = generateUniqueSlugForShop(request.getProductName(), product.getShop().getShopId());
            product.setProductSlug(newSlug);
        }

        // Check SKU uniqueness if SKU is being changed
        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            if (!product.getSku().equals(request.getSku()) &&
                    productRepo.existsBySkuAndIsDeletedFalse(request.getSku())) {
                throw new ItemReadyExistException("A product with this SKU already exists");
            }
            product.setSku(request.getSku());
        }

        // Validate category if being changed
        if (request.getCategoryId() != null) {
            ProductCategoryEntity category = productCategoryRepo.findById(request.getCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Product category not found"));

            if (!category.getIsActive()) {
                throw new RandomExceptions("Cannot assign products to inactive categories");
            }

            product.setCategory(category);
        }

        // Update fields using utility method (much cleaner!)
        updateIfNotNull(product::setProductDescription, request.getProductDescription());
        updateIfNotNull(product::setShortDescription, request.getShortDescription());
        updateIfNotNull(product::setProductImages, request.getProductImages());

        // Pricing updates
        updateIfNotNull(product::setPrice, request.getPrice());
        updateIfNotNull(product::setComparePrice, request.getComparePrice());

        // Inventory updates
        updateIfNotNull(product::setStockQuantity, request.getStockQuantity());
        updateIfNotNull(product::setLowStockThreshold, request.getLowStockThreshold());
        updateIfNotNull(product::setTrackInventory, request.getTrackInventory());

        // Product details
        updateIfNotNull(product::setBrand, request.getBrand());

        // Physical properties
        updateIfNotNull(product::setWeight, request.getWeight());
        updateIfNotNull(product::setLength, request.getLength());
        updateIfNotNull(product::setWidth, request.getWidth());
        updateIfNotNull(product::setHeight, request.getHeight());

        // Status and condition
        updateIfNotNull(product::setCondition, request.getCondition());
        updateIfNotNull(product::setStatus, request.getStatus());

        // SEO and tags
        updateIfNotNull(product::setTags, request.getTags());
        updateIfNotNull(product::setMetaTitle, request.getMetaTitle());
        updateIfNotNull(product::setMetaDescription, request.getMetaDescription());

        // Features
        updateIfNotNull(product::setIsFeatured, request.getIsFeatured());
        updateIfNotNull(product::setIsDigital, request.getIsDigital());
        updateIfNotNull(product::setRequiresShipping, request.getRequiresShipping());

        // Update system fields
        product.setEditedBy(user.getId());

        // Save updated product
        ProductEntity updatedProduct = productRepo.save(product);
        ProductResponse response = buildProductResponse(updatedProduct);

        log.info("Product updated successfully: {} in shop: {} by user: {}",
                updatedProduct.getProductName(),
                updatedProduct.getShop().getShopName(),
                user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product updated successfully",
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductDetailed(UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Check if user has permission to view detailed information
        // Detailed view is for: shop owners, super admins, staff admins
        boolean isOwner = product.getShop().getOwner().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("ROLE_SUPER_ADMIN") ||
                        role.getRoleName().equals("ROLE_STAFF_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new RandomExceptions("Only shop owners and administrators can access detailed product information");
        }

        // Additional checks for shop owners
        if (isOwner) {
            // Verify shop is not deleted
            if (product.getShop().getIsDeleted()) {
                throw new RandomExceptions("Cannot access products from deleted shops");
            }
        }

        // Build detailed response
        ProductResponse response = buildProductResponse(product);

        log.info("Detailed product information accessed: {} from shop: {} by user: {}",
                product.getProductName(),
                product.getShop().getShopName(),
                user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product details retrieved successfully",
                response
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder deleteProduct(UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Verify user owns the shop that owns this product
        if (!product.getShop().getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only delete products from shops you own");
        }

        // Check if shop is still active (additional business logic)
        if (product.getShop().getIsDeleted()) {
            throw new RandomExceptions("Cannot delete products from deleted shops");
        }

        // Check if product is already deleted (shouldn't happen due to query, but safety check)
        if (product.getIsDeleted()) {
            throw new ItemNotFoundException("Product is already deleted");
        }

        // Soft delete - mark as deleted
        product.setIsDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        product.setEditedBy(user.getId());

        // Save the updated product
        productRepo.save(product);

        log.info("Product deleted successfully: {} from shop: {} by user: {}",
                product.getProductName(),
                product.getShop().getShopName(),
                user.getUserName());

        return GlobeSuccessResponseBuilder.success(
                "Product deleted successfully"
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateProductStatus(UUID productId, ProductStatus status)
            throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Verify user owns the shop that owns this product
        if (!product.getShop().getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only update status of products from shops you own");
        }

        // Check if shop is still active
        if (product.getShop().getIsDeleted()) {
            throw new RandomExceptions("Cannot update status of products from deleted shops");
        }

        if (!product.getShop().isApproved()) {
            throw new RandomExceptions("Cannot update status of products from unapproved shops");
        }

        // Check if status is actually changing
        if (product.getStatus().equals(status)) {
            return GlobeSuccessResponseBuilder.success(
                    "Product status is already " + status.name().toLowerCase()
            );
        }

        // Business logic validation for status transitions
        validateStatusTransition(product.getStatus(), status, product);

        // Update status
        ProductStatus oldStatus = product.getStatus();
        product.setStatus(status);
        product.setEditedBy(user.getId());

        // Save updated product
        ProductEntity updatedProduct = productRepo.save(product);

        return GlobeSuccessResponseBuilder.success(
                "Product status updated successfully to " + status.name().toLowerCase()
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateProductStock(UUID productId, Integer newStock)
            throws ItemNotFoundException, RandomExceptions {

        // Validate stock quantity
        if (newStock < 0) {
            throw new RandomExceptions("Stock quantity cannot be negative");
        }

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Verify user owns the shop that owns this product
        if (!product.getShop().getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only update stock of products from shops you own");
        }

        // Check if shop is still active
        if (product.getShop().getIsDeleted()) {
            throw new RandomExceptions("Cannot update stock of products from deleted shops");
        }

        if (!product.getShop().isApproved()) {
            throw new RandomExceptions("Cannot update stock of products from unapproved shops");
        }

        // Check if product tracks inventory
        if (!product.getTrackInventory()) {
            throw new RandomExceptions("Cannot update stock for products that don't track inventory");
        }

        // Check if stock is actually changing
        if (product.getStockQuantity().equals(newStock)) {
            return GlobeSuccessResponseBuilder.success(
                    "Product stock is already " + newStock
            );
        }

        Integer oldStock = product.getStockQuantity();

        // Update stock quantity
        product.setStockQuantity(newStock);
        product.setEditedBy(user.getId());

        // Auto-update product status based on stock level
        ProductStatus newStatus = determineStatusByStock(newStock, product);
        ProductStatus oldStatus = product.getStatus();

        if (!oldStatus.equals(newStatus)) {
            product.setStatus(newStatus);
        }

        // Save updated product
        ProductEntity updatedProduct = productRepo.save(product);

        // Build response message
        String message = String.format("Product stock updated from %d to %d", oldStock, newStock);
        if (!oldStatus.equals(newStatus)) {
            message += String.format(". Status automatically changed from %s to %s",
                    oldStatus.name(), newStatus.name());
        }

        // Add stock level warning if applicable
        if (newStock <= product.getLowStockThreshold() && newStock > 0) {
            message += ". Warning: Product is now at low stock level";
        }

        log.info("Product stock updated: {} changed from {} to {} in shop: {} by user: {}",
                product.getProductName(),
                oldStock,
                newStock,
                product.getShop().getShopName(),
                user.getUserName());

        return GlobeSuccessResponseBuilder.success(message);
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductsByShop(UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find and validate shop
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // Check if user owns this shop
        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only view products from shops you own");
        }

        // Get all products for this shop
        List<ProductEntity> products = productRepo.findByShopAndIsDeletedFalseOrderByCreatedAtDesc(shop);

        // Build response list
        List<ProductResponse> responses = products.stream()
                .map(this::buildProductResponse)
                .toList();

        // Build summary message
        String message = String.format("Retrieved %d products from shop: %s",
                products.size(), shop.getShopName());

        log.info("Products retrieved for shop: {} (count: {}) by user: {}",
                shop.getShopName(), products.size(), user.getUserName());

        return GlobeSuccessResponseBuilder.success(message, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductEntity> getProductsByShopPaged(UUID shopId, int page, int size)
            throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity user = getAuthenticatedAccount();

        // Find and validate shop
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // Check if user owns this shop
        if (!shop.getOwner().getId().equals(user.getId())) {
            throw new RandomExceptions("You can only view products from shops you own");
        }

        // Validate and adjust page parameters
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        // Create pageable request
        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        // Get paginated products for this shop
        Page<ProductEntity> productPage = productRepo.findByShopAndIsDeletedFalseOrderByCreatedAtDesc(shop, pageable);

        log.info("Paginated products retrieved for shop: {} (page: {}, size: {}, total: {}) by user: {}",
                shop.getShopName(), page, size, productPage.getTotalElements(), user.getUserName());

        return productPage;
    }


    private ProductStatus determineStatusByStock(Integer stockQuantity, ProductEntity product) {
        // Only auto-change status if currently ACTIVE or OUT_OF_STOCK
        if (product.getStatus() != ProductStatus.ACTIVE && product.getStatus() != ProductStatus.OUT_OF_STOCK) {
            return product.getStatus(); // Keep current status for DRAFT, ARCHIVED, etc.
        }

        if (stockQuantity <= 0) {
            return ProductStatus.OUT_OF_STOCK;
        } else {
            return ProductStatus.ACTIVE;
        }
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

    private String generateUniqueSlugForShop(String productName, UUID shopId) {
        String baseSlug = createBaseSlug(productName);
        String slug = baseSlug;
        int counter = 2;

        // Check uniqueness only within the same shop
        while (productRepo.existsByProductSlugAndShop_ShopIdAndIsDeletedFalse(slug, shopId)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    private String createBaseSlug(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "product";
        }

        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }


    //Handle updates if value is not null
    private <T> void updateIfNotNull(Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private void validateStatusTransition(ProductStatus currentStatus, ProductStatus newStatus, ProductEntity product)
            throws RandomExceptions {

        // Prevent setting to OUT_OF_STOCK if product has stock
        if (newStatus == ProductStatus.OUT_OF_STOCK && product.getStockQuantity() > 0) {
            throw new RandomExceptions("Cannot set status to OUT_OF_STOCK when product has stock quantity: " + product.getStockQuantity());
        }

        // Prevent setting to ACTIVE if product has no stock and tracks inventory
        if (newStatus == ProductStatus.ACTIVE && product.getTrackInventory() && product.getStockQuantity() <= 0) {
            throw new RandomExceptions("Cannot set status to ACTIVE when product is out of stock");
        }

        // Business rule: Can't go directly from ARCHIVED to ACTIVE
        if (currentStatus == ProductStatus.ARCHIVED && newStatus == ProductStatus.ACTIVE) {
            throw new RandomExceptions("Cannot directly activate archived products. Please set to DRAFT or INACTIVE first");
        }
    }
}