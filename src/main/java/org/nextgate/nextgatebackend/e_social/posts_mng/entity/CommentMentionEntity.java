package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_mentions",
        indexes = {
                @Index(name = "idx_comment_mentions_comment_id", columnList = "commentId"),
                @Index(name = "idx_comment_mentions_user_id", columnList = "mentionedUserId")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CommentMentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID commentId;

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