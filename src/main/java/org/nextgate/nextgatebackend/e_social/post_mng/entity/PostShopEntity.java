package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_shops", indexes = {
        @Index(name = "idx_post_shops_post_id", columnList = "postId"),
        @Index(name = "idx_post_shops_shop_id", columnList = "shopId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_shops_post_shop", columnNames = {"postId", "shopId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostShopEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID shopId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}