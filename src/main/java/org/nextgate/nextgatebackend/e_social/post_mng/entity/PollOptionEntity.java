package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "poll_options", indexes = {
        @Index(name = "idx_poll_id", columnList = "pollId"),
        @Index(name = "idx_poll_order", columnList = "pollId, optionOrder")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PollOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID pollId;

    @Column(nullable = false)
    private String optionText;

    private String optionImageUrl;

    @Column(nullable = false)
    private int optionOrder;

    @Column(nullable = false)
    private long votesCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}