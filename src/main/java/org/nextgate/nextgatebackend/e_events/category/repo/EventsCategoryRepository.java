package org.nextgate.nextgatebackend.e_events.category.repo;

import org.jetbrains.annotations.NotNull;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventsCategoryRepository extends JpaRepository<EventsCategoryEntity, UUID> {

    Optional<EventsCategoryEntity> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndCategoryIdNot(String name, UUID categoryId);

    boolean existsBySlugAndCategoryIdNot(String slug, UUID categoryId);

    @NotNull Page<EventsCategoryEntity> findAll(@NotNull Pageable pageable);

}