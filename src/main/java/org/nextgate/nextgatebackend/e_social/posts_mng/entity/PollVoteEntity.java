package org.nextgate.nextgatebackend.e_social.posts_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "poll_votes", indexes = {
        @Index(name = "idx_poll_votes_poll_id",   columnList = "pollId"),
        @Index(name = "idx_poll_votes_option_id", columnList = "optionId"),
        @Index(name = "idx_poll_votes_voter_id",  columnList = "voterId"),
        @Index(name = "idx_poll_votes_poll_voter", columnList = "pollId, voterId")
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
    private UUID voterId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}