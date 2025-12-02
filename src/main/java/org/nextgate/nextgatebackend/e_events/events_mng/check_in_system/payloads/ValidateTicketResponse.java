package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Response DTO for ticket validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTicketResponse {

    /**
     * Validation result
     */
    private Boolean valid;

    /**
     * Validation status code
     * VALID, DUPLICATE, INVALID_SIGNATURE, EXPIRED, NOT_FOUND, REVOKED
     */
    private String status;

    /**
     * Human-readable message
     */
    private String message;

    // ========================================
    // TICKET INFORMATION (if valid)
    // ========================================

    private UUID ticketInstanceId;
    private String ticketTypeName;
    private String ticketSeries;
    private String attendeeName;
    private String attendeeEmail;
    private String eventName;
    private String bookingReference;

    // ========================================
    // CHECK-IN INFORMATION
    // ========================================

    private Boolean alreadyCheckedIn;
    private ZonedDateTime previousCheckInTime;
    private String previousCheckInLocation;
    private ZonedDateTime currentCheckInTime;

    // ========================================
    // VALIDATION DETAILS
    // ========================================

    private String validationMode;  // ONLINE, OFFLINE
    private String scannerName;
}