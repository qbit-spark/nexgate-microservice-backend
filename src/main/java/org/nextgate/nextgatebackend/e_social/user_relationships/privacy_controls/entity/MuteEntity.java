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
        @Index(name = "idx_muter_id", columnList = "muterId"),
        @Index(name = "idx_muted_id", columnList = "mutedId"),
        @Index(name = "idx_muter_muted", columnList = "muterId,mutedId", unique = true)
})
public class MuteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID muterId;

    @Column(nullable = false)
    private UUID mutedId;

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;
}