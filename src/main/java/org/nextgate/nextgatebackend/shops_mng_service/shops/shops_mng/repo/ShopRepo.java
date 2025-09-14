package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo;

import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShopRepo extends JpaRepository<ShopEntity, UUID> {
    boolean existsByShopNameAndIsDeletedFalse(String shopName);

}