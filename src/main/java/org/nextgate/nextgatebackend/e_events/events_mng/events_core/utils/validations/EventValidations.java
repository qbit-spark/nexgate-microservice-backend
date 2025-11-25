package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.category.repo.EventsCategoryRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.EventDayRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils.validations.TicketValidations;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.springframework.stereotype.Service;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventCreationStage;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.CreateEventRequest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class EventValidations {

    private final EventsRepo eventsRepo;
    private final EventsCategoryRepository eventsCategoryRepository;
    private final ShopRepo shopRepo;
    private final ProductRepo productRepo;
    private final TicketValidations ticketValidations;

    // ========================================================================
    // SOFT VALIDATIONS (For Drafts - Format checks only, no required fields)
    // ========================================================================

    /**
     * Soft validation for basic info - only validates format/length, not required fields
     */
    public void softValidateBasicInfo(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing soft validation for basic info");

        // Title format validation (if provided)
        if (request.getTitle() != null) {
            if (request.getTitle().length() < 3) {
                throw new EventValidationException("Title must be at least 3 characters", EventCreationStage.BASIC_INFO);
            }
            if (request.getTitle().length() > 200) {
                throw new EventValidationException("Title must not exceed 200 characters", EventCreationStage.BASIC_INFO);
            }
        }


        // Description length validation (if provided)
        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            throw new EventValidationException("Description must not exceed 5000 characters", EventCreationStage.BASIC_INFO);
        }

        // Category exists validation (if provided)
        if (request.getCategoryId() != null) {
            validateCategoryExists(request.getCategoryId());
        }
    }



    /**
     * Soft validation for schedule - only validates logic if fields are present
     */
    public void softValidateSchedule(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing soft validation for schedule");

        if (request.getSchedule() == null) {
            return; // Schedule isn't provided yet, that's fine for draft
        }

        // Validate days array if provided
        if (request.getSchedule().getDays() != null && !request.getSchedule().getDays().isEmpty()) {
            for (EventDayRequest day : request.getSchedule().getDays()) {
                // Validate date logic for each day
                if (day.getStartTime() != null && day.getEndTime() != null) {
                    if (day.getEndTime().isBefore(day.getStartTime())) {
                        throw new EventValidationException(
                                "End time must be after start time for day: " + day.getDate(),
                                EventCreationStage.SCHEDULE
                        );
                    }

                    if (day.getEndTime().equals(day.getStartTime())) {
                        throw new EventValidationException(
                                "Start and end time cannot be the same for day: " + day.getDate(),
                                EventCreationStage.SCHEDULE
                        );
                    }
                }
            }
        }

        // Validate timezone format (if provided)
        if (request.getSchedule().getTimezone() != null && !request.getSchedule().getTimezone().isBlank()) {
            validateTimezone(request.getSchedule().getTimezone());
        }
    }


    /**
     * Soft validation for location - only validates format if provided
     */
    public void softValidateLocation(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing soft validation for location");

        // Venue validation (if provided)
        if (request.getVenue() != null) {
            if (request.getVenue().getName() != null && request.getVenue().getName().length() > 200) {
                throw new EventValidationException(
                        "Venue name must not exceed 200 characters",
                        EventCreationStage.LOCATION_DETAILS
                );
            }
            if (request.getVenue().getAddress() != null && request.getVenue().getAddress().length() > 500) {
                throw new EventValidationException(
                        "Venue address must not exceed 500 characters",
                        EventCreationStage.LOCATION_DETAILS
                );
            }
        }

        // Virtual details validation (if provided)
        if (request.getVirtualDetails() != null) {
            if (request.getVirtualDetails().getMeetingLink() != null &&
                    request.getVirtualDetails().getMeetingLink().length() > 500) {
                throw new EventValidationException(
                        "Meeting link must not exceed 500 characters",
                        EventCreationStage.LOCATION_DETAILS
                );
            }
        }
    }

    /**
     * Soft validation for media - only validates format if provided
     */
    public void softValidateMedia(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing soft validation for media");

        if (request.getMedia() == null) {
            return; // Media is optional
        }

        if (request.getMedia().getBanner() != null && request.getMedia().getBanner().length() > 500) {
            throw new EventValidationException("Banner URL must not exceed 500 characters", EventCreationStage.MEDIA);
        }

        if (request.getMedia().getThumbnail() != null && request.getMedia().getThumbnail().length() > 500) {
            throw new EventValidationException("Thumbnail URL must not exceed 500 characters", EventCreationStage.MEDIA);
        }

        if (request.getMedia().getGallery() != null && request.getMedia().getGallery().size() > 20) {
            throw new EventValidationException("Gallery cannot have more than 20 images", EventCreationStage.MEDIA);
        }
    }

    // ========================================================================
    // HARD VALIDATIONS (Per Stage - All required fields must be present)
    // ========================================================================

    /**
     * Hard validation for basic info stage - all required fields must be present
     */
    public void hardValidateBasicInfo(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing hard validation for basic info");

        // Required: Title
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new EventValidationException("Event title is required", EventCreationStage.BASIC_INFO);
        }
        if (request.getTitle().length() < 3) {
            throw new EventValidationException("Title must be at least 3 characters", EventCreationStage.BASIC_INFO);
        }
        if (request.getTitle().length() > 200) {
            throw new EventValidationException("Title must not exceed 200 characters", EventCreationStage.BASIC_INFO);
        }


        // Description validation (if provided)
        if (request.getDescription() != null && request.getDescription().length() > 5000) {
            throw new EventValidationException("Description must not exceed 5000 characters", EventCreationStage.BASIC_INFO);
        }

        // Required: Category
        if (request.getCategoryId() == null) {
            throw new EventValidationException("Category is required", EventCreationStage.BASIC_INFO);
        }
        EventsCategoryEntity category = validateCategoryExists(request.getCategoryId());
        if (!category.getIsActive()) {
            throw new EventValidationException("Cannot create event in inactive category", EventCreationStage.BASIC_INFO);
        }


        // Required: Event Format
        if (request.getEventFormat() == null) {
            throw new EventValidationException("Event format is required", EventCreationStage.BASIC_INFO);
        }

        log.debug("Basic info hard validation passed");
    }

    /**
     * Hard validation for the schedule stage - all schedule fields must be valid
     */
    public void hardValidateSchedule(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing hard validation for schedule");

        // Required: Schedule object
        if (request.getSchedule() == null) {
            throw new EventValidationException("Schedule is required", EventCreationStage.SCHEDULE);
        }

        // Required: Days array
        if (request.getSchedule().getDays() == null || request.getSchedule().getDays().isEmpty()) {
            throw new EventValidationException("At least one day is required in schedule", EventCreationStage.SCHEDULE);
        }

        // Validate timezone
        if (request.getSchedule().getTimezone() == null || request.getSchedule().getTimezone().isBlank()) {
            throw new EventValidationException("Timezone is required", EventCreationStage.SCHEDULE);
        }
        validateTimezone(request.getSchedule().getTimezone());

        // Validate days
        validateDaysSchedule(request);

        log.debug("Schedule hard validation passed");
    }

    /**
     * Hard validation for location stage - validates based on event format
     */
    public void hardValidateLocation(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing hard validation for location");

        if (request.getEventFormat() == null) {
            throw new EventValidationException("Event format must be set before validating location", EventCreationStage.LOCATION_DETAILS);
        }

        // IN_PERSON events require venue
        if (request.getEventFormat() == EventFormat.IN_PERSON) {
            validateVenueRequired(request);
        }

        // ONLINE events require virtual details
        if (request.getEventFormat() == EventFormat.ONLINE) {
            validateVirtualDetailsRequired(request);
        }

        // HYBRID events require both venue and virtual details
        if (request.getEventFormat() == EventFormat.HYBRID) {
            validateVenueRequired(request);
            validateVirtualDetailsRequired(request);
        }

        log.debug("Location hard validation passed");
    }

    /**
     * Hard validation for media stage - optional but validates format if provided
     */
    public void hardValidateMedia(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing hard validation for media");

        // Media is optional, but if provided must be valid
        softValidateMedia(request);

        log.debug("Media hard validation passed");
    }


    /**
     * Hard validation for links stage - validates product and shop IDs
     */
    public void hardValidateLinks(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing hard validation for links");

        // Validate linked products
        if (request.getLinkedProductIds() != null && !request.getLinkedProductIds().isEmpty()) {
            validateLinkedProducts(request.getLinkedProductIds());
        }

        // Validate linked shops
        if (request.getLinkedShopIds() != null && !request.getLinkedShopIds().isEmpty()) {
            validateLinkedShops(request.getLinkedShopIds());
        }

        log.debug("Links hard validation passed");
    }

    /**
     * Validate linked products - ensure they exist and are active
     */
    private void validateLinkedProducts(List<UUID> productIds) throws EventValidationException {
        log.debug("Validating {} linked products", productIds.size());

        if (productIds.size() > 50) {
            throw new EventValidationException(
                    "Cannot link more than 50 products to an event",
                    EventCreationStage.LINKS
            );
        }

        List<String> invalidProducts = new ArrayList<>();
        List<String> inactiveProducts = new ArrayList<>();

        for (UUID productId : productIds) {
            var productOpt = productRepo.findByProductIdAndIsDeletedFalseAndStatus(
                    productId,
                    ProductStatus.ACTIVE
            );

            if (productOpt.isEmpty()) {
                // Check if product exists but is not active
                var anyStatusProduct = productRepo.findById(productId);

                if (anyStatusProduct.isEmpty() || anyStatusProduct.get().getIsDeleted()) {
                    invalidProducts.add(productId.toString());
                } else {
                    inactiveProducts.add(productId.toString() + " (status: " +
                            anyStatusProduct.get().getStatus() + ")");
                }
            }
        }

        // Report all validation errors
        if (!invalidProducts.isEmpty()) {
            throw new EventValidationException(
                    "The following products do not exist or have been deleted: " +
                            String.join(", ", invalidProducts),
                    EventCreationStage.LINKS
            );
        }

        if (!inactiveProducts.isEmpty()) {
            throw new EventValidationException(
                    "The following products are not active and cannot be linked: " +
                            String.join(", ", inactiveProducts),
                    EventCreationStage.LINKS
            );
        }

        log.debug("All linked products are valid and active");
    }

    /**
     * Validate linked shops - ensure they exist and are approved/active
     */
    private void validateLinkedShops(List<UUID> shopIds) throws EventValidationException {
        log.debug("Validating {} linked shops", shopIds.size());

        if (shopIds.size() > 20) {
            throw new EventValidationException(
                    "Cannot link more than 20 shops to an event",
                    EventCreationStage.LINKS
            );
        }

        List<String> invalidShops = new ArrayList<>();
        List<String> inactiveShops = new ArrayList<>();

        for (UUID shopId : shopIds) {
            var shopOpt = shopRepo.findByShopIdAndIsDeletedFalseAndStatus(
                    shopId,
                    ShopStatus.ACTIVE
            );

            if (shopOpt.isEmpty()) {
                // Check if shop exists but is not active
                var anyStatusShop = shopRepo.findById(shopId);

                if (anyStatusShop.isEmpty() || anyStatusShop.get().getIsDeleted()) {
                    invalidShops.add(shopId.toString());
                } else {
                    inactiveShops.add(shopId.toString() + " (status: " +
                            anyStatusShop.get().getStatus() + ")");
                }
            }
        }

        // Report all validation errors
        if (!invalidShops.isEmpty()) {
            throw new EventValidationException(
                    "The following shops do not exist or have been deleted: " +
                            String.join(", ", invalidShops),
                    EventCreationStage.LINKS
            );
        }

        if (!inactiveShops.isEmpty()) {
            throw new EventValidationException(
                    "The following shops are not active and cannot be linked: " +
                            String.join(", ", inactiveShops),
                    EventCreationStage.LINKS
            );
        }

        log.debug("All linked shops are valid and active");
    }

    /**
     * Soft validation for links - just check format, don't require them
     */
    public void softValidateLinks(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing soft validation for links");

        // Check product ID list size (if provided)
        if (request.getLinkedProductIds() != null && request.getLinkedProductIds().size() > 50) {
            throw new EventValidationException(
                    "Cannot link more than 50 products to an event",
                    EventCreationStage.LINKS
            );
        }

        // Check shop ID list size (if provided)
        if (request.getLinkedShopIds() != null && request.getLinkedShopIds().size() > 20) {
            throw new EventValidationException(
                    "Cannot link more than 20 shops to an event",
                    EventCreationStage.LINKS
            );
        }

        log.debug("Links soft validation passed");
    }


    /**
     * Validate event has required tickets before publishing
     */
    public void validateEventHasRequiredTickets(EventEntity event) throws EventValidationException {
        log.debug("Validating event has required tickets: {}", event.getId());

        ticketValidations.validateEventHasRequiredTickets(event);

        log.debug("Event ticket validation passed");
    }


    // ========================================================================
    // PUBLISH VALIDATION (Final validation before publishing)
    // ========================================================================

    /**
     * Comprehensive validation before publishing event
     * Validates all stages are completed and performs cross-stage validation
     */
    public void validateForPublish(CreateEventRequest request) throws EventValidationException {
        log.debug("Performing publish validation");

        // Validate all stages with hard validation
        hardValidateBasicInfo(request);
        hardValidateSchedule(request);
        hardValidateLocation(request);
        hardValidateMedia(request);
        hardValidateLinks(request);

        // Cross-stage validations
        // TODO: Add ticket validation when ticket module is implemented
        // validateTicketsExist(eventId);

        log.debug("Publish validation passed");
    }

    /**
     * Validate existing event entity before publishing (for draft to publish conversion)
     */
    public void validateEventEntityForPublish(EventEntity event) throws EventValidationException {
        log.debug("Performing publish validation for existing event: {}", event.getId());

        // Check all required stages are completed
        if (!event.isStageCompleted(EventCreationStage.BASIC_INFO)) {
            throw new EventValidationException("Basic info must be completed before publishing", EventCreationStage.BASIC_INFO);
        }

        if (!event.isStageCompleted(EventCreationStage.SCHEDULE)) {
            throw new EventValidationException("Schedule must be completed before publishing", EventCreationStage.SCHEDULE);
        }

        if (!event.isStageCompleted(EventCreationStage.LOCATION_DETAILS)) {
            throw new EventValidationException("Location details must be completed before publishing", EventCreationStage.LOCATION_DETAILS);
        }

        // Validate entity data
        validateEventEntityData(event);

        validateEventHasRequiredTickets(event);

        log.debug("Event entity publish validation passed");
    }

    // ========================================================================
    // HELPER VALIDATION METHODS
    // ========================================================================

    private EventsCategoryEntity validateCategoryExists(UUID categoryId) throws EventValidationException {
        return eventsCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventValidationException(
                        "Category not found with ID: " + categoryId,
                        EventCreationStage.BASIC_INFO
                ));
    }

    private void validateTimezone(String timezone) throws EventValidationException {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new EventValidationException(
                    "Invalid timezone: " + timezone,
                    EventCreationStage.SCHEDULE
            );
        }
    }


    private void validateDaysSchedule(CreateEventRequest request) throws EventValidationException {
        if (request.getSchedule().getDays() == null || request.getSchedule().getDays().isEmpty()) {
            throw new EventValidationException(
                    "At least one day is required in schedule",
                    EventCreationStage.SCHEDULE
            );
        }

        List<EventDayRequest> days = request.getSchedule().getDays();
        Set<LocalDate> seenDates = new HashSet<>();

        for (int i = 0; i < days.size(); i++) {
            EventDayRequest day = days.get(i);

            // Required fields
            if (day.getDate() == null) {
                throw new EventValidationException("Date is required for each day", EventCreationStage.SCHEDULE);
            }
            if (day.getStartTime() == null) {
                throw new EventValidationException("Start time is required for each day", EventCreationStage.SCHEDULE);
            }
            if (day.getEndTime() == null) {
                throw new EventValidationException("End time is required for each day", EventCreationStage.SCHEDULE);
            }

            // Check time logic
            if (day.getEndTime().isBefore(day.getStartTime())) {
                throw new EventValidationException(
                        "End time must be after start time for day: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }

            if (day.getEndTime().equals(day.getStartTime())) {
                throw new EventValidationException(
                        "Start and end time cannot be the same for day: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }

            // Check date is not in the past
            if (day.getDate().isBefore(LocalDate.now())) {
                throw new EventValidationException(
                        "Event date cannot be in the past: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }

            // Check for duplicate dates
            if (seenDates.contains(day.getDate())) {
                throw new EventValidationException(
                        "Duplicate date found in schedule: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }
            seenDates.add(day.getDate());

            // Auto-set dayOrder if not provided
            if (day.getDayOrder() == null) {
                day.setDayOrder(i + 1);
            }
        }

        // Ensure dates are in chronological order
        List<LocalDate> dates = days.stream()
                .map(EventDayRequest::getDate)
                .sorted()
                .collect(Collectors.toList());

        List<LocalDate> providedDates = days.stream()
                .map(EventDayRequest::getDate)
                .collect(Collectors.toList());

        if (!dates.equals(providedDates)) {
            throw new EventValidationException(
                    "Days must be in chronological order",
                    EventCreationStage.SCHEDULE
            );
        }
    }

    private void validateVenueRequired(CreateEventRequest request) throws EventValidationException {
        if (request.getVenue() == null) {
            throw new EventValidationException(
                    "Venue is required for " + request.getEventFormat() + " events",
                    EventCreationStage.LOCATION_DETAILS
            );
        }

        if (request.getVenue().getName() == null || request.getVenue().getName().isBlank()) {
            throw new EventValidationException("Venue name is required", EventCreationStage.LOCATION_DETAILS);
        }

        if (request.getVenue().getName().length() > 200) {
            throw new EventValidationException("Venue name must not exceed 200 characters", EventCreationStage.LOCATION_DETAILS);
        }

        if (request.getVenue().getAddress() != null && request.getVenue().getAddress().length() > 500) {
            throw new EventValidationException("Venue address must not exceed 500 characters", EventCreationStage.LOCATION_DETAILS);
        }
    }

    private void validateVirtualDetailsRequired(CreateEventRequest request) throws EventValidationException {
        if (request.getVirtualDetails() == null) {
            throw new EventValidationException(
                    "Virtual details are required for " + request.getEventFormat() + " events",
                    EventCreationStage.LOCATION_DETAILS
            );
        }

        if (request.getVirtualDetails().getMeetingLink() == null || request.getVirtualDetails().getMeetingLink().isBlank()) {
            throw new EventValidationException("Meeting link is required", EventCreationStage.LOCATION_DETAILS);
        }

        if (request.getVirtualDetails().getMeetingLink().length() > 500) {
            throw new EventValidationException("Meeting link must not exceed 500 characters", EventCreationStage.LOCATION_DETAILS);
        }
    }

    private void validateEventEntityData(EventEntity event) throws EventValidationException {
        // Basic validations
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new EventValidationException("Event title is missing", EventCreationStage.BASIC_INFO);
        }

        if (event.getCategory() == null) {
            throw new EventValidationException("Event category is missing", EventCreationStage.BASIC_INFO);
        }

        // Schedule validations
        if (event.getStartDateTime() == null || event.getEndDateTime() == null) {
            throw new EventValidationException("Event schedule is incomplete", EventCreationStage.SCHEDULE);
        }

        // Location validations based on format
        if (event.getEventFormat() == EventFormat.IN_PERSON || event.getEventFormat() == EventFormat.HYBRID) {
            if (event.getVenue() == null) {
                throw new EventValidationException("Venue is missing", EventCreationStage.LOCATION_DETAILS);
            }
        }

        if (event.getEventFormat() == EventFormat.ONLINE || event.getEventFormat() == EventFormat.HYBRID) {
            if (event.getVirtualDetails() == null) {
                throw new EventValidationException("Virtual details are missing", EventCreationStage.LOCATION_DETAILS);
            }
        }
    }
}


