package org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.ConfirmationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stores delivery confirmation codes for orders.
 * Codes are hashed for security (like passwords).
 */
@Entity
@Table(name = "delivery_confirmations", indexes = {
        @Index(name = "idx_confirmation_order", columnList = "order_id"),
        @Index(name = "idx_confirmation_status", columnList = "status"),
        @Index(name = "idx_confirmation_expires", columnList = "expiresAt")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryConfirmationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID confirmationId;

    // Link to order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "orderId", nullable = false)
    private ProductOrderEntity order;

    // Hashed confirmation code (NEVER store plain text!)
    @Column(nullable = false, length = 255)
    private String codeHash;

    // Salt used for hashing (stored separately for security)
    @Column(nullable = false, length = 255)
    private String salt;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfirmationStatus status;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime verifiedAt;

    // Who verified
    @Column(name = "verified_by")
    private UUID verifiedBy;

    // Verification metadata
    @Column(name = "verification_ip", length = 45)
    private String verificationIp;

    @Column(name = "verification_device", length = 255)
    private String verificationDevice;

    // Attempt tracking
    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 5;

    @Column
    private LocalDateTime lastAttemptAt;

    // Revocation (if needed)
    @Column(nullable = false)
    private Boolean isRevoked = false;

    @Column
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    // Metadata
    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    // Business logic
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean canAttempt() {
        return attemptCount < maxAttempts && !isExpired() && !isRevoked;
    }

    public void incrementAttempt() {
        this.attemptCount++;
        this.lastAttemptAt = LocalDateTime.now();
    }


}