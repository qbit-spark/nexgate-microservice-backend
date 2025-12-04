package org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.repo;

import org.nextgate.nextgatebackend.e_commerce.products_mng_service.categories.entity.ProductCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepo extends JpaRepository<ProductCategoryEntity, UUID> {

    Optional<ProductCategoryEntity> findByCategoryIdAndIsActiveTrue(UUID categoryId);
    boolean existsByCategoryName(String categoryName);

    List<ProductCategoryEntity> findByIsActiveTrueOrderByCreatedTimeDesc();
    Page<ProductCategoryEntity> findByIsActiveTrueOrderByCreatedTimeDesc(Pageable pageable);

    List<ProductCategoryEntity> findByIsActiveOrderByCreatedTimeDesc(Boolean isActive);
    Page<ProductCategoryEntity> findByIsActiveOrderByCreatedTimeDesc(Boolean isActive, Pageable pageable);

    List<ProductCategoryEntity> findByParentCategoryIsNullAndIsActiveTrueOrderByCreatedTimeDesc();

    List<ProductCategoryEntity> findByParentCategoryAndIsActiveTrueOrderByCreatedTimeDesc(ProductCategoryEntity parentCategory);
}
