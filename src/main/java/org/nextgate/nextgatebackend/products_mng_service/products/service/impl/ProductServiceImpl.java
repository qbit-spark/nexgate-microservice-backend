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
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ReqAction;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.*;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.service.ProductService;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers.ProductBuildResponseHelper;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers.ProductFilterHelper;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers.ProductHelperMethods;
import org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers.ProductSearchHelper;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums.ShopStatus;
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
import java.util.ArrayList;
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
    private final ProductHelperMethods productHelperMethods;
    private final ProductBuildResponseHelper productBuildResponseHelper;
    private final ProductSearchHelper productSearchHelper;
    private final ProductFilterHelper productFilterHelper;

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder createProduct(UUID shopId, CreateProductRequest request, ReqAction action) throws ItemNotFoundException, RandomExceptions, ItemReadyExistException {

        //1. Get the authenticated user
        AccountEntity account = getAuthenticatedAccount();
        //2. Shop existence check
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        //3. Check if the authenticated user is the owner or admin
        boolean isValidToCreateProduct = validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop);
        if (!isValidToCreateProduct) {
            throw new RandomExceptions("You do not have permission to create a product for this shop");
        }

        //4. Check if the product with the same name, brand, price, specifications,  already exists in the shop
        boolean productExists = productRepo.existsByProductNameAndBrandAndPriceAndSpecificationsAndShopAndIsDeletedFalse(
                request.getProductName(), request.getBrand(), request.getPrice(), request.getSpecifications(), shop);

        if (productExists) {
            throw new ItemReadyExistException("A product with the same name, brand, price, and specifications already exists in this shop");
        }

        //5. Validate category existence
        ProductCategoryEntity category = productCategoryRepo.findByCategoryIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ItemNotFoundException("Category not found or inactive"));

        //6. Generate a unique slug for the product within the shop
        String productSlug = generateUniqueSlugForShop(request.getProductName(), shopId);

        //7. Create the product entity
        ProductEntity product = new ProductEntity();

        // Basic Information
        product.setProductName(request.getProductName());
        product.setProductSlug(productSlug);
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
        product.setSku(productHelperMethods.generateUniqueSKU(shop, category, request));
        product.setSpecifications(request.getSpecifications());
        product.setColors(productHelperMethods.convertColorsToEntity(request.getColors()));

        // Status and Condition
        product.setCondition(request.getCondition());
        product.setStatus(request.getStatus());

        // SEO and Tags
        product.setTags(request.getTags());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());

        // Ordering Limits
        product.setMinOrderQuantity(request.getMinOrderQuantity());
        product.setMaxOrderQuantity(request.getMaxOrderQuantity());
        product.setRequiresApproval(request.getRequiresApproval());

        // Relationships
        product.setShop(shop);
        product.setCategory(category);

        // Features
        product.setIsFeatured(request.getIsFeatured());
        product.setIsDigital(request.getIsDigital());
        product.setRequiresShipping(request.getRequiresShipping());

        // System fields
        product.setCreatedBy(account.getId());
        product.setEditedBy(account.getId());
        product.setIsDeleted(false);

        // Group Buying
        product.setGroupBuyingEnabled(request.getGroupBuyingEnabled());
        if (request.getGroupBuyingEnabled()) {
            product.setGroupMaxSize(request.getGroupMaxSize());
            product.setGroupPrice(request.getGroupPrice());
            product.setGroupTimeLimitHours(request.getGroupTimeLimitHours());
        }

        // Installment Options
        product.setInstallmentEnabled(request.getInstallmentEnabled());
        if (request.getInstallmentEnabled()) {
            product.setInstallmentPlans(productHelperMethods.convertInstallmentPlansToEntity(request.getInstallmentPlans()));
            product.setDownPaymentRequired(request.getDownPaymentRequired());
            product.setMinDownPaymentPercentage(request.getMinDownPaymentPercentage());
        }


        //8. Set initial status based on action
        if (action == ReqAction.SAVE_PUBLISH) {
            product.setStatus(ProductStatus.ACTIVE);
        } else {
            product.setStatus(ProductStatus.DRAFT);
        }

        product.setCreatedBy(account.getAccountId());
        product.setCreatedAt(LocalDateTime.now());
        product.setIsDeleted(false);

        //9. Save product
        productRepo.save(product);

        return GlobeSuccessResponseBuilder.success(
                "Product created successfully",
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductDetailed(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // 1. Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        //2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        // 2. Find product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Ensure the product belongs to the specified shop
        if (!product.getShop().getShopId().equals(shopId)) {
            throw new ItemNotFoundException("Product does not belong to the specified shop");
        }

        //3. Check if the authenticated user is the owner or admin
        boolean isValidToCreateProduct = validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), authenticatedAccount, shop);
        if (!isValidToCreateProduct) {
            throw new RandomExceptions("You do not have permission to create a product for this shop");
        }

        // 4. Build comprehensive response with all features
        ProductDetailedResponse response = productBuildResponseHelper.buildDetailedProductResponse(product);


        return GlobeSuccessResponseBuilder.success(
                "Product details retrieved successfully",
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductsByMyShop(UUID shopId)
            throws ItemNotFoundException, RandomExceptions {

        //1. Get the authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        //2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        //3. Check if the authenticated user is the owner or admin
        boolean isValidToViewProducts = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), authenticatedAccount, shop);
        if (!isValidToViewProducts) {
            throw new RandomExceptions("You do not have permission to view products for this shop");
        }

        //4. Get products with minimal data for performance
        List<ProductEntity> products = productRepo.findByShopAndIsDeletedFalseOrderByCreatedAtDesc(shop);

        //5. Build lightweight response list
        List<ProductSummaryResponse> responses = products.stream()
                .map(productBuildResponseHelper::buildProductSummaryResponse)
                .toList();

        //6. Build summary statistics
        ProductSummaryResponse.ProductListSummary summary = productBuildResponseHelper.buildProductListSummary(products, shop);

        //7. Build final response
        ProductSummaryResponse.ShopProductsListResponse finalResponse = ProductSummaryResponse.ShopProductsListResponse.builder()
                .shop(productBuildResponseHelper.buildShopSummaryForProducts(shop))
                .summary(summary)
                .products(responses)
                .totalProducts(responses.size())
                .build();

        return GlobeSuccessResponseBuilder.success(
                String.format("Retrieved %d products from shop: %s", products.size(), shop.getShopName()),
                finalResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getAllProductsPaged(UUID shopId, int page, int size) throws ItemNotFoundException, RandomExceptions {

        // Get authenticated user
        AccountEntity authenticatedAccount = getAuthenticatedAccount();

        // Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // Check permissions
        boolean isValidToViewProducts = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), authenticatedAccount, shop);
        if (!isValidToViewProducts) {
            throw new RandomExceptions("You do not have permission to view products for this shop");
        }

        // Validate pagination
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ProductEntity> productPage = productRepo.findByShopAndIsDeletedFalseOrderByCreatedAtDesc(shop, pageable);

        // Build lightweight response list
        List<ProductSummaryResponse> responses = productPage.getContent().stream()
                .map(productBuildResponseHelper::buildProductSummaryResponse)
                .toList();

        // Build summary statistics
        ProductSummaryResponse.ProductListSummary summary = productBuildResponseHelper.buildProductListSummary(productPage.getContent(), shop);

        // Build main data response
        ProductSummaryResponse.ShopProductsListResponse dataResponse = ProductSummaryResponse.ShopProductsListResponse.builder()
                .shop(productBuildResponseHelper.buildShopSummaryForProducts(shop))
                .summary(summary)
                .products(responses)
                .totalProducts(responses.size())
                .build();

        // Build final response with pagination
        var finalResponse = new Object() {
            public final ProductSummaryResponse.ShopProductsListResponse contents = dataResponse;
            public final int currentPage = productPage.getNumber() + 1;
            public final int pageSize = productPage.getSize();
            public final long totalElements = productPage.getTotalElements();
            public final int totalPages = productPage.getTotalPages();
            public final boolean hasNext = productPage.hasNext();
            public final boolean hasPrevious = productPage.hasPrevious();
        };

        return GlobeSuccessResponseBuilder.success(
                String.format("Retrieved %d products from shop: %s (Page %d of %d)",
                        responses.size(), shop.getShopName(), productPage.getNumber() + 1, productPage.getTotalPages()),
                finalResponse
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateProduct(UUID shopId, UUID productId, UpdateProductRequest request, ReqAction action)
            throws ItemNotFoundException, RandomExceptions, ItemReadyExistException {

        // 1. Get authenticated user
        AccountEntity account = getAuthenticatedAccount();

        // 2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // 3. Check if the authenticated user is the owner or admin
        boolean isValidToUpdateProduct = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop);
        if (!isValidToUpdateProduct) {
            throw new RandomExceptions("You do not have permission to update products for this shop");
        }

        // 4. Find existing product
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // 5. Check if new product name already exists (if name is being changed)
        if (request.getProductName() != null &&
                !product.getProductName().equals(request.getProductName()) &&
                productRepo.existsByProductNameAndShopAndIsDeletedFalse(request.getProductName(), shop)) {
            throw new ItemReadyExistException("A product with this name already exists in this shop");
        }

        // 6. Check if new SKU already exists (if SKU is being changed)
        if (request.getSku() != null &&
                !request.getSku().equals(product.getSku()) &&
                productRepo.existsBySkuAndIsDeletedFalse(request.getSku())) {
            throw new ItemReadyExistException("A product with this SKU already exists");
        }

        // 7. Validate category if provided
        ProductCategoryEntity category = null;
        if (request.getCategoryId() != null) {
            category = productCategoryRepo.findByCategoryIdAndIsActiveTrue(request.getCategoryId())
                    .orElseThrow(() -> new ItemNotFoundException("Category not found or inactive"));
        }

        // 8. Update product fields using helper method
        productHelperMethods.updateProductFields(product, request, category, shop, account);

        // 8.5. Handle status based on action (just like in create)
        if (action == ReqAction.SAVE_PUBLISH) {
            product.setStatus(ProductStatus.ACTIVE);
        } else if (action == ReqAction.SAVE_DRAFT) {
            product.setStatus(ProductStatus.DRAFT);
        }
        // If no action specified, keep current status

        // 9. Save updated product
        ProductEntity updatedProduct = productRepo.save(product);

        // 10. Build response
        ProductDetailedResponse response = productBuildResponseHelper.buildDetailedProductResponse(updatedProduct);

        return GlobeSuccessResponseBuilder.success(
                String.format("Product updated successfully and %s",
                        action == ReqAction.SAVE_PUBLISH ? "published" : "saved as draft"),
                response
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder publishProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // 1. Get authenticated user
        AccountEntity account = getAuthenticatedAccount();

        // 2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // 3. Check permissions
        boolean isValidToPublishProduct = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop);
        if (!isValidToPublishProduct) {
            throw new RandomExceptions("You do not have permission to publish products for this shop");
        }

        // 4. Find existing product
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // 5. Check if product is already active
        if (product.getStatus() == ProductStatus.ACTIVE) {
            throw new RandomExceptions("Product is already published");
        }

        // 6. Validate product can be published (has minimum required fields)
        validateProductForPublishing(product);

        // 7. Update status to ACTIVE
        product.setStatus(ProductStatus.ACTIVE);
        product.setEditedBy(account.getId());
        product.setUpdatedAt(LocalDateTime.now());

        // 8. Save updated product
        ProductEntity publishedProduct = productRepo.save(product);

        // 9. Build response
        ProductDetailedResponse response = productBuildResponseHelper.buildDetailedProductResponse(publishedProduct);

        return GlobeSuccessResponseBuilder.success(
                String.format("Product '%s' published successfully", product.getProductName()),
                response
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder deleteProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // 1. Get authenticated user
        AccountEntity account = getAuthenticatedAccount();

        // 2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // 3. Check permissions
        boolean isValidToDeleteProduct = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop);
        if (!isValidToDeleteProduct) {
            throw new RandomExceptions("You do not have permission to delete products for this shop");
        }

        // 4. Find existing product
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // 5. Check current status and apply appropriate deletion logic
        String responseMessage;

        if (product.getStatus() == ProductStatus.DRAFT) {
            // HARD DELETE - Permanently remove from database
            productRepo.delete(product);
            responseMessage = String.format("Draft product '%s' has been permanently deleted", product.getProductName());

            log.info("Product hard deleted: {} by user: {}", product.getProductName(), account.getUserName());

            return GlobeSuccessResponseBuilder.success(responseMessage);

        } else {
            // SOFT DELETE - Mark as deleted but keep in database
            product.setIsDeleted(true);
            product.setDeletedAt(LocalDateTime.now());
            product.setDeletedBy(account.getId());
            product.setStatus(ProductStatus.ARCHIVED); // Change status to archived
            product.setEditedBy(account.getId());
            product.setUpdatedAt(LocalDateTime.now());

            productRepo.save(product);

            responseMessage = String.format("Product '%s' has been deleted and will be permanently removed after 30 days",
                    product.getProductName());

            log.info("Product soft deleted: {} by user: {}", product.getProductName(), account.getUserName());

            // Return deletion info
            var deletionInfo = new Object() {
                public final String message = responseMessage;
                public final String productName = product.getProductName();
                public final ProductStatus previousStatus = product.getStatus();
                public final LocalDateTime deletedAt = product.getDeletedAt();
                public final String deletionType = "SOFT_DELETE";
                public final String note = "Product will be permanently deleted after 30 days";
            };

            return GlobeSuccessResponseBuilder.success(responseMessage, deletionInfo);
        }
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder restoreProduct(UUID shopId, UUID productId)
            throws ItemNotFoundException, RandomExceptions {

        // 1. Get authenticated user
        AccountEntity account = getAuthenticatedAccount();

        // 2. Validate shop existence
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        // 3. Check permissions
        boolean isValidToRestoreProduct = validateSystemRolesOrOwner(
                List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop);
        if (!isValidToRestoreProduct) {
            throw new RandomExceptions("You do not have permission to restore products for this shop");
        }

        // 4. Find soft-deleted product (including deleted ones)
        ProductEntity product = productRepo.findByProductIdAndShop_ShopId(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // 5. Check if product is actually soft-deleted
        if (!product.getIsDeleted()) {
            throw new RandomExceptions("Product is not deleted and cannot be restored");
        }

        // 6. Restore the product
        product.setIsDeleted(false);
        product.setDeletedAt(null);
        product.setDeletedBy(null);
        product.setStatus(ProductStatus.DRAFT); // Restore as draft for safety
        product.setEditedBy(account.getId());
        product.setUpdatedAt(LocalDateTime.now());

        ProductEntity restoredProduct = productRepo.save(product);

        log.info("Product restored: {} by user: {}", product.getProductName(), account.getUserName());

        // Build response
        ProductDetailedResponse response = productBuildResponseHelper.buildDetailedProductResponse(restoredProduct);

        return GlobeSuccessResponseBuilder.success(
                String.format("Product '%s' has been restored successfully", product.getProductName()),
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getPublicProductsByShop(UUID shopId) throws ItemNotFoundException {

        // 1. Validate shop
        ShopEntity shop = validatePublicShop(shopId);

        // 2. Get only ACTIVE products for public viewing
        List<ProductEntity> products = productRepo.findByShopAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(shop, ProductStatus.ACTIVE);

        // 3. Build lightweight response list (reuse existing helper)
        List<ProductSummaryResponse> responses = products.stream()
                .map(productBuildResponseHelper::buildProductSummaryResponse)
                .toList();

        // 4. Build shop summary for public (no internal summary stats)
        ProductSummaryResponse.ShopSummaryForProducts shopSummary = productBuildResponseHelper.buildShopSummaryForProducts(shop);

        // 5. Build final response WITHOUT internal summary
        var finalResponse = new Object() {
            public final ProductSummaryResponse.ShopSummaryForProducts shop = shopSummary;
            public final List<ProductSummaryResponse> products = responses;
            public final Integer totalProducts = responses.size();
        };

        return GlobeSuccessResponseBuilder.success(
                String.format("Retrieved %d products from %s", products.size(), shop.getShopName()),
                finalResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getPublicProductsByShopPaged(UUID shopId, int page, int size) throws ItemNotFoundException {

        // 1. Validate shop
        ShopEntity shop = validatePublicShop(shopId);

        // 2. Validate pagination
        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. Get only ACTIVE products for public viewing
        Page<ProductEntity> productPage = productRepo.findByShopAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(shop, ProductStatus.ACTIVE, pageable);

        // 4. Build lightweight response list
        List<ProductSummaryResponse> responses = productPage.getContent().stream()
                .map(productBuildResponseHelper::buildProductSummaryResponse)
                .toList();

        // 5. Build shop summary for public
        ProductSummaryResponse.ShopSummaryForProducts shopSummary = productBuildResponseHelper.buildShopSummaryForProducts(shop);

        // 6. Build final response with pagination but WITHOUT internal summary
        var finalResponse = new Object() {
            public final Object contents = new Object() {
                public final ProductSummaryResponse.ShopSummaryForProducts shop = shopSummary;
                public final List<ProductSummaryResponse> products = responses;
                public final Integer totalProducts = responses.size();
            };
            public final int currentPage = productPage.getNumber() + 1;
            public final int pageSize = productPage.getSize();
            public final long totalElements = productPage.getTotalElements();
            public final int totalPages = productPage.getTotalPages();
            public final boolean hasNext = productPage.hasNext();
            public final boolean hasPrevious = productPage.hasPrevious();
        };

        return GlobeSuccessResponseBuilder.success(
                String.format("Retrieved %d products from %s (Page %d of %d)",
                        responses.size(), shop.getShopName(), productPage.getNumber() + 1, productPage.getTotalPages()),
                finalResponse
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductById(UUID shopId, UUID productId) throws ItemNotFoundException {

        // 1. Validate shop existence and is approved/active
        ShopEntity shop = validatePublicShop(shopId);

        // 2. Find product (only ACTIVE products for public)
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // 3. Check if product is publicly available
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ItemNotFoundException("Product not available");
        }

        // 4. Build public response (no sensitive details)
        ProductPublicResponse response = productBuildResponseHelper.buildPublicProductResponse(product);

        return GlobeSuccessResponseBuilder.success(
                "Product retrieved successfully",
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder searchProducts(UUID shopId, String query, List<ProductStatus> status,
                                                      int page, int size, String sortBy, String sortDir) throws ItemNotFoundException {

        // 1. Validate and sanitize search query
        String sanitizedQuery = productSearchHelper.validateAndSanitizeQuery(query);

        // 2. Determine user type and permissions
        SearchContext searchContext = productSearchHelper.determineSearchContext(shopId);

        // 3. Validate and set pagination parameters
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 50) size = 50; // Prevent large searches

        // 4. Validate and set sorting
        String validatedSortBy = productSearchHelper.validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // 5. Determine which product statuses to search
        List<ProductStatus> searchStatuses = productSearchHelper.determineSearchStatuses(status, searchContext);

        // 6. Build sort criteria (relevance vs other fields)
        Sort sort = productSearchHelper.buildSearchSort(validatedSortBy, direction, sanitizedQuery);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // 7. Execute search
        Page<ProductEntity> searchResults = productSearchHelper.executeProductSearch(searchContext.getShop(), sanitizedQuery, searchStatuses, pageable);

        // 8. Build response based on user type
        return productSearchHelper.buildSearchResponse(searchResults, sanitizedQuery, searchContext, searchStatuses);
    }


    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder filterProducts(UUID shopId, ProductFilterCriteria criteria,
                                                      int page, int size, String sortBy, String sortDir)
            throws ItemNotFoundException {

        // 1. Determine user context and permissions (reuse from search)
        SearchContext searchContext = productSearchHelper.determineSearchContext(shopId);

        // 2. Validate pagination
        if (page < 1) page = 1;
        if (size <= 0) size = 10;
        if (size > 50) size = 50; // Prevent large requests

        // 3. Validate and set sorting
        String validatedSortBy = productSearchHelper.validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, validatedSortBy);
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        // 4. Determine search statuses based on user permissions
        List<ProductStatus> searchStatuses = productSearchHelper.determineSearchStatuses(criteria.getStatus(), searchContext);

        // 5. Execute filter using helper
        Page<ProductEntity> filterResults = productFilterHelper.executeProductFilter(
                searchContext.getShop(), criteria, searchStatuses, pageable);

        // 6. Build response
        return productFilterHelper.buildFilterResponse(filterResults, criteria, searchContext, searchStatuses);
    }

    @Override
    public GlobeSuccessResponseBuilder findBySlug(UUID shopId, String slug) throws ItemNotFoundException {

        // 1. Validate shop existence and is approved/active
        ShopEntity shop = validatePublicShop(shopId);

        // 2. Find product (only ACTIVE products for public)
        ProductEntity product = productRepo.findByProductSlugAndShopAndIsDeletedFalse(slug, shop)
                .orElseThrow(() -> new ItemNotFoundException("Product with given slug do not found"));

        // 3. Check if product is publicly available
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ItemNotFoundException("Product not available");
        }

        // 4. Build public response (no sensitive details)
        ProductPublicResponse response = productBuildResponseHelper.buildPublicProductResponse(product);

        return GlobeSuccessResponseBuilder.success(
                "Product retrieved successfully",
                response
        );

    }


    // HELPER METHODS
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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, ShopEntity shop) {
        // Check if the user has any of the custom roles
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        // Check if the user is the owner of the shop
        boolean isOwner = shop.getOwner().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }

    private void validateProductForPublishing(ProductEntity product) throws RandomExceptions {
        List<String> missingFields = new ArrayList<>();

        // Check required fields for publishing
        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            missingFields.add("Product name");
        }

        if (product.getProductDescription() == null || product.getProductDescription().trim().isEmpty()) {
            missingFields.add("Product description");
        }

        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            missingFields.add("Valid price");
        }

        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            missingFields.add("Stock quantity");
        }

        if (product.getCategory() == null) {
            missingFields.add("Product category");
        }

        if (product.getProductImages() == null || product.getProductImages().isEmpty()) {
            missingFields.add("At least one product image");
        }

        // If there are missing fields, throw exception
        if (!missingFields.isEmpty()) {
            String missingFieldsList = String.join(", ", missingFields);
            throw new RandomExceptions(
                    String.format("Cannot publish product. Missing required fields: %s", missingFieldsList)
            );
        }

        // Check if group buying is enabled but missing required fields
        if (product.getGroupBuyingEnabled() != null && product.getGroupBuyingEnabled()) {
            List<String> groupBuyingIssues = new ArrayList<>();

            if (product.getGroupMaxSize() == null || product.getGroupMaxSize() < 2) {
                groupBuyingIssues.add("Group maximum size (at least 2)");
            }

            if (product.getGroupPrice() == null || product.getGroupPrice().compareTo(BigDecimal.ZERO) <= 0) {
                groupBuyingIssues.add("Group price");
            }

            if (product.getGroupTimeLimitHours() == null || product.getGroupTimeLimitHours() < 1) {
                groupBuyingIssues.add("Group time limit");
            }

            if (!groupBuyingIssues.isEmpty()) {
                String issuesList = String.join(", ", groupBuyingIssues);
                throw new RandomExceptions(
                        String.format("Cannot publish product. Group buying is enabled but missing: %s", issuesList)
                );
            }
        }

        // Check if installment is enabled but missing required fields
        if (product.getInstallmentEnabled() != null && product.getInstallmentEnabled()) {
            if (product.getInstallmentPlans() == null || product.getInstallmentPlans().isEmpty()) {
                throw new RandomExceptions(
                        "Cannot publish product. Installment is enabled but no installment plans are configured"
                );
            }
        }
    }

    private ShopEntity validatePublicShop(UUID shopId) throws ItemNotFoundException {
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        // Check if shop is publicly accessible
        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        if (!shop.isApproved()) {
            throw new ItemNotFoundException("Shop not available");
        }

        if (shop.getStatus() != ShopStatus.ACTIVE) {
            throw new ItemNotFoundException("Shop not available");
        }

        return shop;
    }

}