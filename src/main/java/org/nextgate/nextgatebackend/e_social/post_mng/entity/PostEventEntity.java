package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_events", indexes = {
        @Index(name = "idx_post_events_post_id", columnList = "postId"),
        @Index(name = "idx_post_events_event_id", columnList = "eventId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_events_post_event", columnNames = {"postId", "eventId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}