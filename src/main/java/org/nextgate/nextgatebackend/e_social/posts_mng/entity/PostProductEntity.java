package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_products", indexes = {
        @Index(name = "idx_post_products_post_id", columnList = "postId"),
        @Index(name = "idx_post_products_product_id", columnList = "productId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_products_post_product", columnNames = {"postId", "productId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}