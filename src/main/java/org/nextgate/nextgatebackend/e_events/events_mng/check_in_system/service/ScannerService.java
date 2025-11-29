package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service;

import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegisterScannerRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface ScannerService {
    ScannerEntity registerScanner(RegisterScannerRequest request) throws ItemNotFoundException;
    ScannerEntity getByScannerId(String scannerId) throws ItemNotFoundException;
    List<ScannerEntity> getScannersForEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException;
    void revokeScanner(String scannerId, String reason) throws ItemNotFoundException, AccessDeniedException;
}
