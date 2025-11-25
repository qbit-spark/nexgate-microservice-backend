package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Request to create an event ticket checkout session
 * Simplified design: buyer specifies tickets for self + other attendees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateEventCheckoutRequest {

    @NotNull(message = "Event ID is required")
    private UUID eventId;

    @NotNull(message = "Ticket type ID is required")
    private UUID ticketTypeId;

    @NotNull(message = "Tickets for buyer is required (use 0 if buyer not attending)")
    @Min(value = 0, message = "Tickets for buyer cannot be negative")
    private Integer ticketsForMe;

    @Valid
    private List<OtherAttendeeRequest> otherAttendees;  // Optional - can be empty/null

    @Builder.Default
    private Boolean sendTicketsToAttendees = false;  // Default: all QRs to buyer

    private UUID paymentMethodId;  // Optional - defaults to wallet

    // ========================================
    // NESTED CLASS
    // ========================================

    /**
     * Other attendee (friend/family) with their ticket quantity
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OtherAttendeeRequest {

        @NotBlank(message = "Attendee name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Attendee email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Attendee phone is required")
        @Pattern(regexp = "^\\+255[67]\\d{8}$", message = "Invalid phone number format. Must be Tanzania format (+255...)")
        private String phone;

        @NotNull(message = "Quantity is required for each attendee")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}