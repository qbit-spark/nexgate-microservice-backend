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