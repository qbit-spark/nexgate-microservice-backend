package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "user_mutes", indexes = {
        @Index(name = "idx_blocker_id", columnList = "blockerId"),
        @Index(name = "idx_blocked_id", columnList = "blockedId"),
        @Index(name = "idx_blocker_blocked", columnList = "blockerId,blockedId", unique = true)
})
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID blockerId;

    @Column(nullable = false)
    private UUID blockedId;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;
}