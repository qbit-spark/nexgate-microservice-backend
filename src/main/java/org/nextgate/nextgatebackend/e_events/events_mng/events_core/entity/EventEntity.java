package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.MediaJsonConverter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "events", indexes = {
        // Basic lookups
        @Index(name = "idx_event_slug", columnList = "slug"),
        @Index(name = "idx_event_status", columnList = "status"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_format", columnList = "event_format"),
        @Index(name = "idx_event_visibility", columnList = "visibility"),

        // Foreign key indexes
        @Index(name = "idx_event_organizer", columnList = "organizer_id"),
        @Index(name = "idx_event_category", columnList = "category_id"),

        // Date queries
        @Index(name = "idx_event_dates", columnList = "start_date_time, end_date_time"),
        @Index(name = "idx_event_start_date", columnList = "start_date_time"),

        // Soft delete
        @Index(name = "idx_event_is_deleted", columnList = "is_deleted"),

        // ===== PERFORMANCE-CRITICAL COMPOSITE INDEXES =====

        // Duplicate detection (MOST IMPORTANT)
        @Index(name = "idx_event_duplicate_check",
                columnList = "status, is_deleted, start_date_time"),

        // Organizer queries (dashboard, my events)
        @Index(name = "idx_event_organizer_status",
                columnList = "organizer_id, status, is_deleted, start_date_time"),

        // Public listings (homepage, search)
        @Index(name = "idx_event_public_listing",
                columnList = "status, visibility, is_deleted, start_date_time"),

        // Category browsing
        @Index(name = "idx_event_category_status",
                columnList = "category_id, status, is_deleted, start_date_time")
})
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

    // Event classification
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType; // ONE_TIME, MULTI_DAY, RECURRING

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
    @Column(name = "start_date_time", nullable = false)
    private ZonedDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private ZonedDateTime endDateTime;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // For MULTI_DAY events - Keep as separate table
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventDayEntity> days = new ArrayList<>();

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
    @JoinTable(
            name = "event_products",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @Builder.Default
    private List<ProductEntity> linkedProducts = new ArrayList<>();

    // Linked shops - Relationship with ShopEntity
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_shops",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "shop_id")
    )
    @Builder.Default
    private List<ShopEntity> linkedShops = new ArrayList<>();

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
        return Arrays.stream(EventCreationStage.values())
                .filter(EventCreationStage::isRequired)
                .filter(stage -> !isStageCompleted(stage))
                .collect(Collectors.toList());
    }

    public int getCompletionPercentage() {
        long totalRequired = Arrays.stream(EventCreationStage.values())
                .filter(EventCreationStage::isRequired)
                .count();

        long completed = completedStages.stream()
                .map(EventCreationStage::valueOf)
                .filter(EventCreationStage::isRequired)
                .count();

        return (int) ((completed * 100) / totalRequired);
    }

}