package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity;


import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.TicketInstanceStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils.BookedTicketsJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.VirtualDetails;
import org.springframework.data.annotation.CreatedBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a confirmed event booking order
 * Created after successful payment of an EventCheckoutSession
 * Contains booked tickets with QR codes for event entry
 */
@Entity
@Table(name = "event_booking_orders", indexes = {
        @Index(name = "idx_booking_customer", columnList = "customer_id"),
        @Index(name = "idx_booking_event", columnList = "event_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_checkout", columnList = "checkout_session_id"),
        @Index(name = "idx_booking_date", columnList = "booked_at")
})
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventBookingOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID bookingId;

    // ========================================
    // REFERENCES
    // ========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AccountEntity customer;

    @Column(name = "checkout_session_id", nullable = false)
    private UUID checkoutSessionId;

    // ========================================
    // EVENT SNAPSHOT (for PDF generation)
    // ========================================
    // Store event details at time of booking (in case event changes later)

    @Column(name = "event_title", nullable = false, length = 255)
    private String eventTitle;

    @Column(name = "event_start_date_time", nullable = false)
    private LocalDateTime eventStartDateTime;

    @Column(name = "event_end_date_time", nullable = false)
    private LocalDateTime eventEndDateTime;

    @Column(name = "event_timezone", nullable = false, length = 50)
    private String eventTimezone;

    @Column(name = "event_location", length = 500)
    private String eventLocation;  // Full venue details (name + address)

    @Column(name = "event_format", length = 20)
    private String eventFormat;  // IN_PERSON, ONLINE, HYBRID

    //@Column(name = "virtual_meeting_link", length = 500)
    //private String virtualMeetingLink;  // For ONLINE/HYBRID events

    @Embedded
    private VirtualDetails virtualDetails;

    // ========================================
    // ORGANIZER SNAPSHOT (for PDF)
    // ========================================

    @Column(name = "organizer_name", nullable = false, length = 255)
    private String organizerName;

    @Column(name = "organizer_email", length = 255)
    private String organizerEmail;

    @Column(name = "organizer_phone", length = 50)
    private String organizerPhone;

    // ========================================
    // BOOKED TICKETS
    // ========================================

    @Column(name = "booked_tickets", columnDefinition = "jsonb", nullable = false)
    @Convert(converter = BookedTicketsJsonConverter.class)
    private List<BookedTicket> bookedTickets = new ArrayList<>();

    // ========================================
    // PRICING
    // ========================================

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    // ========================================
    // BOOKING REFERENCE
    // ========================================
    // Short readable code for easy reference (e.g., "EVT-2025-001234")

    @Column(name = "booking_reference", unique = true, nullable = false, length = 50)
    private String bookingReference;

    // ========================================
    // STATUS
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    // ========================================
    // AUDIT TIMESTAMPS
    // ========================================

    @Column(name = "booked_at", nullable = false)
    private LocalDateTime bookedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AccountEntity createdBy;

    // ========================================
    // LIFECYCLE CALLBACKS
    // ========================================

    @PrePersist
    protected void onCreate() {
        if (bookedAt == null) {
            bookedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    // ========================================
    // BUSINESS LOGIC METHODS
    // ========================================

    /**
     * Get total number of tickets in this booking
     */
    public int getTotalTicketCount() {
        return bookedTickets != null ? bookedTickets.size() : 0;
    }

    /**
     * Check if all tickets have been used (checked in)
     */
    public boolean areAllTicketsUsed() {
        if (bookedTickets == null || bookedTickets.isEmpty()) {
            return false;
        }
        return bookedTickets.stream()
                .allMatch(ticket -> ticket.getCheckedIn() != null && ticket.getCheckedIn());
    }

    /**
     * Get count of checked-in tickets
     */
    public long getCheckedInCount() {
        if (bookedTickets == null) {
            return 0;
        }
        return bookedTickets.stream()
                .filter(ticket -> ticket.getCheckedIn() != null && ticket.getCheckedIn())
                .count();
    }

    // ========================================
    // NESTED CLASS FOR JSON STORAGE
    // ========================================

    /**
     * Represents an individual booked ticket with complete details
     * Contains all information needed for PDF ticket generation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedTicket {

        // ========================================
        // TICKET IDENTIFIERS
        // ========================================

        private UUID ticketInstanceId;         // Unique ID for this ticket instance
        private UUID ticketTypeId;             // Reference to TicketEntity
        private String ticketTypeName;         // e.g., "VIP", "General Admission"
        private String ticketSeries;           // e.g., "VIP-0001", "GENER-0042"
        private BigDecimal price;
        private String qrCode;                 // Unique QR code for check-in
        private String attendanceMode;         // IN_PERSON, ONLINE (from ticket type)

        // ========================================
        // ATTENDEE INFORMATION
        // ========================================

        private String attendeeName;
        private String attendeeEmail;
        private String attendeePhone;

        // ========================================
        // BUYER INFORMATION
        // ========================================

        private String buyerName;              // Person who purchased
        private String buyerEmail;

        // ========================================
        // CHECK-IN DETAILS
        // ========================================

        private Boolean checkedIn;
        private ZonedDateTime checkedInAt;
        private String checkedInBy;            // Staff username
        private String checkInLocation;        // Gate/entrance name

        // ========================================
        // TICKET STATUS & VALIDITY
        // ========================================

        private TicketInstanceStatus status;   // ACTIVE, USED, CANCELLED
        private ZonedDateTime validFrom;       // Ticket valid from this time
        private ZonedDateTime validUntil;      // Ticket valid until this time
    }
}