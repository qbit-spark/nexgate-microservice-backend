package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.repo.EventsCategoryRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventCreationStage;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.*;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils.validations.TicketValidations;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventValidations {

    private final EventsCategoryRepository eventsCategoryRepository;
    private final ShopRepo shopRepo;
    private final ProductRepo productRepo;
    private final TicketValidations ticketValidations;

    // ========================================================================
    // DRAFT CREATION VALIDATION
    // ========================================================================

    public void validateCreateDraft(CreateEventDraftRequest request) throws EventValidationException {
        log.debug("Validating create draft request");

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new EventValidationException("Event title is required", EventCreationStage.BASIC_INFO);
        }
        if (request.getTitle().length() < 3) {
            throw new EventValidationException("Title must be at least 3 characters", EventCreationStage.BASIC_INFO);
        }
        if (request.getTitle().length() > 200) {
            throw new EventValidationException("Title must not exceed 200 characters", EventCreationStage.BASIC_INFO);
        }

        if (request.getCategoryId() == null) {
            throw new EventValidationException("Category is required", EventCreationStage.BASIC_INFO);
        }
        EventsCategoryEntity category = validateCategoryExists(request.getCategoryId());
        if (!category.getIsActive()) {
            throw new EventValidationException("Cannot create event in inactive category", EventCreationStage.BASIC_INFO);
        }

        if (request.getEventFormat() == null) {
            throw new EventValidationException("Event format is required", EventCreationStage.BASIC_INFO);
        }
    }

    // ========================================================================
    // BASIC INFO VALIDATION
    // ========================================================================

    public void validateBasicInfo(UpdateEventBasicInfoRequest request) throws EventValidationException {
        log.debug("Validating basic info update");

        if (request.getTitle() != null) {
            if (request.getTitle().length() < 3) {
                throw new EventValidationException("Title must be at least 3 characters", EventCreationStage.BASIC_INFO);
            }
            if (request.getTitle().length() > 200) {
                throw new EventValidationException("Title must not exceed 200 characters", EventCreationStage.BASIC_INFO);
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() < 15) {
                throw new EventValidationException("Description must be at least 15 characters", EventCreationStage.BASIC_INFO);
            }
            if (request.getDescription().length() > 5000) {
                throw new EventValidationException("Description must not exceed 5000 characters", EventCreationStage.BASIC_INFO);
            }
        }

        if (request.getCategoryId() != null) {
            EventsCategoryEntity category = validateCategoryExists(request.getCategoryId());
            if (!category.getIsActive()) {
                throw new EventValidationException("Cannot use inactive category", EventCreationStage.BASIC_INFO);
            }
        }
    }

    // ========================================================================
    // SCHEDULE VALIDATION
    // ========================================================================

    public void validateSchedule(ScheduleRequest request) throws EventValidationException {
        log.debug("Validating schedule update");

        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new EventValidationException("At least one day is required", EventCreationStage.SCHEDULE);
        }

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            validateTimezone(request.getTimezone());
        }

        validateDays(request.getDays());
    }

    private void validateDays(List<EventDayRequest> days) throws EventValidationException {
        Set<LocalDate> seenDates = new HashSet<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < days.size(); i++) {
            EventDayRequest day = days.get(i);

            if (day.getDate() == null) {
                throw new EventValidationException("Date is required for each day", EventCreationStage.SCHEDULE);
            }
            if (day.getStartTime() == null) {
                throw new EventValidationException("Start time is required for each day", EventCreationStage.SCHEDULE);
            }
            if (day.getEndTime() == null) {
                throw new EventValidationException("End time is required for each day", EventCreationStage.SCHEDULE);
            }

            if (day.getDate().isBefore(today)) {
                throw new EventValidationException(
                        "Event date cannot be in the past: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }

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

            if (seenDates.contains(day.getDate())) {
                throw new EventValidationException(
                        "Duplicate date found: " + day.getDate(),
                        EventCreationStage.SCHEDULE
                );
            }
            seenDates.add(day.getDate());

            if (day.getDayOrder() == null) {
                day.setDayOrder(i + 1);
            }
        }

        // Ensure chronological order
        List<LocalDate> sortedDates = days.stream()
                .map(EventDayRequest::getDate)
                .sorted()
                .collect(Collectors.toList());

        List<LocalDate> providedDates = days.stream()
                .map(EventDayRequest::getDate)
                .collect(Collectors.toList());

        if (!sortedDates.equals(providedDates)) {
            throw new EventValidationException("Days must be in chronological order", EventCreationStage.SCHEDULE);
        }
    }

    // ========================================================================
    // LOCATION VALIDATION
    // ========================================================================

    public void validateLocation(UpdateEventLocationRequest request, EventFormat format)
            throws EventValidationException {
        log.debug("Validating location update for format: {}", format);

        if (format == EventFormat.IN_PERSON) {
            validateVenue(request.getVenue(), true);
        } else if (format == EventFormat.ONLINE) {
            validateVirtualDetails(request.getVirtualDetails(), true);
        } else if (format == EventFormat.HYBRID) {
            validateVenue(request.getVenue(), true);
            validateVirtualDetails(request.getVirtualDetails(), true);
        }
    }

    private void validateVenue(VenueRequest venue, boolean required) throws EventValidationException {
        if (venue == null) {
            if (required) {
                throw new EventValidationException("Venue is required", EventCreationStage.LOCATION_DETAILS);
            }
            return;
        }

        if (required && (venue.getName() == null || venue.getName().isBlank())) {
            throw new EventValidationException("Venue name is required", EventCreationStage.LOCATION_DETAILS);
        }

        if (venue.getName() != null && venue.getName().length() > 200) {
            throw new EventValidationException("Venue name must not exceed 200 characters", EventCreationStage.LOCATION_DETAILS);
        }

        if (venue.getAddress() != null && venue.getAddress().length() > 500) {
            throw new EventValidationException("Venue address must not exceed 500 characters", EventCreationStage.LOCATION_DETAILS);
        }
    }

    private void validateVirtualDetails(VirtualDetailsRequest details, boolean required)
            throws EventValidationException {
        if (details == null) {
            if (required) {
                throw new EventValidationException("Virtual details are required", EventCreationStage.LOCATION_DETAILS);
            }
            return;
        }

        if (required && (details.getMeetingLink() == null || details.getMeetingLink().isBlank())) {
            throw new EventValidationException("Meeting link is required", EventCreationStage.LOCATION_DETAILS);
        }

        if (details.getMeetingLink() != null && details.getMeetingLink().length() > 500) {
            throw new EventValidationException("Meeting link must not exceed 500 characters", EventCreationStage.LOCATION_DETAILS);
        }

        if (details.getMeetingId() != null && details.getMeetingId().length() > 100) {
            throw new EventValidationException("Meeting ID must not exceed 100 characters", EventCreationStage.LOCATION_DETAILS);
        }

        if (details.getPasscode() != null && details.getPasscode().length() > 100) {
            throw new EventValidationException("Passcode must not exceed 100 characters", EventCreationStage.LOCATION_DETAILS);
        }
    }

    // ========================================================================
    // MEDIA VALIDATION
    // ========================================================================

    public void validateMedia(MediaRequest request) throws EventValidationException {
        log.debug("Validating media update");

        if (request == null) {
            return;
        }

        if (request.getBanner() != null && request.getBanner().length() > 500) {
            throw new EventValidationException("Banner URL must not exceed 500 characters", EventCreationStage.MEDIA);
        }

        if (request.getThumbnail() != null && request.getThumbnail().length() > 500) {
            throw new EventValidationException("Thumbnail URL must not exceed 500 characters", EventCreationStage.MEDIA);
        }

        if (request.getGallery() != null && request.getGallery().size() > 20) {
            throw new EventValidationException("Gallery cannot have more than 20 images", EventCreationStage.MEDIA);
        }
    }

    // ========================================================================
    // ATTACHMENT VALIDATION (Product/Shop)
    // ========================================================================

    public void validateProductAttachment(UUID productId) throws EventValidationException {
        log.debug("Validating product attachment: {}", productId);

        var product = productRepo.findByProductIdAndIsDeletedFalseAndStatus(productId, ProductStatus.ACTIVE);

        if (product.isEmpty()) {
            var anyProduct = productRepo.findById(productId);
            if (anyProduct.isEmpty() || anyProduct.get().getIsDeleted()) {
                throw new EventValidationException("Product not found: " + productId, EventCreationStage.LINKS);
            } else {
                throw new EventValidationException(
                        "Product is not active (status: " + anyProduct.get().getStatus() + ")",
                        EventCreationStage.LINKS
                );
            }
        }
    }

    public void validateShopAttachment(UUID shopId) throws EventValidationException {
        log.debug("Validating shop attachment: {}", shopId);

        var shop = shopRepo.findByShopIdAndIsDeletedFalseAndStatus(shopId, ShopStatus.ACTIVE);

        if (shop.isEmpty()) {
            var anyShop = shopRepo.findById(shopId);
            if (anyShop.isEmpty() || anyShop.get().getIsDeleted()) {
                throw new EventValidationException("Shop not found: " + shopId, EventCreationStage.LINKS);
            } else {
                throw new EventValidationException(
                        "Shop is not active (status: " + anyShop.get().getStatus() + ")",
                        EventCreationStage.LINKS
                );
            }
        }
    }

    public void validateLinkedProductsCount(int currentCount) throws EventValidationException {
        if (currentCount >= 50) {
            throw new EventValidationException("Cannot link more than 50 products", EventCreationStage.LINKS);
        }
    }

    public void validateLinkedShopsCount(int currentCount) throws EventValidationException {
        if (currentCount >= 20) {
            throw new EventValidationException("Cannot link more than 20 shops", EventCreationStage.LINKS);
        }
    }

    // ========================================================================
    // PUBLISH VALIDATION (Final check before publishing)
    // ========================================================================

    public void validateForPublish(EventEntity event) throws EventValidationException {
        log.debug("Validating event for publish: {}", event.getId());

        // Check required stages
        if (!event.isStageCompleted(EventCreationStage.BASIC_INFO)) {
            throw new EventValidationException("Basic info must be completed", EventCreationStage.BASIC_INFO);
        }

        if (!event.isStageCompleted(EventCreationStage.SCHEDULE)) {
            throw new EventValidationException("Schedule must be completed", EventCreationStage.SCHEDULE);
        }

        if (!event.isStageCompleted(EventCreationStage.LOCATION_DETAILS)) {
            throw new EventValidationException("Location must be completed", EventCreationStage.LOCATION_DETAILS);
        }

        // Validate entity data
        validateEventData(event);

        // Validate tickets
        ticketValidations.validateEventHasRequiredTickets(event);

        log.debug("Event validation for publish passed");
    }

    private void validateEventData(EventEntity event) throws EventValidationException {
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new EventValidationException("Event title is missing", EventCreationStage.BASIC_INFO);
        }

        if (event.getCategory() == null) {
            throw new EventValidationException("Event category is missing", EventCreationStage.BASIC_INFO);
        }

        if (event.getStartDateTime() == null || event.getEndDateTime() == null) {
            throw new EventValidationException("Event schedule is incomplete", EventCreationStage.SCHEDULE);
        }

        EventFormat format = event.getEventFormat();

        if (format == EventFormat.IN_PERSON || format == EventFormat.HYBRID) {
            if (event.getVenue() == null || event.getVenue().getName() == null) {
                throw new EventValidationException("Venue is missing", EventCreationStage.LOCATION_DETAILS);
            }
        }

        if (format == EventFormat.ONLINE || format == EventFormat.HYBRID) {
            if (event.getVirtualDetails() == null || event.getVirtualDetails().getMeetingLink() == null) {
                throw new EventValidationException("Virtual details are missing", EventCreationStage.LOCATION_DETAILS);
            }
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private EventsCategoryEntity validateCategoryExists(UUID categoryId) throws EventValidationException {
        return eventsCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventValidationException(
                        "Category not found: " + categoryId,
                        EventCreationStage.BASIC_INFO
                ));
    }

    private void validateTimezone(String timezone) throws EventValidationException {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new EventValidationException("Invalid timezone: " + timezone, EventCreationStage.SCHEDULE);
        }
    }
}