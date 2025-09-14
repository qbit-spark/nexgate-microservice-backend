package org.nextgate.nextgatebackend.shops_mng_service.categories.repo;

import jakarta.validation.constraints.NotNull;
import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.UUID;

public interface ShopCategoryRepo extends JpaRepository<ShopCategoryEntity, UUID> {

    // BASIC FINDERS
    ShopCategoryEntity findByCategoryNameAndIsDeletedFalse(String categoryName);

    // EXISTENCE CHECK
    boolean existsByCategoryNameAndIsDeletedFalse(String categoryName);

    // NON-PAGEABLE LISTS
    List<ShopCategoryEntity> findByIsDeletedFalse();
    List<ShopCategoryEntity> findByIsDeletedFalseAndIsActiveTrue();

    // PAGEABLE LISTS
    Page<ShopCategoryEntity> findByIsDeletedFalseOrderByCreatedTimeDesc(Pageable pageable);
    Page<ShopCategoryEntity> findByIsDeletedFalseAndIsActiveTrueOrderByCategoryNameAsc(Pageable pageable);
    Page<ShopCategoryEntity> findByIsDeletedFalseOrderByCategoryNameAsc(Pageable pageable);

    // SEARCH
    Page<ShopCategoryEntity> findByIsDeletedFalseAndCategoryNameContainingIgnoreCase(String categoryName, Pageable pageable);

}