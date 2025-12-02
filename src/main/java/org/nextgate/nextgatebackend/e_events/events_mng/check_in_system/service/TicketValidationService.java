package org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.service;


import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.check_in_system.payloads.ValidateTicketResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

/**
 * Service for validating and checking in tickets
 */
public interface TicketValidationService {

    /**
     * Validate ticket and perform check-in
     *
     * Flow:
     * 1. Validate scanner (active, device fingerprint match)
     * 2. Verify JWT signature with event's public key
     * 3. Check if the ticket is already used (duplicate detection)
     * 4. Mark the ticket as checked in
     * 5. Update attendance (placeholder for now)
     *
     * @param request Validation request with JWT and scanner info
     * @return Validation response with result and ticket details
     * @throws ItemNotFoundException if scanner not found
     */
    ValidateTicketResponse validateAndCheckIn(ValidateTicketRequest request)
            throws ItemNotFoundException;
}