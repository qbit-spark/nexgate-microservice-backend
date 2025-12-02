package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Attendee Entity
 *
 * Represents a person who attends events.
 * Can be linked to a system user (AccountEntity) OR exist independently.
 *
 * Use Cases:
 * - Registered user buys ticket for themselves (has AccountEntity)
 * - User buys ticket for friend (no AccountEntity)
 * - Guest checkout (no AccountEntity)
 * - Track attendance history across events
 * - Loyalty programs and marketing
 */
@Entity
@Table(
        name = "event_attendees",
        indexes = {
                @Index(name = "idx_attendee_email", columnList = "email"),
                @Index(name = "idx_attendee_account", columnList = "account_id"),
                @Index(name = "idx_attendee_phone", columnList = "phone")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // ========================================
    // IDENTITY
    // ========================================

    /**
     * Link to user account (if attendee is a registered user)
     *
     * NULL = Guest/Non-user (friend's ticket, guest checkout)
     * NOT NULL = Registered user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    /**
     * Email address (unique identifier)
     * Used to track attendee across events even without account
     */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Full name
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Phone number (optional)
     */
    @Column(name = "phone", length = 50)
    private String phone;

    // ========================================
    // ATTENDANCE HISTORY
    // ========================================

    /**
     * List of event IDs attended (with check-in)
     * Stored as JSON array of UUIDs
     */
    @Type(JsonBinaryType.class)
    @Column(name = "events_attended", columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> eventsAttended = new ArrayList<>();

    /**
     * List of check-in records across all events
     * Lightweight summary for quick queries
     */
    @Type(JsonBinaryType.class)
    @Column(name = "check_in_history", columnDefinition = "jsonb")
    @Builder.Default
    private List<CheckInSummary> checkInHistory = new ArrayList<>();

    /**
     * Total events attended (checked in)
     */
    @Column(name = "total_events_attended")
    @Builder.Default
    private Integer totalEventsAttended = 0;

    /**
     * Total tickets booked (may not have attended)
     */
    @Column(name = "total_tickets_booked")
    @Builder.Default
    private Integer totalTicketsBooked = 0;

    /**
     * Last event attended
     */
    @Column(name = "last_attended_at")
    private Instant lastAttendedAt;

    /**
     * First event attended
     */
    @Column(name = "first_attended_at")
    private Instant firstAttendedAt;

    // ========================================
    // PREFERENCES & SEGMENTATION
    // ========================================

    /**
     * Preferred event categories
     * Automatically inferred from attendance history
     * Example: ["TECHNOLOGY", "MUSIC", "BUSINESS"]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "preferred_categories", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> preferredCategories = new ArrayList<>();

    /**
     * Loyalty points earned across all events
     */
    @Column(name = "loyalty_points")
    @Builder.Default
    private Integer loyaltyPoints = 0;

    /**
     * Marketing opt-in status
     */
    @Column(name = "marketing_opted_in")
    @Builder.Default
    private Boolean marketingOptedIn = true;

    // ========================================
    // AUDIT
    // ========================================

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ========================================
    // NESTED CLASS: CHECK-IN SUMMARY
    // ========================================

    /**
     * Lightweight check-in record for quick queries
     * Full check-in details are in BookedTicket.checkIns
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckInSummary {

        /**
         * Event ID
         */
        private UUID eventId;

        /**
         * Event name (snapshot)
         */
        private String eventName;

        /**
         * Ticket instance ID
         */
        private UUID ticketInstanceId;

        /**
         * When checked in
         */
        private Instant checkInTime;

        /**
         * Which day (for multi-day events)
         */
        private String dayName;

        /**
         * Event category (for preference tracking)
         */
        private String eventCategory;
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Record a new check-in
     */
    public void recordCheckIn(
            UUID eventId,
            String eventName,
            UUID ticketInstanceId,
            String dayName,
            String eventCategory) {

        // Add to check in history
        CheckInSummary summary = CheckInSummary.builder()
                .eventId(eventId)
                .eventName(eventName)
                .ticketInstanceId(ticketInstanceId)
                .checkInTime(Instant.now())
                .dayName(dayName)
                .eventCategory(eventCategory)
                .build();

        if (this.checkInHistory == null) {
            this.checkInHistory = new ArrayList<>();
        }
        this.checkInHistory.add(summary);

        // Update events attended (if not already in list)
        if (this.eventsAttended == null) {
            this.eventsAttended = new ArrayList<>();
        }
        if (!this.eventsAttended.contains(eventId)) {
            this.eventsAttended.add(eventId);
            this.totalEventsAttended++;
        }

        // Update timestamps
        this.lastAttendedAt = Instant.now();
        if (this.firstAttendedAt == null) {
            this.firstAttendedAt = Instant.now();
        }

        // Update preferred categories
        if (eventCategory != null && !eventCategory.isBlank()) {
            if (this.preferredCategories == null) {
                this.preferredCategories = new ArrayList<>();
            }
            if (!this.preferredCategories.contains(eventCategory)) {
                this.preferredCategories.add(eventCategory);
            }
        }

        // Award loyalty points (10 points per check-in)
        this.loyaltyPoints += 10;
    }

    /**
     * Record a ticket booking (before check-in)
     */
    public void recordBooking() {
        this.totalTicketsBooked++;
    }

    /**
     * Check if attendee has attended a specific event
     */
    public boolean hasAttendedEvent(UUID eventId) {
        return eventsAttended != null && eventsAttended.contains(eventId);
    }

    /**
     * Check if attendee is a registered user
     */
    public boolean isRegisteredUser() {
        return account != null;
    }

    /**
     * Get attendance rate (% of booked tickets actually attended)
     */
    public double getAttendanceRate() {
        if (totalTicketsBooked == 0) {
            return 0.0;
        }
        return (totalEventsAttended * 100.0) / totalTicketsBooked;
    }
}