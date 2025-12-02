package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service.TicketValidationService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/e-events/check-in/validate")
@RequiredArgsConstructor
public class TicketValidationController {

    private final TicketValidationService validationService;

    /**
     * Validate ticket and perform check-in
     *
     * POST /api/v1/e-events/check-in/validate
     *
     * Request Body:
     * {
     *   "jwtToken": "eyJhbGc...",
     *   "scannerId": "scanner-uuid-123",
     *   "deviceFingerprint": "abc123def456",
     *   "checkInLocation": "Gate A"
     * }
     *
     * Response:
     * {
     *   "success": true/false,
     *   "message": "Entry granted" / "Ticket already used",
     *   "data": {
     *     "valid": true/false,
     *     "status": "VALID" / "DUPLICATE" / "INVALID_SIGNATURE" / "NOT_FOUND",
     *     "attendeeName": "John Doe",
     *     ...
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> validateTicket(
            @Valid @RequestBody ValidateTicketRequest request)
            throws ItemNotFoundException {

        log.info("Ticket validation request from scanner: {}", request.getScannerId());

        ValidateTicketResponse response = validationService.validateAndCheckIn(request);

        // Determine HTTP status based on validation result
        HttpStatus status = response.getValid() ? HttpStatus.OK : HttpStatus.OK;
        // Note: We use 200 OK even for invalid tickets because it's not a server error
        // The "valid" field in response indicates success/failure

        return ResponseEntity.status(status)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(response.getValid())
                        .httpStatus(status)
                        .message(response.getMessage())
                        .data(response)
                        .build());
    }
}