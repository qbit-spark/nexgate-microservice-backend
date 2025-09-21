
package org.nextgate.nextgatebackend.products_mng_service.products.repo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepo extends JpaRepository<ProductEntity, UUID>, JpaSpecificationExecutor<ProductEntity> {

    // ========================
    // BASIC FINDERS
    // ========================

    Optional<ProductEntity> findByProductIdAndIsDeletedFalse(UUID productId);

    Optional<ProductEntity> findByProductIdAndShop_ShopIdAndIsDeletedFalse(UUID productId, UUID shopId);

    Optional<ProductEntity> findByProductSlugAndIsDeletedFalse(String productSlug);

    // ========================
    // EXISTENCE CHECKS
    // ========================

    boolean existsByProductNameAndShopAndIsDeletedFalse(String productName, ShopEntity shop);
    boolean existsByProductNameAndBrandAndPriceAndSpecificationsAndShopAndIsDeletedFalse(
            String productName,
            String brand,
            BigDecimal price,
            Map<String,String> specifications, ShopEntity shop
    );
    boolean existsBySkuAndIsDeletedFalse(String sku);

    Optional<ProductEntity> findByProductIdAndShop_ShopId(UUID productId, UUID shopId);


    Page<ProductEntity> findByShopAndIsDeletedFalseAndStatusInAndProductNameContainingIgnoreCaseOrderByCreatedAtDesc(
            ShopEntity shop, List<ProductStatus> status, String productName, Pageable pageable);


    @Query("SELECT p FROM ProductEntity p WHERE " +
            "p.shop = :shop AND " +
            "p.isDeleted = false AND " +
            "p.status IN :statuses AND " +
            "(LOWER(p.productName) LIKE LOWER(:query) OR " +
            "LOWER(p.productDescription) LIKE LOWER(:query) OR " +
            "LOWER(p.shortDescription) LIKE LOWER(:query) OR " +
            "LOWER(p.brand) LIKE LOWER(:query))")
    Page<ProductEntity> searchProductsWithLike(@Param("shop") ShopEntity shop,
                                               @Param("query") String query,
                                               @Param("statuses") List<ProductStatus> statuses,
                                               Pageable pageable);

    //exit by sku and shop
    boolean existsBySkuAndShopAndIsDeletedFalse(String sku, ShopEntity shop);

    boolean existsByProductSlugAndIsDeletedFalse(String productSlug);

    // New method for shop-scoped slug checking
    boolean existsByProductSlugAndShop_ShopIdAndIsDeletedFalse(String productSlug, UUID shopId);

    // Find product by shop slug and product slug
    Optional<ProductEntity> findByProductSlugAndShop_ShopSlugAndIsDeletedFalse(
            String productSlug, String shopSlug);

    // ========================
    // SHOP PRODUCTS (Shop Owner Management)
    // ========================

    // All products by shop
    List<ProductEntity> findByShopAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop);

    Page<ProductEntity> findByShopAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop, Pageable pageable);

    // Products by shop and status
    List<ProductEntity> findByShopAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop, ProductStatus status);

    Page<ProductEntity> findByShopAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop, ProductStatus status, Pageable pageable);

    // Low stock products for shop owners
    List<ProductEntity> findByShopAndStockQuantityLessThanEqualAndTrackInventoryTrueAndIsDeletedFalseOrderByStockQuantityAsc(ShopEntity shop, Integer threshold);

    // Count products by shop
    long countByShopAndIsDeletedFalse(ShopEntity shop);

    @Query("SELECT p FROM ProductEntity p WHERE " +
            "p.shop = :shop AND " +
            "p.isDeleted = false AND " +
            "p.status IN :statuses AND " +
            "(LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.productDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "EXISTS (SELECT 1 FROM p.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
            "EXISTS (SELECT 1 FROM p.specifications s WHERE LOWER(s) LIKE LOWER(CONCAT('%', :query, '%'))))")
    Page<ProductEntity> searchProductsInShop(@Param("shop") ShopEntity shop,
                                             @Param("query") String query,
                                             @Param("statuses") List<ProductStatus> statuses,
                                             Pageable pageable);

    long countByShopAndStatusAndIsDeletedFalse(ShopEntity shop, ProductStatus status);

    // ========================
    // PUBLIC BROWSING (Customer View) - FIXED: Removed duplicates
    // ========================

    // Active products for public browsing
    List<ProductEntity> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status);

    Page<ProductEntity> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    // Featured products
    List<ProductEntity> findByIsFeaturedTrueAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status);

    Page<ProductEntity> findByIsFeaturedTrueAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    // ========================
    // CATEGORY BROWSING
    // ========================

    List<ProductEntity> findByCategoryCategoryIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(UUID categoryId, ProductStatus status);

    Page<ProductEntity> findByCategoryCategoryIdAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(UUID categoryId, ProductStatus status, Pageable pageable);

    // ========================
    // SEARCH & FILTERING
    // ========================

    // Search by name (case-insensitive)
    List<ProductEntity> findByProductNameContainingIgnoreCaseAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(String name, ProductStatus status);

    Page<ProductEntity> findByProductNameContainingIgnoreCaseAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(String name, ProductStatus status, Pageable pageable);

    // Price range filtering
    List<ProductEntity> findByPriceBetweenAndStatusAndIsDeletedFalseOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice, ProductStatus status);

    Page<ProductEntity> findByPriceBetweenAndStatusAndIsDeletedFalseOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice, ProductStatus status, Pageable pageable);

    // Brand filtering
    List<ProductEntity> findByBrandIgnoreCaseAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(String brand, ProductStatus status);

    Page<ProductEntity> findByBrandIgnoreCaseAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(String brand, ProductStatus status, Pageable pageable);

    // ========================
    // SHOP + CATEGORY COMBINATION
    // ========================

    List<ProductEntity> findByShopAndCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop, UUID categoryId);

    Page<ProductEntity> findByShopAndCategoryCategoryIdAndIsDeletedFalseOrderByCreatedAtDesc(ShopEntity shop, UUID categoryId, Pageable pageable);

    // ========================
    // STOCK & INVENTORY
    // ========================

    // Out of stock products
    List<ProductEntity> findByStockQuantityAndIsDeletedFalseOrderByCreatedAtDesc(Integer stockQuantity);

    // In stock products
    List<ProductEntity> findByStockQuantityGreaterThanAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(Integer stockQuantity, ProductStatus status);

    // ========================
    // ADMIN QUERIES - FIXED: Renamed to avoid duplicates
    // ========================

    // All products (admin view) - renamed to avoid duplicate
    List<ProductEntity> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    Page<ProductEntity> findAllByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // ========================
    // BUSINESS INTELLIGENCE
    // ========================

    // Most recent products
    List<ProductEntity> findTop10ByStatusAndIsDeletedFalseOrderByCreatedAtDesc(ProductStatus status);

    // Products needing attention (low stock across all shops)
    List<ProductEntity> findByStockQuantityLessThanEqualAndTrackInventoryTrueAndStatusAndIsDeletedFalseOrderByStockQuantityAsc(Integer threshold, ProductStatus status);
}