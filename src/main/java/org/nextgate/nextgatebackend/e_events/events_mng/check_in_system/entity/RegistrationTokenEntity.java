package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Registration Token Entity
 *
 * Represents a time-limited token for registering a scanner device.
 * Similar to WhatsApp's "Link Device" feature.
 *
 * Flow:
 * 1. Admin generates registration token for specific event
 * 2. Admin displays QR code containing token
 * 3. Scanner app scans QR code
 * 4. Scanner app registers using token
 * 5. Token is marked as used (one-time use only)
 */
@Entity
@Table(
        name = "scanners_registration_tokens",
        indexes = {
                @Index(name = "idx_registration_token", columnList = "token"),
                @Index(name = "idx_registration_event", columnList = "event_id"),
                @Index(name = "idx_registration_valid", columnList = "used, expires_at"),
                @Index(name = "idx_registration_created_by", columnList = "created_by")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationTokenEntity {

    // ========================================
    // PRIMARY KEY
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The registration token (unique string)
     * Example: "REG-ABC123-XYZ789"
     */
    @Column(name = "token", unique = true, nullable = false, length = 100)
    private String token;

    // ========================================
    // EVENT RELATIONSHIP
    // ========================================

    /**
     * The event this registration token is for
     * Scanner will be registered to scan tickets for this event only
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    /**
     * Scanner name/description (optional)
     * Example: "Gate A - Main Entrance", "VIP Section"
     * If provided, will be used as default scanner name
     */
    @Column(name = "scanner_name", length = 200)
    private String scannerName;

    // ========================================
    // VALIDITY & USAGE
    // ========================================

    /**
     * Token expiration time
     * Typically set to 5-10 minutes from creation
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Token validity duration in minutes
     * Used to calculate expiresAt
     */
    @Column(name = "validity_minutes", nullable = false)
    @Builder.Default
    private Integer validityMinutes = 5;

    /**
     * Whether token has been used
     */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    /**
     * When token was used (if used = true)
     */
    @Column(name = "used_at")
    private Instant usedAt;

    /**
     * Scanner ID that used this token (if used)
     */
    @Column(name = "used_by_scanner", length = 100)
    private String usedByScanner;

    // ========================================
    // AUDIT FIELDS
    // ========================================

    /**
     * When token was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Admin who created this token
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private AccountEntity createdBy;

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Check if token is still valid (not used and not expired)
     */
    public boolean isValid() {
        if (used) {
            return false;
        }
        return Instant.now().isBefore(expiresAt);
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Mark token as used
     */
    public void markAsUsed(String scannerId) {
        this.used = true;
        this.usedAt = Instant.now();
        this.usedByScanner = scannerId;
    }

    /**
     * Get remaining validity time in seconds
     */
    public long getRemainingValiditySeconds() {
        if (isExpired()) {
            return 0;
        }
        return expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
    }
}