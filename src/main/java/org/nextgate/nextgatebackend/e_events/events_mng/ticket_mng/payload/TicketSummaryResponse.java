package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight ticket summary for list views
 * Contains only essential information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {

    private UUID id;
    private String name;
    private BigDecimal price;

    // Quantity info
    private Integer totalTickets;
    private Integer ticketsSold;
    private Integer ticketsAvailable;
    private Boolean isSoldOut;

    // For HYBRID events
    private AttendanceMode attendanceMode;

    // Status
    private TicketStatus status;
    private Boolean isOnSale;
}