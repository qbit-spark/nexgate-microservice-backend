package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketValidityType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket_types",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ticket_name_event_mode",
                        columnNames = {"event_id", "name", "attendance_mode"}
                )
        },
        indexes = {
                @Index(name = "idx_ticket_event", columnList = "event_id"),
                @Index(name = "idx_ticket_status", columnList = "status"),
                @Index(name = "idx_ticket_event_status", columnList = "event_id, status, is_deleted")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // ========== RELATIONSHIP ==========
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    // ========== BASIC INFO ==========
    @Column(name = "name", nullable = false, length = 100)
    private String name; // "VIP Pass", "Early Bird", "Student Discount"

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price; // Can be 0.00 for free tickets

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";

    // ========== QUANTITY MANAGEMENT ==========
    @Column(name = "total_quantity")
    private Integer totalQuantity; // null if unlimited

    @Column(name = "quantity_sold", nullable = false)
    @Builder.Default
    private Integer quantitySold = 0;

    @Column(name = "is_unlimited", nullable = false)
    @Builder.Default
    private Boolean isUnlimited = false;

    // ========== SALES PERIOD (Optional) ==========
    @Column(name = "sales_start_date_time")
    private ZonedDateTime salesStartDateTime;

    @Column(name = "sales_end_date_time")
    private ZonedDateTime salesEndDateTime;

    // ========== PURCHASE LIMITS ==========
    @Column(name = "min_quantity_per_order", nullable = false)
    @Builder.Default
    private Integer minQuantityPerOrder = 1;

    @Column(name = "max_quantity_per_order")
    private Integer maxQuantityPerOrder; // null = no limit per order

    @Column(name = "max_quantity_per_user")
    private Integer maxQuantityPerUser; // null = no limit per user

    // ========== TICKET VALIDITY ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "valid_until_type", nullable = false, length = 30)
    @Builder.Default
    private TicketValidityType validUntilType = TicketValidityType.EVENT_END;

    @Column(name = "custom_valid_until")
    private ZonedDateTime customValidUntil; // Only used when validUntilType = CUSTOM

    // ========== FOR HYBRID EVENTS ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_mode", length = 20)
    private AttendanceMode attendanceMode; // IN_PERSON, ONLINE (required for HYBRID events)

    // ========== INCLUSIVE ITEMS ==========
    @Column(name = "inclusive_items", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    @Builder.Default
    private List<String> inclusiveItems = new ArrayList<>();

    // ========== VISIBILITY ==========
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false; // For invite-only tickets

    // ========== STATUS ==========
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;

    // ========== SOFT DELETE ==========
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private AccountEntity deletedBy;

    // ========== AUDIT FIELDS ==========
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private AccountEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AccountEntity updatedBy;

    // ========== HELPER METHODS ==========

    /**
     * Get remaining quantity (calculated field)
     */
    public Integer getQuantityRemaining() {
        if (isUnlimited) {
            return null; // Unlimited tickets
        }
        return totalQuantity - quantitySold;
    }

    /**
     * Get quantity available for purchase
     */
    public Integer getQuantityAvailable() {
        return getQuantityRemaining();
    }

    /**
     * Check if ticket is sold out
     */
    public boolean isSoldOut() {
        if (isUnlimited) {
            return false;
        }
        return quantitySold >= totalQuantity;
    }

    /**
     * Check if ticket is currently on sale (based on sales period)
     */
    public boolean isOnSale() {
        if (status != TicketStatus.ACTIVE || isDeleted) {
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now();

        // Check sales start date
        if (salesStartDateTime != null && now.isBefore(salesStartDateTime)) {
            return false;
        }

        // Check sales end date
        if (salesEndDateTime != null && now.isAfter(salesEndDateTime)) {
            return false;
        }

        return true;
    }

    /**
     * Generate seat number prefix from ticket name
     * Max 5 characters, uppercase, no spaces/special chars
     */
    public String generateSeatPrefix() {
        if (name == null || name.isBlank()) {
            return "TKT";
        }

        String cleaned = name.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .replaceAll("\\s+", "");

        // Take first 5 characters (or less if shorter)
        return cleaned.length() <= 5 ? cleaned : cleaned.substring(0, 5);
    }

    /**
     * Check if ticket can be deleted
     * Can delete if no tickets sold, otherwise must close
     */
    public boolean canBeDeleted() {
        return quantitySold == 0;
    }

    /**
     * Validate capacity can be reduced
     */
    public boolean canReduceCapacityTo(Integer newCapacity) {
        if (isUnlimited) {
            return false; // Cannot set capacity on unlimited tickets
        }
        return newCapacity >= quantitySold;
    }
}