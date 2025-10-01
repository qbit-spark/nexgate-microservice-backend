package org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.products_mng_service.categories.repo.ProductCategoryRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductHelperMethods {

    private final ProductRepo productRepo;

    // ===============================
// SKU GENERATION METHODS
// ===============================

    public String generateSKU(ShopEntity shop, ProductCategoryEntity category, CreateProductRequest request) {
        // SKU Format: SHP[SHORT-UUID]-[CATEGORY]-[BRAND]-[ATTRIBUTE]-[SEQUENCE]

        StringBuilder sku = new StringBuilder();

        // 1. Shop prefix with short UUID (first 8 characters)
        String shopPrefix = "SHP" + shop.getShopId().toString().substring(0, 8).toUpperCase();
        sku.append(shopPrefix).append("-");

        // 2. Category abbreviation (first 3 characters, uppercase)
        String categoryCode = category.getCategoryName()
                .replaceAll("[^A-Za-z0-9]", "")
                .substring(0, Math.min(3, category.getCategoryName().length()))
                .toUpperCase();
        sku.append(categoryCode).append("-");

        // 3. Brand abbreviation (first 3 characters if available)
        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            String brandCode = request.getBrand()
                    .replaceAll("[^A-Za-z0-9]", "")
                    .substring(0, Math.min(3, request.getBrand().length()))
                    .toUpperCase();
            sku.append(brandCode).append("-");
        }

        // 4. Product attribute (from specifications or product name)
        String attribute = extractMainAttribute(request);
        sku.append(attribute).append("-");

        // 5. Sequence number (count of products in this shop + 1)
        long productCount = productRepo.countByShopAndIsDeletedFalse(shop);
        sku.append(String.format("%04d", productCount + 1));

        return sku.toString();
    }

    public String generateUniqueSKU(ShopEntity shop, ProductCategoryEntity category, CreateProductRequest request) {
        String baseSKU = generateSKU(shop, category, request);
        String uniqueSKU = baseSKU;
        int counter = 1;

        // Keep generating until we find a unique SKU
        while (productRepo.existsBySkuAndIsDeletedFalse(uniqueSKU)) {
            uniqueSKU = baseSKU + "-" + String.format("%02d", counter);
            counter++;
        }

        return uniqueSKU;
    }

    public String extractMainAttribute(CreateProductRequest request) {
        // Try to extract main attribute from specifications
        if (request.getSpecifications() != null && !request.getSpecifications().isEmpty()) {
            // Look for common attributes
            Map<String, String> specs = request.getSpecifications();

            if (specs.containsKey("Storage") || specs.containsKey("storage")) {
                String storage = specs.getOrDefault("Storage", specs.get("storage"));
                return storage.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(3, storage.length())).toUpperCase();
            }

            if (specs.containsKey("RAM") || specs.containsKey("Memory")) {
                String memory = specs.getOrDefault("RAM", specs.get("Memory"));
                return memory.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(3, memory.length())).toUpperCase();
            }

            if (specs.containsKey("Size") || specs.containsKey("Screen Size")) {
                String size = specs.getOrDefault("Size", specs.get("Screen Size"));
                return size.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(3, size.length())).toUpperCase();
            }

            // If no common attributes, use first specification value
            String firstValue = specs.values().iterator().next();
            return firstValue.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(3, firstValue.length())).toUpperCase();
        }

        // Fallback: use first 3 characters of product name
        return request.getProductName()
                .replaceAll("[^A-Za-z0-9]", "")
                .substring(0, Math.min(3, request.getProductName().length()))
                .toUpperCase();
    }

    public void updateProductFields(ProductEntity product, UpdateProductRequest request,
                                    ProductCategoryEntity category, ShopEntity shop, AccountEntity account) {

        // Basic Information
        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
            // Regenerate slug if name changed
            String newSlug = generateUniqueSlugForShop(request.getProductName(), shop.getShopId());
            product.setProductSlug(newSlug);
        }

        if (request.getProductDescription() != null) {
            product.setProductDescription(request.getProductDescription());
        }

        if (request.getShortDescription() != null) {
            product.setShortDescription(request.getShortDescription());
        }

        if (request.getProductImages() != null) {
            product.setProductImages(request.getProductImages());
        }

        // Pricing
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }

        if (request.getComparePrice() != null) {
            product.setComparePrice(request.getComparePrice());
        }

        // Inventory
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }

        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }

        if (request.getTrackInventory() != null) {
            product.setTrackInventory(request.getTrackInventory());
        }

        // Product Details
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }

        if (request.getSku() != null) {
            product.setSku(request.getSku());
        }

        if (request.getCondition() != null) {
            product.setCondition(request.getCondition());
        }

        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        // Category
        if (category != null) {
            product.setCategory(category);
        }

        // SEO and Tags
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }

        if (request.getMetaTitle() != null) {
            product.setMetaTitle(request.getMetaTitle());
        }

        if (request.getMetaDescription() != null) {
            product.setMetaDescription(request.getMetaDescription());
        }

        // Features
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        if (request.getIsDigital() != null) {
            product.setIsDigital(request.getIsDigital());
        }

        if (request.getRequiresShipping() != null) {
            product.setRequiresShipping(request.getRequiresShipping());
        }

        // ===============================
        // NEW FIELDS - SPECIFICATIONS
        // ===============================
        if (request.getSpecifications() != null) {
            product.setSpecifications(request.getSpecifications());
        }

        // ===============================
        // NEW FIELDS - COLORS
        // ===============================
        if (request.getColors() != null) {
            product.setColors(convertColorsToEntityFromUpdate(request.getColors()));
        }

        // ===============================
        // NEW FIELDS - ORDERING LIMITS
        // ===============================
        if (request.getMinOrderQuantity() != null) {
            product.setMinOrderQuantity(request.getMinOrderQuantity());
        }

        if (request.getMaxOrderQuantity() != null) {
            product.setMaxOrderQuantity(request.getMaxOrderQuantity());
        }

        if (request.getRequiresApproval() != null) {
            product.setRequiresApproval(request.getRequiresApproval());
        }

        // ===============================
        // NEW FIELDS - GROUP BUYING
        // ===============================
        if (request.getGroupBuyingEnabled() != null) {
            product.setGroupBuyingEnabled(request.getGroupBuyingEnabled());

            if (request.getGroupBuyingEnabled()) {

                if (request.getGroupMaxSize() != null) {
                    product.setGroupMaxSize(request.getGroupMaxSize());
                }
                if (request.getGroupPrice() != null) {
                    product.setGroupPrice(request.getGroupPrice());
                }
                if (request.getGroupTimeLimitHours() != null) {
                    product.setGroupTimeLimitHours(request.getGroupTimeLimitHours());
                }
            } else {
                // If disabling group buying, clear all group buying fields
                product.setGroupMaxSize(null);
                product.setGroupPrice(null);
                product.setGroupTimeLimitHours(null);
            }
        } else {
            // Update individual group buying fields if group buying is already enabled
            if (product.getGroupBuyingEnabled() != null && product.getGroupBuyingEnabled()) {
                if (request.getGroupMaxSize() != null) {
                    product.setGroupMaxSize(request.getGroupMaxSize());
                }
                if (request.getGroupPrice() != null) {
                    product.setGroupPrice(request.getGroupPrice());
                }
                if (request.getGroupTimeLimitHours() != null) {
                    product.setGroupTimeLimitHours(request.getGroupTimeLimitHours());
                }

            }
        }

        // ===============================
        // NEW FIELDS - INSTALLMENT OPTIONS
        // ===============================
        if (request.getInstallmentEnabled() != null) {
            product.setInstallmentEnabled(request.getInstallmentEnabled());

            if (request.getInstallmentEnabled()) {
                if (request.getInstallmentPlans() != null) {
                    product.setInstallmentPlans(convertInstallmentPlansToEntityFromUpdate(request.getInstallmentPlans()));
                }
                if (request.getDownPaymentRequired() != null) {
                    product.setDownPaymentRequired(request.getDownPaymentRequired());
                }
                if (request.getMinDownPaymentPercentage() != null) {
                    product.setMinDownPaymentPercentage(request.getMinDownPaymentPercentage());
                }
            } else {
                // If disabling installments, clear all installment fields
                product.setInstallmentPlans(new ArrayList<>());
                product.setDownPaymentRequired(false);
                product.setMinDownPaymentPercentage(BigDecimal.ZERO);
            }
        } else {
            // Update individual installment fields if installments are already enabled
            if (product.getInstallmentEnabled() != null && product.getInstallmentEnabled()) {
                if (request.getInstallmentPlans() != null) {
                    product.setInstallmentPlans(convertInstallmentPlansToEntityFromUpdate(request.getInstallmentPlans()));
                }
                if (request.getDownPaymentRequired() != null) {
                    product.setDownPaymentRequired(request.getDownPaymentRequired());
                }
                if (request.getMinDownPaymentPercentage() != null) {
                    product.setMinDownPaymentPercentage(request.getMinDownPaymentPercentage());
                }
            }
        }

        // System fields - always update these
        product.setEditedBy(account.getId());
        product.setUpdatedAt(LocalDateTime.now());
    }

