package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.ScannerStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Scanner Entity - Represents a registered device for ticket check-in
 *
 * Each scanner is linked to ONE event at a time (session-based).
 * To scan tickets for a different event, the scanner must close the current session
 * and register for the new event.
 *
 * Security Features:
 * - Device fingerprinting (prevents credential theft)
 * - Session-based access (one event at a time)
 * - JWT credentials (secure authentication)
 * - Automatic revocation on suspicious activity
 */
@Entity
@Table(
        name = "scanners",
        indexes = {
                @Index(name = "idx_scanner_id", columnList = "scanner_id"),
                @Index(name = "idx_scanner_event", columnList = "event_id"),
                @Index(name = "idx_scanner_status", columnList = "status"),
                @Index(name = "idx_device_fingerprint", columnList = "device_fingerprint"),
                @Index(name = "idx_scanner_active", columnList = "event_id, status"),
                @Index(name = "idx_scanner_created_by", columnList = "created_by")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScannerEntity {

    // ========================================
    // PRIMARY KEY
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Unique scanner identifier (UUID as string)
     * Used for API authentication
     */
    @Column(name = "scanner_id", unique = true, nullable = false, length = 100)
    private String scannerId;

    // ========================================
    // SCANNER IDENTIFICATION
    // ========================================

    /**
     * Human-readable name for the scanner
     * Example: "Gate A - Main Entrance", "VIP Section Scanner"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;



    // ========================================
    // EVENT RELATIONSHIP
    // ========================================

    /**
     * The event this scanner is registered for
     * Each scanner can only scan tickets for ONE event at a time
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    // ========================================
    // SECURITY & CREDENTIALS
    // ========================================

    /**
     * Scanner credentials (JWT token)
     * Used to authenticate scanner when making API calls
     * Contains: scannerId, eventId, expiration
     */
    @Column(name = "credentials", columnDefinition = "TEXT", nullable = false)
    private String credentials;

    /**
     * Device fingerprint hash
     * Computed from device hardware properties (model, OS, etc.)
     * Used to detect if credentials were copied to different device
     */
    @Column(name = "device_fingerprint", nullable = false, length = 64)
    private String deviceFingerprint;

    /**
     * Additional device information (stored as JSON for flexibility)
     * Example: device model, OS version, app version
     */
    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    // ========================================
    // STATUS & LIFECYCLE
    // ========================================

    /**
     * Scanner status
     * ACTIVE - Currently active and scanning
     * CLOSED - Session closed (scanner switched to different event)
     * REVOKED - Blocked due to security violation
     * EXPIRED - Credentials expired
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScannerStatus status = ScannerStatus.ACTIVE;
    /**
     * When scanner was registered
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When scanner record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * When session was closed (if status = CLOSED)
     */
    @Column(name = "closed_at")
    private Instant closedAt;

    /**
     * When scanner was revoked (if status = REVOKED)
     */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Reason for revocation
     */
    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    // ========================================
    // ACTIVITY TRACKING
    // ========================================

    /**
     * Last time scanner synced with server
     */
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    /**
     * Last successful scan timestamp
     */
    @Column(name = "last_scan_at")
    private Instant lastScanAt;

    /**
     * Total number of tickets scanned by this scanner
     */
    @Column(name = "total_scans")
    @Builder.Default
    private Integer totalScans = 0;

    /**
     * Number of successful validations
     */
    @Column(name = "successful_scans")
    @Builder.Default
    private Integer successfulScans = 0;

    /**
     * Number of failed validations (duplicates, invalid tickets)
     */
    @Column(name = "failed_scans")
    @Builder.Default
    private Integer failedScans = 0;

    // ========================================
    // NETWORK & LOCATION
    // ========================================

    /**
     * Last known IP address of scanner
     */
    @Column(name = "last_seen_ip", length = 45)
    private String lastSeenIp;

    /**
     * Last known location (GPS coordinates or venue location)
     * Stored as JSON: {"lat": -6.7924, "lng": 39.2083, "accuracy": 10}
     */
    @Column(name = "last_seen_location", columnDefinition = "TEXT")
    private String lastSeenLocation;

    // ========================================
    // AUDIT FIELDS
    // ========================================

    /**
     * Admin who registered this scanner
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private AccountEntity createdBy;

    /**
     * Admin who closed/revoked this scanner
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private AccountEntity closedBy;

    // ========================================
    // HELPER METHODS
    // ========================================
    public boolean isActive() {
        return ScannerStatus.ACTIVE.equals(status);
    }

    public boolean isClosed() {
        return ScannerStatus.CLOSED.equals(status);
    }

    public boolean isRevoked() {
        return ScannerStatus.REVOKED.equals(status);
    }

    public boolean isExpired() {
        return ScannerStatus.EXPIRED.equals(status);
    }

    public void close(AccountEntity closedBy) {
        this.status = ScannerStatus.CLOSED;
        this.closedAt = Instant.now();
        this.closedBy = closedBy;
    }

    public void revoke(String reason, AccountEntity revokedBy) {
        this.status = ScannerStatus.REVOKED;
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
        this.closedBy = revokedBy;
    }

    /**
     * Increment scan counters
     */
    public void recordScan(boolean successful) {
        this.totalScans++;
        if (successful) {
            this.successfulScans++;
        } else {
            this.failedScans++;
        }
        this.lastScanAt = Instant.now();
    }

    /**
     * Update last sync time
     */
    public void updateSyncTime() {
        this.lastSyncedAt = Instant.now();
    }

    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        if (totalScans == null || totalScans == 0) {
            return 0.0;
        }
        return (successfulScans * 100.0) / totalScans;
    }
}