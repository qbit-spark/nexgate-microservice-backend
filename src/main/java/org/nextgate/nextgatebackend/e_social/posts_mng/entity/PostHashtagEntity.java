package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_hashtags", indexes = {
        @Index(name = "idx_post_hashtags_post_id", columnList = "postId"),
        @Index(name = "idx_post_hashtags_hashtag", columnList = "hashtag"),
        @Index(name = "idx_post_hashtags_hashtag_created", columnList = "hashtag, createdAt")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostHashtagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private String hashtag;

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