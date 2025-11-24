package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.SessionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "event_sessions", indexes = {
        @Index(name = "idx_session_event", columnList = "event_id"),
        @Index(name = "idx_session_date", columnList = "session_date"),
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "idx_session_event_date", columnList = "event_id, session_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationship to parent event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    // Session details
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Session numbering
    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;  // 1, 2, 3, 4...

    // Capacity management
    @Column(name = "capacity")
    private Integer capacity;  // Max attendees for this session

    @Column(name = "booked_spots")
    @Builder.Default
    private Integer bookedSpots = 0;  // Current bookings

    @Column(name = "available_spots")
    private Integer availableSpots;  // capacity - bookedSpots

    // Session status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    // Additional info
    @Column(columnDefinition = "TEXT")
    private String notes;  // Optional session-specific notes

    // Soft delete
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate available spots based on capacity and bookings
     */
    public void calculateAvailableSpots() {
        if (capacity != null) {
            this.availableSpots = capacity - (bookedSpots != null ? bookedSpots : 0);
        }
    }

    /**
     * Check if session is full
     */
    public boolean isFull() {
        return availableSpots != null && availableSpots <= 0;
    }

    /**
     * Increment booked spots and recalculate available spots
     */
    public void incrementBookedSpots() {
        this.bookedSpots = (this.bookedSpots != null ? this.bookedSpots : 0) + 1;
        calculateAvailableSpots();
    }

    /**
     * Decrement booked spots (for cancellations)
     */
    public void decrementBookedSpots() {
        if (this.bookedSpots != null && this.bookedSpots > 0) {
            this.bookedSpots--;
            calculateAvailableSpots();
        }
    }
}