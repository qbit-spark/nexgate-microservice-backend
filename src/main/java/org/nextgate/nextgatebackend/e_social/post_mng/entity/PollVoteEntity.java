package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "poll_votes", indexes = {
        @Index(name = "idx_poll_id", columnList = "pollId"),
        @Index(name = "idx_option_id", columnList = "optionId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_poll_user", columnList = "pollId, userId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_poll_user_option", columnNames = {"pollId", "userId", "optionId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PollVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID pollId;

    @Column(nullable = false)
    private UUID optionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}