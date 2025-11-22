package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.rates.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "shop_ratings", indexes = {
        @Index(name = "idx_rating_shop", columnList = "shop_id"),
        @Index(name = "idx_rating_user", columnList = "user_id"),
        @Index(name = "idx_rating_shop_user", columnList = "shop_id, user_id"),
        @Index(name = "idx_rating_value", columnList = "rating_value")
})
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShopRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", referencedColumnName = "shopId")
    private ShopEntity shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private AccountEntity user;

    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}