package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.post_mng.enums.LinkStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_links", indexes = {
        @Index(name = "idx_post_id", columnList = "postId", unique = true),
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true)
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID postId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(nullable = false, unique = true)
    private String shortCode;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Column(nullable = false)
    private String domain;

    private String favicon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LinkStatus status = LinkStatus.PENDING;

    @Column(nullable = false)
    private boolean isSafe = true;

    @Column(nullable = false)
    private long clickCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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