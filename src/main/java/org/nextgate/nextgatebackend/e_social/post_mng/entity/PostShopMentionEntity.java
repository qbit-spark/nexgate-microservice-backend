package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_shop_mentions", indexes = {
        @Index(name = "idx_post_id", columnList = "postId"),
        @Index(name = "idx_mentioned_shop_id", columnList = "mentionedShopId"),
        @Index(name = "idx_mentioned_shop_created", columnList = "mentionedShopId, createdAt")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_shop", columnNames = {"postId", "mentionedShopId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostShopMentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID mentionedShopId;

    @Column(nullable = false)
    private int startIndex;

    @Column(nullable = false)
    private int endIndex;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}