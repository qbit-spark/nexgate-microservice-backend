package org.nextgate.nextgatebackend.shops_mng_service.categories.repo;

import org.nextgate.nextgatebackend.shops_mng_service.categories.entity.ShopCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

public interface ShopCategoryRepo extends JpaRepository<ShopCategoryEntity, UUID> {

    // BASIC FINDERS
    Optional<ShopCategoryEntity> findByCategoryId(UUID categoryId);

    // EXISTENCE CHECK
    boolean existsByCategoryName(String categoryName);

    // NON-PAGEABLE LISTS
    List<ShopCategoryEntity> findByIsActive(Boolean isActive);

    Page<ShopCategoryEntity> findByIsActive(Boolean isActive, Pageable pageable);


}