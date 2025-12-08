package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_comments",
        indexes = {
                @Index(name = "idx_post_comments_post_id", columnList = "postId"),
                @Index(name = "idx_post_comments_user_id", columnList = "userId"),
                @Index(name = "idx_post_comments_parent_id", columnList = "parentCommentId")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID userId;

    // For nested replies (null if top-level comment)
    private UUID parentCommentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int likesCount = 0;

    @Column(nullable = false)
    private int repliesCount = 0;

    @Column(nullable = false)
    private boolean isPinned = false; // Author can pin comments

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

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