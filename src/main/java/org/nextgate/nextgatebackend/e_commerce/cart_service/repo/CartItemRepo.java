package org.nextgate.nextgatebackend.e_commerce.cart_service.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartItemEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepo extends JpaRepository<CartItemEntity, UUID> {

    List<CartItemEntity> findByCartOrderByCreatedAtDesc(CartEntity cart);

    List<CartItemEntity> findByCart_UserOrderByCreatedAtDesc(AccountEntity user);

    Optional<CartItemEntity> findByCartAndProduct(CartEntity cart, ProductEntity product);

    Optional<CartItemEntity> findByCart_UserAndProduct(AccountEntity user, ProductEntity product);

    Optional<CartItemEntity> findByItemIdAndCart(UUID itemId, CartEntity cart);

    Optional<CartItemEntity> findByItemIdAndCart_User(UUID itemId, AccountEntity user);

    void deleteByCart(CartEntity cart);

    void deleteByCart_User(AccountEntity user);

    long countByCart(CartEntity cart);

    long countByCart_User(AccountEntity user);
}