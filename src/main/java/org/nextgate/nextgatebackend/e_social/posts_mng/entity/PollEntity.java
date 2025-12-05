package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "polls", indexes = {
        @Index(name = "idx_polls_post_id", columnList = "postId", unique = true)
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PollEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID postId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean allowMultipleVotes = false;

    @Column(nullable = false)
    private boolean isAnonymous = true;

    @Column(nullable = false)
    private long totalVotes = 0;  // ADD THIS ✅

    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}