package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity;


import jakarta.persistence.*;
import lombok.*;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.TicketInstanceStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils.BookedTicketsJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils.VirtualDetailsJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.VirtualDetails;
import org.springframework.data.annotation.CreatedBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a confirmed event booking order
 * Created after successful payment of an EventCheckoutSession
 * Contains booked tickets with QR codes for event entry
 *
 * Supports multi-day events with per-day check-in tracking
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


    @Column(name = "virtual_details", columnDefinition = "jsonb")
    @Convert(converter = VirtualDetailsJsonConverter.class)
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
     * Get the total number of tickets in this booking
     */
    public int getTotalTicketCount() {
        return bookedTickets != null ? bookedTickets.size() : 0;
    }

    /**
     * Check if all tickets have been used (checked in at least once)
     */
    public boolean areAllTicketsUsed() {
        if (bookedTickets == null || bookedTickets.isEmpty()) {
            return false;
        }
        return bookedTickets.stream()
                .allMatch(BookedTicket::hasAnyCheckIn);
    }

    /**
     * Get count of tickets with at least one check-in
     */
    public long getCheckedInCount() {
        if (bookedTickets == null) {
            return 0;
        }
        return bookedTickets.stream()
                .filter(BookedTicket::hasAnyCheckIn)
                .count();
    }

    /**
     * Get total number of check-ins across all tickets
     */
    public long getTotalCheckInCount() {
        if (bookedTickets == null) {
            return 0;
        }
        return bookedTickets.stream()
                .mapToLong(BookedTicket::getCheckInCount)
                .sum();
    }

    /**
     * Get attendance rate (percentage of tickets with check-ins)
     */
    public double getAttendanceRate() {
        int total = getTotalTicketCount();
        if (total == 0) {
            return 0.0;
        }
        return (getCheckedInCount() * 100.0) / total;
    }

    // ========================================
    // NESTED CLASS FOR JSON STORAGE
    // ========================================

    /**
     * Represents an individual booked ticket with complete details
     * Contains all information needed for PDF ticket generation
     * Supports multi-day events with multiple check-ins
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

        private String jwtToken;               // ✅ Signed JWT for validation
        private String qrCode;                 // ✅ Backward compatibility / simple reference
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
        // CHECK-IN DETAILS (MULTI-DAY SUPPORT)
        // ========================================

        /**
         * List of check-in records
         * Supports multiple check-ins for multi-day events
         *
         * Example for 3-day festival:
         * - Day 1: Check-in at 18:00
         * - Day 2: Check-in at 14:00
         * - Day 3: Check-in at 15:00
         */
        @Builder.Default
        private List<CheckInRecord> checkIns = new ArrayList<>();

        // ========================================
        // TICKET STATUS & VALIDITY
        // ========================================

        private TicketInstanceStatus status;   // ACTIVE, USED, CANCELLED
        private ZonedDateTime validFrom;       // Ticket valid from this time
        private ZonedDateTime validUntil;      // Ticket valid until this time

        // ========================================
        // HELPER METHODS FOR CHECK-IN
        // ========================================

        /**
         * Check if ticket has been checked in for a specific day
         *
         * @param dayName The day name (e.g., "Day 1", "Day 2 - Saturday")
         * @return true if checked in for this day
         */
        public boolean isCheckedInForDay(String dayName) {
            if (checkIns == null || dayName == null) {
                return false;
            }
            return checkIns.stream()
                    .anyMatch(checkIn -> dayName.equals(checkIn.getDayName()));
        }

        /**
         * Check if ticket has any check-in records
         *
         * @return true if ticket has been checked in at least once
         */
        public boolean hasAnyCheckIn() {
            return checkIns != null && !checkIns.isEmpty();
        }

        /**
         * Get the most recent check-in record
         *
         * @return Last check-in record or null if none
         */
        public CheckInRecord getLastCheckIn() {
            if (checkIns == null || checkIns.isEmpty()) {
                return null;
            }
            return checkIns.getLast();
        }

        /**
         * Get total number of check-ins
         *
         * @return Count of check-ins
         */
        public int getCheckInCount() {
            return checkIns != null ? checkIns.size() : 0;
        }

        /**
         * Get all check-ins for a specific day
         *
         * @param dayName The day name
         * @return List of check-ins for this day
         */
        public List<CheckInRecord> getCheckInsForDay(String dayName) {
            if (checkIns == null || dayName == null) {
                return new ArrayList<>();
            }
            return checkIns.stream()
                    .filter(checkIn -> dayName.equals(checkIn.getDayName()))
                    .toList();
        }

        /**
         * Check if checked in today (by date, not by day name)
         * Useful for same-day re-entry prevention
         *
         * @return true if checked in today
         */
        public boolean isCheckedInToday() {
            if (checkIns == null) {
                return false;
            }
            LocalDate today = LocalDate.now();
            return checkIns.stream()
                    .anyMatch(checkIn -> checkIn.getCheckInTime().toLocalDate().equals(today));
        }

        // ========================================
        // NESTED CLASS: CHECK-IN RECORD
        // ========================================

        /**
         * Represents a single check-in event
         * Multiple check-ins allowed for multi-day events
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CheckInRecord {

            /**
             * When check-in occurred
             */
            private ZonedDateTime checkInTime;

            /**
             * Where check-in occurred (e.g., "Gate A", "VIP Entrance")
             */
            private String checkInLocation;

            /**
             * Who processed the check-in (scanner name)
             */
            private String checkedInBy;

            /**
             * Which event day this check-in is for
             * Example: "Day 1", "Day 2 - Saturday"
             * Must match dayName from eventSchedules in JWT
             */
            private String dayName;

            /**
             * Scanner ID that performed the check-in
             */
            private String scannerId;

            /**
             * Check-in method (default: QR_SCAN)
             */
            @Builder.Default
            private String checkInMethod = "QR_SCAN";
        }
    }
}