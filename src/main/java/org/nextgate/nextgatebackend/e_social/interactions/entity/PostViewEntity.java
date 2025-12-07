package org.nextgate.nextgatebackend.e_social.interactions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_views",
        indexes = {
                @Index(name = "idx_post_views_post_id", columnList = "postId"),
                @Index(name = "idx_post_views_user_id", columnList = "userId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_views_post_user", columnNames = {"postId", "userId"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostViewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int viewCount = 1; // Track multiple views by same user

    @Column(nullable = false, updatable = false)
    private LocalDateTime firstViewedAt;

    private LocalDateTime lastViewedAt;

    @PrePersist
    protected void onCreate() {
        firstViewedAt = LocalDateTime.now();
        lastViewedAt = LocalDateTime.now();
    }
}