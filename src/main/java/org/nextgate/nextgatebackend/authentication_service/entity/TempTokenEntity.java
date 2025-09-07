package org.nextgate.nextgatebackend.authentication_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.enums.TempTokenPurpose;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "temp_token_table", indexes = {
        @Index(name = "idx_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_account_purpose", columnList = "account_id, purpose")
})
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TempTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;  // Hash of the actual JWT token for lookup

    @Column(name = "purpose", nullable = false)
    @Enumerated(EnumType.STRING)
    private TempTokenPurpose purpose;

    @Column(name = "identifier", nullable = false)
    private String identifier;  // Email, phone, or username used

    @Column(name = "user_identifier", nullable = false)
    private String userIdentifier;  // Always store this for auditing

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;  // Hash of expected OTP

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "attempts", nullable = false)
    private Integer attempts = 0;  // Track failed attempts

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true)  // ✅ Nullable for registration flow
    private AccountEntity account;

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isMaxAttemptsReached() {
        return attempts >= maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void markAsUsed() {
        this.isUsed = true;
    }
}