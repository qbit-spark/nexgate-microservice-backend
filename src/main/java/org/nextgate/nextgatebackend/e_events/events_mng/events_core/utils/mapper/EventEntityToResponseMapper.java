package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.mapper;


import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventDayEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.EventResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper to convert EventEntity to EventResponse DTO
 */
@Component
public class EventEntityToResponseMapper {

    /**
     * Convert EventEntity to EventResponse
     */
    public EventResponse toResponse(EventEntity event) {
        if (event == null) {
            return null;
        }

        EventResponse.EventResponseBuilder builder = EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .description(event.getDescription())
                .eventFormat(event.getEventFormat())
                .eventVisibility(event.getEventVisibility())
                .status(event.getStatus())
                .currentStage(event.getCurrentStage())
                .completedStages(event.getCompletedStages())
                .completionPercentage(event.getCompletionPercentage())
                .canPublish(event.canPublish())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt());

        // Category
        if (event.getCategory() != null) {
            builder.category(EventResponse.CategoryInfo.builder()
                    .categoryId(event.getCategory().getCategoryId())
                    .categoryName(event.getCategory().getName())
                    .categorySlug(event.getCategory().getSlug())
                    .build());
        }

        // Schedule
        EventResponse.ScheduleInfo.ScheduleInfoBuilder scheduleBuilder = EventResponse.ScheduleInfo.builder()
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .timezone(event.getTimezone());

        // Add days for MULTI_DAY events
        if (event.getDays() != null && !event.getDays().isEmpty()) {
            scheduleBuilder.days(event.getDays().stream()
                    .map(this::mapEventDay)
                    .collect(Collectors.toList()));
        }

        builder.schedule(scheduleBuilder.build());

        // Venue (for IN_PERSON and HYBRID)
        if (event.getVenue() != null) {
            EventResponse.VenueInfo.VenueInfoBuilder venueBuilder = EventResponse.VenueInfo.builder()
                    .name(event.getVenue().getName())
                    .address(event.getVenue().getAddress());

            if (event.getVenue().getCoordinates() != null) {
                venueBuilder.coordinates(EventResponse.CoordinatesInfo.builder()
                        .latitude(event.getVenue().getCoordinates().getLatitude() != null
                                ? event.getVenue().getCoordinates().getLatitude().toString()
                                : null)
                        .longitude(event.getVenue().getCoordinates().getLongitude() != null
                                ? event.getVenue().getCoordinates().getLongitude().toString()
                                : null)
                        .build());
            }

            builder.venue(venueBuilder.build());
        }

        // Virtual details (for ONLINE and HYBRID)
        if (event.getVirtualDetails() != null) {
            builder.virtualDetails(EventResponse.VirtualDetailsInfo.builder()
                    .meetingLink(event.getVirtualDetails().getMeetingLink())
                    .meetingId(event.getVirtualDetails().getMeetingId())
                    .passcode(event.getVirtualDetails().getPasscode())
                    .build());
        }

        // Media
        if (event.getMedia() != null) {
            builder.media(EventResponse.MediaInfo.builder()
                    .banner(event.getMedia().getBanner())
                    .thumbnail(event.getMedia().getThumbnail())
                    .gallery(event.getMedia().getGallery())
                    .build());
        }

        // Linked products
        if (event.getLinkedProducts() != null && !event.getLinkedProducts().isEmpty()) {
            builder.linkedProducts(event.getLinkedProducts().stream()
                    .map(product -> EventResponse.LinkedProductInfo.builder()
                            .productId(product.getProductId())
                            .productName(product.getProductName())
                            .productSlug(product.getProductSlug())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Linked shops
        if (event.getLinkedShops() != null && !event.getLinkedShops().isEmpty()) {
            builder.linkedShops(event.getLinkedShops().stream()
                    .map(shop -> EventResponse.LinkedShopInfo.builder()
                            .shopId(shop.getShopId())
                            .shopName(shop.getShopName())
                            .shopSlug(shop.getShopSlug())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Organizer
        if (event.getOrganizer() != null) {
            builder.organizer(EventResponse.OrganizerInfo.builder()
                    .organizerId(event.getOrganizer().getId())
                    .organizerName(event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName())
                    .organizerUsername(event.getOrganizer().getUserName())
                    .build());
        }

        // Audit info
        if (event.getCreatedBy() != null) {
            builder.createdBy(event.getCreatedBy().getUserName());
        }
        if (event.getUpdatedBy() != null) {
            builder.updatedBy(event.getUpdatedBy().getUserName());
        }

        return builder.build();
    }

    /**
     * Map EventDayEntity to EventDayInfo
     */
    private EventResponse.EventDayInfo mapEventDay(EventDayEntity day) {
        return EventResponse.EventDayInfo.builder()
                .id(day.getId())
                .date(day.getDate().toString())
                .startTime(day.getStartTime().toString())
                .endTime(day.getEndTime().toString())
                .description(day.getDescription())
                .dayOrder(day.getDayOrder())
                .build();
    }
}

