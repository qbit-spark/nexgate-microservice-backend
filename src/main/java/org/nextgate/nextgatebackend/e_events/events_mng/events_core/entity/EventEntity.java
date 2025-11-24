package org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventType;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.MediaJsonConverter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.RecurrenceJsonConverter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_slug", columnList = "slug"),
        @Index(name = "idx_event_status", columnList = "status"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_organizer", columnList = "organizer_id"),
        @Index(name = "idx_event_category", columnList = "category_id"),
        @Index(name = "idx_event_dates", columnList = "start_date_time, end_date_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Category relationship
    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(name = "category_name")
    private String categoryName;

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

    // For RECURRING events - Make this JSONB
    @Column(name = "recurrence", columnDefinition = "jsonb")
    @Convert(converter = RecurrenceJsonConverter.class)
    private Recurrence recurrence;

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

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Soft delete
    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
}