// ===============================
// CONVERSION HELPER METHODS
// ===============================

    public List<Map<String, Object>> convertColorsToEntity(List<CreateProductRequest.ColorRequest> colorRequests) {
        if (colorRequests == null || colorRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> colors = new ArrayList<>();
        for (CreateProductRequest.ColorRequest colorRequest : colorRequests) {
            Map<String, Object> color = new HashMap<>();
            color.put("name", colorRequest.getName());
            color.put("hex", colorRequest.getHex());
            color.put("images", colorRequest.getImages() != null ? colorRequest.getImages() : new ArrayList<>());
            color.put("priceAdjustment", colorRequest.getPriceAdjustment() != null ? colorRequest.getPriceAdjustment() : BigDecimal.ZERO);
            colors.add(color);
        }

        return colors;
    }

    public List<Map<String, Object>> convertInstallmentPlansToEntity(List<CreateProductRequest.InstallmentPlanRequest> planRequests) {
        if (planRequests == null || planRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> plans = new ArrayList<>();
        for (CreateProductRequest.InstallmentPlanRequest planRequest : planRequests) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("duration", planRequest.getDuration());
            plan.put("interval", planRequest.getInterval());
            plan.put("interestRate", planRequest.getInterestRate() != null ? planRequest.getInterestRate() : BigDecimal.ZERO);
            plan.put("description", planRequest.getDescription());
            plans.add(plan);
        }

        return plans;
    }

    // Also add this method to ProductHelperMethods for slug generation
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


    public List<Map<String, Object>> convertColorsToEntityFromUpdate(List<UpdateProductRequest.ColorRequest> colorRequests) {
        if (colorRequests == null || colorRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> colors = new ArrayList<>();
        for (UpdateProductRequest.ColorRequest colorRequest : colorRequests) {
            Map<String, Object> color = new HashMap<>();
            color.put("name", colorRequest.getName());
            color.put("hex", colorRequest.getHex());
            color.put("images", colorRequest.getImages() != null ? colorRequest.getImages() : new ArrayList<>());
            color.put("priceAdjustment", colorRequest.getPriceAdjustment() != null ? colorRequest.getPriceAdjustment() : BigDecimal.ZERO);
            colors.add(color);
        }

        return colors;
    }

    public List<Map<String, Object>> convertInstallmentPlansToEntityFromUpdate(List<UpdateProductRequest.InstallmentPlanRequest> planRequests) {
        if (planRequests == null || planRequests.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> plans = new ArrayList<>();
        for (UpdateProductRequest.InstallmentPlanRequest planRequest : planRequests) {
            Map<String, Object> plan = new HashMap<>();
            plan.put("duration", planRequest.getDuration());
            plan.put("interval", planRequest.getInterval());
            plan.put("interestRate", planRequest.getInterestRate() != null ? planRequest.getInterestRate() : BigDecimal.ZERO);
            plan.put("description", planRequest.getDescription());
            plans.add(plan);
        }

        return plans;
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
}
