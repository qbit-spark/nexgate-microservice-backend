package org.nextgate.nextgatebackend.products_mng_service.products.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.CreateProductRequest;
import org.nextgate.nextgatebackend.products_mng_service.products.payload.UpdateProductRequest;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    // ========================
    // PRODUCT MANAGEMENT (Shop Owner Operations)
    // ========================

    /**
     * Create a new product - Only shop owners and admins can create products for their shops
     */
    GlobeSuccessResponseBuilder createProduct(UUID shopId, CreateProductRequest request)
            throws ItemReadyExistException, ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Update an existing product - Only shop owner can update their products
//     */
//    GlobeSuccessResponseBuilder updateProduct(UUID productId, UpdateProductRequest request)
//            throws ItemNotFoundException, ItemReadyExistException, RandomExceptions;
//
//    /**
//     * Get detailed product information - For shop owners and admins
//     */
//    GlobeSuccessResponseBuilder getProductDetailed(UUID productId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Soft delete a product - Only shop owner can delete their products
//     */
//    GlobeSuccessResponseBuilder deleteProduct(UUID productId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Update product status - Shop owner can change status
//     */
//    GlobeSuccessResponseBuilder updateProductStatus(UUID productId, ProductStatus status)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Update product stock quantity
//     */
//    GlobeSuccessResponseBuilder updateProductStock(UUID productId, Integer newStock)
//            throws ItemNotFoundException, RandomExceptions;
//
//    // ========================
//    // SHOP OWNER PRODUCT QUERIES
//    // ========================
//
//    /**
//     * Get products by specific shop ID - For shop owners managing their inventory
//     */
//    GlobeSuccessResponseBuilder getProductsByShop(UUID shopId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Get products by shop with pagination - For shop owners
//     */
//    Page<ProductEntity> getProductsByShopPaged(UUID shopId, int page, int size)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Get all my shops' products summary - Returns products grouped by shop
//     */
//    GlobeSuccessResponseBuilder getMyShopsProductsSummary()
//            throws ItemNotFoundException;
//
//    /**
//     * Get products by shop and status - For inventory management
//     */
//    GlobeSuccessResponseBuilder getProductsByShopAndStatus(UUID shopId, ProductStatus status)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Get low stock products for a shop - Inventory alerts
//     */
//    GlobeSuccessResponseBuilder getLowStockProductsByShop(UUID shopId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    // ========================
//    // PUBLIC BROWSING (Customer View)
//    // ========================
//
//    /**
//     * Get single product for public view - Anyone can access active products
//     */
//    GlobeSuccessResponseBuilder getProductPublic(UUID productId)
//            throws ItemNotFoundException;
//
//    /**
//     * Get single product by slug for public view
//     */
//    GlobeSuccessResponseBuilder getProductBySlugPublic(String productSlug)
//            throws ItemNotFoundException;
//
//    /**
//     * Get all active products for public browsing
//     */
//    GlobeSuccessResponseBuilder getAllActiveProducts();
//
//    /**
//     * Get active products with pagination
//     */
//    Page<ProductEntity> getAllActiveProductsPaged(int page, int size);
//
//    /**
//     * Get featured products
//     */
//    GlobeSuccessResponseBuilder getFeaturedProducts();
//
//    /**
//     * Get featured products with pagination
//     */
//    Page<ProductEntity> getFeaturedProductsPaged(int page, int size);
//
//    /**
//     * Get recent products (newest first)
//     */
//    GlobeSuccessResponseBuilder getRecentProducts(int limit);
//
//    // ========================
//    // CATEGORY-BASED BROWSING
//    // ========================
//
//    /**
//     * Get products by category for public browsing
//     */
//    GlobeSuccessResponseBuilder getProductsByCategory(UUID categoryId)
//            throws ItemNotFoundException;
//
//    /**
//     * Get products by category with pagination
//     */
//    Page<ProductEntity> getProductsByCategoryPaged(UUID categoryId, int page, int size)
//            throws ItemNotFoundException;
//
//    /**
//     * Get products by shop and category combination
//     */
//    GlobeSuccessResponseBuilder getProductsByShopAndCategory(UUID shopId, UUID categoryId)
//            throws ItemNotFoundException;
//
//    // ========================
//    // SEARCH & FILTERING
//    // ========================
//
//    /**
//     * Search products by name (case-insensitive)
//     */
//    GlobeSuccessResponseBuilder searchProductsByName(String searchTerm);
//
//    /**
//     * Search products by name with pagination
//     */
//    Page<ProductEntity> searchProductsByNamePaged(String searchTerm, int page, int size);
//
//    /**
//     * Filter products by price range
//     */
//    GlobeSuccessResponseBuilder getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
//
//    /**
//     * Filter products by price range with pagination
//     */
//    Page<ProductEntity> getProductsByPriceRangePaged(BigDecimal minPrice, BigDecimal maxPrice, int page, int size);
//
//    /**
//     * Filter products by brand
//     */
//    GlobeSuccessResponseBuilder getProductsByBrand(String brand);
//
//    /**
//     * Filter products by brand with pagination
//     */
//    Page<ProductEntity> getProductsByBrandPaged(String brand, int page, int size);
//
//    /**
//     * Advanced search with multiple filters
//     */
//    Page<ProductEntity> searchProductsAdvanced(String searchTerm, UUID categoryId,
//                                               BigDecimal minPrice, BigDecimal maxPrice,
//                                               String brand, Boolean inStockOnly,
//                                               int page, int size);
//
//    // ========================
//    // INVENTORY MANAGEMENT
//    // ========================
//
//    /**
//     * Get out of stock products
//     */
//    GlobeSuccessResponseBuilder getOutOfStockProducts();
//
//    /**
//     * Get in-stock products only
//     */
//    GlobeSuccessResponseBuilder getInStockProducts();
//
//    /**
//     * Get products needing attention (low stock across all shops) - For admin
//     */
//    GlobeSuccessResponseBuilder getProductsNeedingAttention();
//
//    /**
//     * Bulk update product status - For shop owners
//     */
//    GlobeSuccessResponseBuilder bulkUpdateProductStatus(List<UUID> productIds, ProductStatus status)
//            throws ItemNotFoundException, RandomExceptions;
//
//    // ========================
//    // ADMIN OPERATIONS
//    // ========================
//
//    /**
//     * Get all products (admin view) - Including inactive/deleted
//     */
//    GlobeSuccessResponseBuilder getAllProductsAdmin();
//
//    /**
//     * Get all products with pagination (admin view)
//     */
//    Page<ProductEntity> getAllProductsAdminPaged(int page, int size);
//
//    /**
//     * Get products by status for admin review
//     */
//    GlobeSuccessResponseBuilder getProductsByStatusAdmin(ProductStatus status);
//
//    /**
//     * Admin: Force delete product (hard delete) - Super admin only
//     */
//    GlobeSuccessResponseBuilder forceDeleteProduct(UUID productId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    /**
//     * Admin: Restore deleted product
//     */
//    GlobeSuccessResponseBuilder restoreProduct(UUID productId)
//            throws ItemNotFoundException, RandomExceptions;
//
//    // ========================
//    // BUSINESS INTELLIGENCE & ANALYTICS
//    // ========================
//
//    /**
//     * Get product count by shop
//     */
//    Long getProductCountByShop(UUID shopId);
//
//    /**
//     * Get product count by status for a shop
//     */
//    Long getProductCountByShopAndStatus(UUID shopId, ProductStatus status);
//
//    /**
//     * Get shop's inventory summary
//     */
//    GlobeSuccessResponseBuilder getShopInventorySummary(UUID shopId)
//            throws ItemNotFoundException, RandomExceptions;
//
    // ========================
    // VALIDATION HELPERS
    // ========================

    /**
     * Check if product name exists for a shop
     */
//    boolean existsByNameAndShop(String productName, UUID shopId);
//
//    /**
//     * Check if SKU exists globally
//     */
//    boolean existsBySku(String sku);
//
//    /**
//     * Check if product slug exists
//     */
//    boolean existsBySlug(String productSlug);
}