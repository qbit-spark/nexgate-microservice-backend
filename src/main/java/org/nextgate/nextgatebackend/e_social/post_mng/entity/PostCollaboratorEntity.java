package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.post_mng.enums.CollaboratorStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_collaborators", indexes = {
        @Index(name = "idx_post_collaborators_post_id", columnList = "postId"),
        @Index(name = "idx_post_collaborators_user_id", columnList = "userId"),
        @Index(name = "idx_post_collaborators_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_collaborators_post_user", columnNames = {"postId", "userId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostCollaboratorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaboratorStatus status = CollaboratorStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime invitedAt;

    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
    }
}