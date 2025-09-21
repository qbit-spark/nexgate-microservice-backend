package org.nextgate.nextgatebackend.wishlist_service.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.wishlist_service.entity.WishlistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepo extends JpaRepository<WishlistItemEntity, UUID> {

    List<WishlistItemEntity> findByUserOrderByCreatedAtDesc(AccountEntity user);

    Optional<WishlistItemEntity> findByUserAndProduct(AccountEntity user, ProductEntity product);

    Optional<WishlistItemEntity> findByWishlistIdAndUser(UUID wishlistId, AccountEntity user);

    boolean existsByUserAndProduct(AccountEntity user, ProductEntity product);

    void deleteByUserAndProduct(AccountEntity user, ProductEntity product);

    void deleteByUser(AccountEntity user);

    long countByUser(AccountEntity user);
}