package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
        @Index(name = "idx_posts_author_id", columnList = "authorId"),
        @Index(name = "idx_posts_post_type", columnList = "postType"),
        @Index(name = "idx_posts_status", columnList = "status"),
        @Index(name = "idx_posts_published_at", columnList = "publishedAt"),
        @Index(name = "idx_posts_created_at", columnList = "createdAt"),
        @Index(name = "idx_posts_is_deleted", columnList = "isDeleted"),
        @Index(name = "idx_posts_likes_count", columnList = "likesCount"),
        @Index(name = "idx_posts_status_published_deleted", columnList = "status, publishedAt, isDeleted")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID authorId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType postType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status = PostStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostVisibility visibility = PostVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentPermission whoCanComment = CommentPermission.EVERYONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepostPermission whoCanRepost = RepostPermission.EVERYONE;

    @Column(nullable = false)
    private boolean hideLikesCount = false;

    @Column(nullable = false)
    private boolean hideCommentsCount = false;

    @Column(nullable = false)
    private boolean isCollaborative = false;

    @Column(nullable = false)
    private boolean hasExternalLink = false;

    @Column(columnDefinition = "jsonb")
    private String mediaData;

    @Column(nullable = false)
    private boolean isDeleted = false;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private long likesCount = 0;

    @Column(nullable = false)
    private long commentsCount = 0;

    @Column(nullable = false)
    private long repostsCount = 0;

    @Column(nullable = false)
    private long bookmarksCount = 0;

    @Column(nullable = false)
    private long viewsCount = 0;

    private LocalDateTime scheduledAt;

    private LocalDateTime publishedAt;

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