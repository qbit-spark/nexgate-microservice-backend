package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_user_mentions", indexes = {
        @Index(name = "idx_post_user_mentions_post_id", columnList = "postId"),
        @Index(name = "idx_post_user_mentions_user_id", columnList = "mentionedUserId"),
        @Index(name = "idx_post_user_mentions_user_created", columnList = "mentionedUserId, createdAt")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_user_mentions_post_user", columnNames = {"postId", "mentionedUserId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostUserMentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID mentionedUserId;

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