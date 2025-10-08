package org.nextgate.nextgatebackend.products_mng_service.products.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.installment_purchase.utils.helpers.InstallmentPlanResponseHelper;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateInstallmentPlanRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.InstallmentPlanResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.InstallmentPlansListResponse;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateInstallmentPlanRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.service.InstallmentPlanService;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentPlanServiceImpl implements InstallmentPlanService {

    private final InstallmentPlanRepo installmentPlanRepo;
    private final ProductRepo productRepo;
    private final ShopRepo shopRepo;
    private final AccountRepo accountRepo;
    private final InstallmentPlanResponseHelper installmentPlanResponseHelper;

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder createInstallmentPlan(UUID shopId, UUID productId,
                                                             CreateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Creating installment plan for product: {}", productId);

        // Get authenticated user
        AccountEntity account = getAuthenticatedAccount();

        // Validate shop
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        // Check permissions
        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to create installment plans for this shop");
        }

        // Validate product
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // Build installment plan entity
        InstallmentPlanEntity plan = InstallmentPlanEntity.builder()
                .planName(request.getPlanName())
                .paymentFrequency(request.getPaymentFrequency())
                .customFrequencyDays(request.getCustomFrequencyDays())
                .numberOfPayments(request.getNumberOfPayments())
                .apr(request.getApr())
                .minDownPaymentPercent(request.getMinDownPaymentPercent())
                .gracePeriodDays(request.getGracePeriodDays())
                .fulfillmentTiming(request.getFulfillmentTiming())
                .displayOrder(request.getDisplayOrder())
                .isFeatured(request.getIsFeatured())
                .isActive(request.getIsActive())
                .product(product)
                .shop(shop)
                .metadata(new HashMap<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // If this is set as featured, unfeatured all other plans
        if (request.getIsFeatured()) {
            unfeaturedOtherPlans(product);
        }

        // Save plan
        InstallmentPlanEntity savedPlan = installmentPlanRepo.save(plan);

        // Enable installments on product if not already enabled
        if (!Boolean.TRUE.equals(product.getInstallmentEnabled())) {
            product.setInstallmentEnabled(true);
            productRepo.save(product);
        }

        log.info("✓ Installment plan created: {}", savedPlan.getPlanId());

        return GlobeSuccessResponseBuilder.success(
                "Installment plan created successfully",
                buildPlanResponse(savedPlan)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getProductInstallmentPlans(UUID shopId, UUID productId)
            throws ItemNotFoundException {

        // Validate product
        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // Get all plans
        List<InstallmentPlanEntity> plans = installmentPlanRepo.findByProductOrderByDisplayOrderAsc(product);

        // Build response
        List<InstallmentPlanResponse> planResponses = plans.stream()
                .map(this::buildPlanResponse)
                .collect(Collectors.toList());

        long activePlansCount = plans.stream().filter(InstallmentPlanEntity::getIsActive).count();

        InstallmentPlansListResponse response = InstallmentPlansListResponse.builder()
                .productId(productId)
                .productName(product.getProductName())
                .productPrice(product.getPrice())
                .installmentEnabled(product.getInstallmentEnabled())
                .totalPlans(plans.size())
                .activePlans(activePlansCount)
                .plans(planResponses)
                .build();

        return GlobeSuccessResponseBuilder.success(
                String.format("Retrieved %d installment plans", plans.size()),
                response
        );
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getInstallmentPlanById(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException {

        InstallmentPlanEntity plan = validateAndGetPlan(shopId, productId, planId);

        return GlobeSuccessResponseBuilder.success(
                "Installment plan retrieved successfully",
                buildPlanResponse(plan)
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateInstallmentPlan(UUID shopId, UUID productId, UUID planId,
                                                             UpdateInstallmentPlanRequest request)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Updating installment plan: {}", planId);

        AccountEntity account = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to update installment plans for this shop");
        }

        InstallmentPlanEntity plan = validateAndGetPlan(shopId, productId, planId);

        // Update fields
        if (request.getPlanName() != null) plan.setPlanName(request.getPlanName());
        if (request.getPaymentFrequency() != null) plan.setPaymentFrequency(request.getPaymentFrequency());
        if (request.getCustomFrequencyDays() != null) plan.setCustomFrequencyDays(request.getCustomFrequencyDays());
        if (request.getNumberOfPayments() != null) plan.setNumberOfPayments(request.getNumberOfPayments());
        if (request.getApr() != null) plan.setApr(request.getApr());
        if (request.getMinDownPaymentPercent() != null) plan.setMinDownPaymentPercent(request.getMinDownPaymentPercent());
        if (request.getGracePeriodDays() != null) plan.setGracePeriodDays(request.getGracePeriodDays());
        if (request.getFulfillmentTiming() != null) plan.setFulfillmentTiming(request.getFulfillmentTiming());
        if (request.getDisplayOrder() != null) plan.setDisplayOrder(request.getDisplayOrder());

        if (request.getIsFeatured() != null) {
            plan.setIsFeatured(request.getIsFeatured());
            if (request.getIsFeatured()) {
                unfeaturedOtherPlans(plan.getProduct());
            }
        }

        if (request.getIsActive() != null) plan.setIsActive(request.getIsActive());

        plan.setUpdatedAt(LocalDateTime.now());

        InstallmentPlanEntity updatedPlan = installmentPlanRepo.save(plan);

        log.info("✓ Installment plan updated: {}", updatedPlan.getPlanId());

        return GlobeSuccessResponseBuilder.success(
                "Installment plan updated successfully",
                buildPlanResponse(updatedPlan)
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder deleteInstallmentPlan(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Deleting installment plan: {}", planId);

        AccountEntity account = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to delete installment plans for this shop");
        }

        InstallmentPlanEntity plan = validateAndGetPlan(shopId, productId, planId);

        installmentPlanRepo.delete(plan);

        // Check if product should have installments disabled
        ProductEntity product = plan.getProduct();
        long remainingPlans = installmentPlanRepo.countByProductAndIsActiveTrue(product);
        if (remainingPlans == 0) {
            product.setInstallmentEnabled(false);
            productRepo.save(product);
            log.info("Disabled installments on product {} (no active plans remaining)", productId);
        }

        log.info("✓ Installment plan deleted: {}", planId);

        return GlobeSuccessResponseBuilder.success("Installment plan deleted successfully");
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder togglePlanStatus(UUID shopId, UUID productId, UUID planId, Boolean isActive)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Toggling plan status: {} to {}", planId, isActive);

        AccountEntity account = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to modify installment plans for this shop");
        }

        InstallmentPlanEntity plan = validateAndGetPlan(shopId, productId, planId);

        plan.setIsActive(isActive);
        plan.setUpdatedAt(LocalDateTime.now());
        installmentPlanRepo.save(plan);

        // Check if product should have installments disabled
        if (!isActive) {
            ProductEntity product = plan.getProduct();
            long remainingActivePlans = installmentPlanRepo.countByProductAndIsActiveTrue(product);
            if (remainingActivePlans == 0) {
                product.setInstallmentEnabled(false);
                productRepo.save(product);
                log.info("Disabled installments on product {} (no active plans remaining)", productId);
            }
        }

        String message = isActive ? "Installment plan activated successfully" : "Installment plan deactivated successfully";

        return GlobeSuccessResponseBuilder.success(message, buildPlanResponse(plan));
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder setFeaturedPlan(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Setting featured plan: {}", planId);

        AccountEntity account = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to modify installment plans for this shop");
        }

        InstallmentPlanEntity plan = validateAndGetPlan(shopId, productId, planId);

        unfeaturedOtherPlans(plan.getProduct());

        plan.setIsFeatured(true);
        plan.setUpdatedAt(LocalDateTime.now());
        installmentPlanRepo.save(plan);

        return GlobeSuccessResponseBuilder.success(
                "Plan set as featured successfully",
                buildPlanResponse(plan)
        );
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder toggleProductInstallments(UUID shopId, UUID productId, Boolean enabled)
            throws ItemNotFoundException, RandomExceptions {

        log.info("Toggling installments for product: {} to {}", productId, enabled);

        AccountEntity account = getAuthenticatedAccount();

        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        if (shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found");
        }

        if (!validateSystemRolesOrOwner(List.of("ROLE_SUPER_ADMIN", "ROLE_STAFF_ADMIN"), account, shop)) {
            throw new RandomExceptions("You do not have permission to modify products for this shop");
        }

        ProductEntity product = productRepo.findByProductIdAndShop_ShopIdAndIsDeletedFalse(productId, shopId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found in this shop"));

        // If enabling, check if at least one active plan exists
        if (enabled) {
            long activePlans = installmentPlanRepo.countByProductAndIsActiveTrue(product);
            if (activePlans == 0) {
                throw new RandomExceptions(
                        "Cannot enable installments: No active installment plans exist for this product. " +
                                "Please create at least one active plan first."
                );
            }
        }

        product.setInstallmentEnabled(enabled);
        productRepo.save(product);

        String message = enabled
                ? "Installments enabled successfully"
                : "Installments disabled successfully";

        log.info("✓ Installments {} for product: {}", enabled ? "enabled" : "disabled", productId);

        return GlobeSuccessResponseBuilder.success(message);
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private InstallmentPlanEntity validateAndGetPlan(UUID shopId, UUID productId, UUID planId)
            throws ItemNotFoundException {

        InstallmentPlanEntity plan = installmentPlanRepo.findById(planId)
                .orElseThrow(() -> new ItemNotFoundException("Installment plan not found"));

        if (!plan.getProduct().getProductId().equals(productId)) {
            throw new ItemNotFoundException("Plan does not belong to this product");
        }

        if (!plan.getShop().getShopId().equals(shopId)) {
            throw new ItemNotFoundException("Plan does not belong to this shop");
        }

        return plan;
    }

    private void unfeaturedOtherPlans(ProductEntity product) {
        List<InstallmentPlanEntity> featuredPlans = installmentPlanRepo
                .findByProductOrderByDisplayOrderAsc(product)
                .stream()
                .filter(InstallmentPlanEntity::getIsFeatured)
                .toList();

        for (InstallmentPlanEntity featuredPlan : featuredPlans) {
            featuredPlan.setIsFeatured(false);
            installmentPlanRepo.save(featuredPlan);
        }
    }

    /**
     * Build InstallmentPlanResponse DTO from entity
     */
    private InstallmentPlanResponse buildPlanResponse(InstallmentPlanEntity plan) {
        // Get primary image
        String primaryImage = null;
        if (plan.getProduct().getProductImages() != null &&
                !plan.getProduct().getProductImages().isEmpty()) {
            primaryImage = plan.getProduct().getProductImages().get(0);
        }

        // Build nested product info
        InstallmentPlanResponse.ProductBasicInfo productInfo =
                InstallmentPlanResponse.ProductBasicInfo.builder()
                        .productId(plan.getProduct().getProductId())
                        .productName(plan.getProduct().getProductName())
                        .productSlug(plan.getProduct().getProductSlug())
                        .productPrice(plan.getProduct().getPrice())
                        .primaryImage(primaryImage)
                        .build();

        // Build and return main response
        return InstallmentPlanResponse.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .displayOrder(plan.getDisplayOrder())
                .paymentFrequency(plan.getPaymentFrequency())
                .customFrequencyDays(plan.getCustomFrequencyDays())
                .numberOfPayments(plan.getNumberOfPayments())
                .apr(plan.getApr())
                .minDownPaymentPercent(plan.getMinDownPaymentPercent())
                .gracePeriodDays(plan.getGracePeriodDays())
                .calculatedDurationDays(plan.getCalculatedDurationDays())
                .calculatedDurationDisplay(plan.getCalculatedDurationDisplay())
                .fulfillmentTiming(plan.getFulfillmentTiming())
                .isActive(plan.getIsActive())
                .isFeatured(plan.getIsFeatured())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .product(productInfo)
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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, ShopEntity shop) {
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        boolean isOwner = shop.getOwner().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }
}