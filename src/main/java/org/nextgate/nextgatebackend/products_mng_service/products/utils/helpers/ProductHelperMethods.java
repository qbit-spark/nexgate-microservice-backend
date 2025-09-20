package org.nextgate.nextgatebackend.products_mng_service.products.utils.helpers;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.products_mng_service.categories.entity.ProductCategoryEntity;
import org.nextgate.nextgatebackend.products_mng_service.categories.repo.ProductCategoryRepo;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
}
