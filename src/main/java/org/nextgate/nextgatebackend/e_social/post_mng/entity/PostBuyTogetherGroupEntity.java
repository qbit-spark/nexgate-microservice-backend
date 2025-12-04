package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_buy_together_groups", indexes = {
        @Index(name = "idx_post_buy_together_groups_post_id", columnList = "postId"),
        @Index(name = "idx_post_buy_together_groups_group_id", columnList = "groupId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_buy_together_groups_post_group", columnNames = {"postId", "groupId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostBuyTogetherGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID groupId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}