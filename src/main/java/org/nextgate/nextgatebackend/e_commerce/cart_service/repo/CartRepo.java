package org.nextgate.nextgatebackend.e_commerce.cart_service.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepo extends JpaRepository<CartEntity, UUID> {

    Optional<CartEntity> findByUser(AccountEntity user);

    boolean existsByUser(AccountEntity user);

    void deleteByUser(AccountEntity user);
}