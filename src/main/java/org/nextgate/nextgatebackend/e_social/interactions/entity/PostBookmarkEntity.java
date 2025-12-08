package org.nextgate.nextgatebackend.e_social.interactions.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_bookmarks",
        indexes = {
                @Index(name = "idx_post_bookmarks_post_id", columnList = "postId"),
                @Index(name = "idx_post_bookmarks_user_id", columnList = "userId")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_post_bookmarks_post_user", columnNames = {"postId", "userId"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostBookmarkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}