package org.nextgate.nextgatebackend.e_commerce.wishlist_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items", indexes = {
        @Index(name = "idx_wishlist_user", columnList = "user_id"),
        @Index(name = "idx_wishlist_product", columnList = "product_id"),
        @Index(name = "idx_wishlist_user_product", columnList = "user_id, product_id")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WishlistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID wishlistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private AccountEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    private ProductEntity product;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}