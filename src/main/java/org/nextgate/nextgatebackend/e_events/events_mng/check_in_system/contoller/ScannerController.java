package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.entity.ScannerEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.RegisterScannerRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ScannerResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.ScannerService;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.utils.ScannerMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/e-event/check-in/scanners")
@RequiredArgsConstructor
public class ScannerController {

    private final ScannerService scannerService;
    private final ScannerMapper scannerMapper;

    @PostMapping("/register")
    public ResponseEntity<GlobeSuccessResponseBuilder> registerScanner(
            @Valid @RequestBody RegisterScannerRequest request)
            throws IllegalStateException, ItemNotFoundException {

        log.info("Registering new scanner");

        ScannerEntity scanner = scannerService.registerScanner(request);
        ScannerResponse response = scannerMapper.toResponse(scanner);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.CREATED)
                        .message("Scanner registered successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getScannersForEvent(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        log.info("Fetching scanners for event: {}", eventId);

        List<ScannerEntity> scanners = scannerService.getScannersForEvent(eventId);
        List<ScannerResponse> responseList = scanners.stream()
                .map(scannerMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Scanners retrieved successfully")
                        .data(responseList)
                        .build());
    }

    @GetMapping("/event/{eventId}/active")
    public ResponseEntity<GlobeSuccessResponseBuilder> getActiveScannersForEvent(@PathVariable UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        log.info("Fetching active scanners for event: {}", eventId);

        List<ScannerEntity> scanners = scannerService.getScannersForEvent(eventId);
        List<ScannerResponse> responseList = scanners.stream()
                .map(scannerMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Active scanners retrieved successfully")
                        .data(responseList)
                        .build());
    }

    @PostMapping("/{scannerId}/revoke")
    public ResponseEntity<GlobeSuccessResponseBuilder> revokeScanner(
            @PathVariable String scannerId,
            @RequestParam String reason)
            throws ItemNotFoundException, AccessDeniedException {

        log.info("Revoking scanner: {}", scannerId);

        scannerService.revokeScanner(scannerId, reason);

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Scanner revoked successfully")
                        .build());
    }
}