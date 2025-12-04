package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.repo;


import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.enums.ScannerStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;



public interface ScannerRepository extends JpaRepository<ScannerEntity, UUID> {

    /**
     * Find scanner by scanner ID (for authentication)
     */
    Optional<ScannerEntity> findByScannerId(String scannerId);

    /**
     * Find all scanners for a specific event
     * Used by: Admin dashboard to view event scanners
     */
    List<ScannerEntity> findByEvent(EventEntity event);

    /**
     * Find active scanners for an event
     * Used by: Admin dashboard to see which scanners are currently active
     */
    List<ScannerEntity> findByEventAndStatus(EventEntity event, ScannerStatus status);

    /**
     * Check if device fingerprint exists for an event (prevent duplicate registration)
     * Used by: Scanner registration validation
     */
    boolean existsByEventAndDeviceFingerprint(EventEntity event, String deviceFingerprint);

    /**
     * Find scanner by device fingerprint and event (for re-registration detection)
     * Used by: Allowing the same device to re-register after closing session
     */
    Optional<ScannerEntity> findByEventAndDeviceFingerprint(EventEntity event, String deviceFingerprint);

    List<ScannerEntity> findByDeviceFingerprintAndStatus(String deviceFingerprint, ScannerStatus status);
}