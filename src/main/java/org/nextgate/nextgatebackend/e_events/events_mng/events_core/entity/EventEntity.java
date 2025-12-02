package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity;

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
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.MediaJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeys;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.CheckInWindowStrategy.HOURS_BEFORE;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_slug", columnList = "slug"), @Index(name = "idx_event_status", columnList = "status"), @Index(name = "idx_event_format", columnList = "event_format"), @Index(name = "idx_event_visibility", columnList = "eventVisibility"), @Index(name = "idx_event_organizer", columnList = "organizer_id"), @Index(name = "idx_event_category", columnList = "category_id"), @Index(name = "idx_event_dates", columnList = "start_date_time, end_date_time"), @Index(name = "idx_event_start_date", columnList = "start_date_time"), @Index(name = "idx_event_is_deleted", columnList = "is_deleted"), @Index(name = "idx_event_duplicate_check", columnList = "status, is_deleted, start_date_time"), @Index(name = "idx_event_organizer_status", columnList = "organizer_id, status, is_deleted, start_date_time"), @Index(name = "idx_event_public_listing", columnList = "status, eventVisibility, is_deleted, start_date_time"), @Index(name = "idx_event_category_status", columnList = "category_id, status, is_deleted, start_date_time")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Category relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", nullable = false)
    private EventsCategoryEntity category;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_visibility", nullable = false, length = 20)
    private EventVisibility eventVisibility;


    @Enumerated(EnumType.STRING)
    @Column(name = "event_format", nullable = false, length = 20)
    private EventFormat eventFormat; // IN_PERSON, ONLINE, HYBRID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    // Venue details (for IN_PERSON and HYBRID)
    @Embedded
    private Venue venue;

    // Virtual details (for ONLINE and HYBRID)
    @Embedded
    private VirtualDetails virtualDetails;

    // Schedule - Keep as direct columns for querying
    @Column(name = "start_date_time")
    private ZonedDateTime startDateTime;

    @Column(name = "end_date_time")
    private ZonedDateTime endDateTime;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // For MULTI_DAY events - Keep as separate table
    // FIXED: mappedBy must match the field name in EventDayEntity
    @OneToMany(mappedBy = "eventEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventDayEntity> days = new ArrayList<>();


     // CHECK-IN WINDOW CONFIGURATION

    /**
     * Check-in window strategy
     * Determines when attendees can check in
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "checkin_strategy", length = 20)
    @Builder.Default
    private CheckInWindowStrategy checkInStrategy = CheckInWindowStrategy.HOURS_BEFORE;


    // For HOURS_BEFORE strategy


    /**
     * Hours before event to allow check-in
     * Used when checkInStrategy = HOURS_BEFORE
     * Default: 2 hours
     */
    @Column(name = "early_checkin_hours")
    @Builder.Default
    private Integer earlyCheckInHours = 3;

    /**
     * Minutes after event end to allow check-in (grace period)
     * Used when checkInStrategy = HOURS_BEFORE
     * Default: 30 minutes
     */
    @Column(name = "late_checkin_minutes")
    @Builder.Default
    private Integer lateCheckInMinutes = 30;


    // For SPECIFIC_TIME strategy

    /**
     * Specific time when check-in opens each day
     * Used when checkInStrategy = SPECIFIC_TIME
     * Format: "HH:mm" (24-hour format)
     * Example: "08:00" means gates open at 8 AM
     */
    @Column(name = "checkin_opens_at", length = 5)
    private String checkInOpensAt;

    /**
     * Specific time when check-in closes each day
     * Used when checkInStrategy = SPECIFIC_TIME
     * Format: "HH:mm" (24-hour format)
     * Example: "23:00" means gates close at 11 PM
     */
    @Column(name = "checkin_closes_at", length = 5)
    private String checkInClosesAt;

    // Note: ALL_DAY and EXACT_TIME strategies don't need additional fields

    // Organizer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", referencedColumnName = "id", nullable = false)
    private AccountEntity organizer;

    // Media
    @Column(name = "media", columnDefinition = "jsonb")
    @Convert(converter = MediaJsonConverter.class)
    private Media media;

    // Linked products - Relationship with ProductEntity
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "event_products", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "product_id"))
    @Builder.Default
    private List<ProductEntity> linkedProducts = new ArrayList<>();

    // Linked shops - Relationship with ShopEntity
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "event_shops", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "shop_id"))
    @Builder.Default
    private List<ShopEntity> linkedShops = new ArrayList<>();

    // Tickets
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TicketEntity> tickets = new ArrayList<>();

    // Audit fields
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    // Audit fields - User associations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = false, updatable = false)
    private AccountEntity createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id")
    private AccountEntity updatedBy;

    // Soft delete
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", referencedColumnName = "id")
    private AccountEntity deletedBy;

    @Column(name = "current_stage")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventCreationStage currentStage = EventCreationStage.BASIC_INFO;

    @Column(name = "completed_stages", columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    @Builder.Default
    private List<String> completedStages = new ArrayList<>();

    @Type(JsonBinaryType.class)  // ← This does ALL the work!
    @Column(name = "rsa_keys", columnDefinition = "jsonb")
    private RSAKeys rsaKeys;



    // Helper methods
    public boolean isStageCompleted(EventCreationStage stage) {
        return completedStages.contains(stage.name());
    }

    public void markStageCompleted(EventCreationStage stage) {
        if (!completedStages.contains(stage.name())) {
            completedStages.add(stage.name());
        }
    }

    public boolean canPublish() {
        // Check if all required stages are completed
        for (EventCreationStage stage : EventCreationStage.values()) {
            if (stage.isRequired() && !isStageCompleted(stage)) {
                return false;
            }
        }
        return true;
    }

    public List<EventCreationStage> getRemainingRequiredStages() {
        return Arrays.stream(EventCreationStage.values()).filter(EventCreationStage::isRequired).filter(stage -> !isStageCompleted(stage)).collect(Collectors.toList());
    }

    public int getCompletionPercentage() {
        long totalRequired = Arrays.stream(EventCreationStage.values()).filter(EventCreationStage::isRequired).count();

        long completed = completedStages.stream().map(EventCreationStage::valueOf).filter(EventCreationStage::isRequired).count();

        return (int) ((completed * 100) / totalRequired);
    }

    /**
     * Check if event has active RSA keys
     */
    public boolean hasActiveKeys() {
        return rsaKeys != null && rsaKeys.isActive();
    }

}