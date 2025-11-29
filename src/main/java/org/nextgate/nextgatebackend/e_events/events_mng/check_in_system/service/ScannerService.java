package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ScannerService {
    ScannerEntity registerScanner(RegisterScannerRequest request);
    ScannerEntity getByScannerId(String scannerId);
    List<ScannerEntity> getScannersForEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException;
    List<ScannerEntity> getActiveScannersForEvent(UUID eventId) throws AccessDeniedException, ItemNotFoundException;
    void closeScanner(String scannerId) throws ItemNotFoundException, AccessDeniedException;
    void revokeScanner(String scannerId, String reason) throws ItemNotFoundException, AccessDeniedException;
}
