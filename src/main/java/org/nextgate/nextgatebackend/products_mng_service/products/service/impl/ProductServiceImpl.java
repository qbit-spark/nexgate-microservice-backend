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
import org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers.ProductHelperMethods;
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
    private final ProductHelperMethods productHelperMethods;
    private final ProductBuildResponseHelper productBuildResponseHelper;

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
            product.setGroupMinSize(request.getGroupMinSize());
            product.setGroupMaxSize(request.getGroupMaxSize());
            product.setGroupPrice(request.getGroupPrice());
            product.setGroupTimeLimitHours(request.getGroupTimeLimitHours());
            product.setGroupRequiresMinimum(request.getGroupRequiresMinimum());
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
    public GlobeSuccessResponseBuilder updateProduct(UUID shopId, UUID productId, UpdateProductRequest request)
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
                productRepo.existsBySkuAndShopAndIsDeletedFalse(request.getSku(), shop)) {
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

        // 9. Save updated product
        ProductEntity updatedProduct = productRepo.save(product);

        // 10. Build response
        ProductDetailedResponse response = productBuildResponseHelper.buildDetailedProductResponse(updatedProduct);

        return GlobeSuccessResponseBuilder.success(
                "Product updated successfully",
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

}