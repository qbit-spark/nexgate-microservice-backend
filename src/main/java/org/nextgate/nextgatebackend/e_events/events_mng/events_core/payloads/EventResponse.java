package org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.*;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.AttendanceMode;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Complete Event Response DTO
 * Returns full event details to the client
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {

    private UUID id;
    private String title;
    private String slug;
    private String description;

    // Category info
    private CategoryInfo category;

    // Event classification
    private EventType eventType;
    private EventFormat eventFormat;
    private EventVisibility eventVisibility;
    private EventStatus status;

    // Schedule
    private ScheduleInfo schedule;

    // Location (based on format)
    private VenueInfo venue;
    private VirtualDetailsInfo virtualDetails;

    // Media
    private MediaInfo media;

    // Linked entities
    @Builder.Default
    private List<LinkedProductInfo> linkedProducts = new ArrayList<>();

    @Builder.Default
    private List<LinkedShopInfo> linkedShops = new ArrayList<>();

    // Tickets
    @Builder.Default
    private List<TicketSummaryInfo> tickets = new ArrayList<>();

    // Organizer info
    private OrganizerInfo organizer;

    // Progress tracking
    private EventCreationStage currentStage;
    @Builder.Default
    private List<String> completedStages = new ArrayList<>();
    private Integer completionPercentage;
    private Boolean canPublish;

    // Audit info
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // ========== NESTED DTOs ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private UUID categoryId;
        private String categoryName;
        private String categorySlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleInfo {
        private ZonedDateTime startDateTime;
        private ZonedDateTime endDateTime;
        private String timezone;

        // For MULTI_DAY events
        @Builder.Default
        private List<EventDayInfo> days = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDayInfo {
        private UUID id;
        private String date;  // LocalDate as string
        private String startTime;  // LocalTime as string
        private String endTime;  // LocalTime as string
        private String description;
        private Integer dayOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueInfo {
        private String name;
        private String address;
        private CoordinatesInfo coordinates;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordinatesInfo {
        private String latitude;
        private String longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VirtualDetailsInfo {
        private String meetingLink;
        private String meetingId;
        private String passcode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaInfo {
        private String banner;
        private String thumbnail;
        @Builder.Default
        private List<String> gallery = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedProductInfo {
        private UUID productId;
        private String productName;
        private String productSlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedShopInfo {
        private UUID shopId;
        private String shopName;
        private String shopSlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizerInfo {
        private UUID organizerId;
        private String organizerName;
        private String organizerUsername;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSummaryInfo {
        private UUID id;
        private String name;
        private BigDecimal price;
        private String currency;
        private Integer totalTickets;
        private Integer ticketsSold;
        private Integer ticketsAvailable;
        private Boolean isUnlimited;
        private Boolean isSoldOut;
        private AttendanceMode attendanceMode;
        private TicketStatus status;
        private Boolean isOnSale;
    }
}