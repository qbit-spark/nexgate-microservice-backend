package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketValidityType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Full ticket response with all details
 * Used for single ticket view and detailed responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private UUID id;
    private UUID eventId;

    // ========== BASIC INFO ==========
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;

    // ========== QUANTITY (Using "tickets" terminology) ==========
    private Integer totalTickets;
    private Integer ticketsSold;
    private Integer ticketsRemaining;
    private Integer ticketsAvailable;
    private Boolean isUnlimited;
    private Boolean isSoldOut;

    // ========== SALES PERIOD ==========
    private ZonedDateTime salesStartDateTime;
    private ZonedDateTime salesEndDateTime;
    private Boolean isOnSale; // Calculated: currently available for purchase

    // ========== PURCHASE LIMITS ==========
    private Integer minQuantityPerOrder;
    private Integer maxQuantityPerOrder;
    private Integer maxQuantityPerUser;

    // ========== TICKET VALIDITY ==========
    private TicketValidityType validUntilType;
    private ZonedDateTime customValidUntil;

    // ========== FOR HYBRID EVENTS ==========
    private AttendanceMode attendanceMode;

    // ========== INCLUSIVE ITEMS ==========
    @Builder.Default
    private List<String> inclusiveItems = new ArrayList<>();

    // ========== VISIBILITY & STATUS ==========
    private Boolean isHidden;
    private TicketStatus status;

    // ========== AUDIT INFO ==========
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String createdBy; // Username
    private String updatedBy; // Username
}