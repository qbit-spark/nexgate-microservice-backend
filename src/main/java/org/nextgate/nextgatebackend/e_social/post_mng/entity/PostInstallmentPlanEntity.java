package org.nextgate.nextgatebackend.e_social.post_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_installment_plans", indexes = {
        @Index(name = "idx_post_installment_plans_post_id", columnList = "postId"),
        @Index(name = "idx_post_installment_plans_plan_id", columnList = "planId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_installment_plans_post_plan", columnNames = {"postId", "planId"})
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PostInstallmentPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID postId;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}