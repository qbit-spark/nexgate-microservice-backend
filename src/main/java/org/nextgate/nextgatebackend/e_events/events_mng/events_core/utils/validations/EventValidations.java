package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventValidations {

    private final EventsCategoryRepository categoryRepository;
    private final TicketValidations ticketValidations;

    // ========================================================================
    // DRAFT CREATION
    // ========================================================================

    public void validateCreateDraft(CreateEventDraftRequest request) throws EventValidationException {
        validateTitle(request.getTitle(), true);
        validateCategory(request.getCategoryId());

        if (request.getEventFormat() == null) {
            throw new EventValidationException("Event format is required", EventCreationStage.BASIC_INFO);
        }
    }

    // ========================================================================
    // BASIC INFO
    // ========================================================================

    public void validateBasicInfo(UpdateEventBasicInfoRequest request) throws EventValidationException {
        if (request.getTitle() != null) {
            validateTitle(request.getTitle(), false);
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
            validateCategory(request.getCategoryId());
        }
    }

    // ========================================================================
    // SCHEDULE
    // ========================================================================

    public void validateSchedule(ScheduleRequest request) throws EventValidationException {
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new EventValidationException("At least one day is required", EventCreationStage.SCHEDULE);
        }

        if (request.getTimezone() != null && !request.getTimezone().isBlank()) {
            validateTimezone(request.getTimezone());
        }

        validateDays(request.getDays());
    }

    // ========================================================================
    // LOCATION
    // ========================================================================

    public void validateLocation(UpdateEventLocationRequest request, EventFormat format)
            throws EventValidationException {

        boolean venueRequired = format == EventFormat.IN_PERSON || format == EventFormat.HYBRID;
        boolean virtualRequired = format == EventFormat.ONLINE || format == EventFormat.HYBRID;

        if (venueRequired) {
            validateVenue(request.getVenue(), true);
        } else if (request.getVenue() != null) {
            validateVenue(request.getVenue(), false);
        }

        if (virtualRequired) {
            validateVirtualDetails(request.getVirtualDetails(), true);
        } else if (request.getVirtualDetails() != null) {
            validateVirtualDetails(request.getVirtualDetails(), false);
        }
    }

    // ========================================================================
    // MEDIA
    // ========================================================================

    public void validateMedia(MediaRequest request) throws EventValidationException {
        if (request == null) return;

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
    // PUBLISH
    // ========================================================================

    public void validateForPublish(EventEntity event) throws EventValidationException {
        // Check required stages
        checkStageCompleted(event, EventCreationStage.BASIC_INFO);
        checkStageCompleted(event, EventCreationStage.SCHEDULE);
        checkStageCompleted(event, EventCreationStage.LOCATION_DETAILS);

        // Validate entity data
        validateEventData(event);

        // Validate tickets
        ticketValidations.validateEventHasRequiredTickets(event);
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private void validateTitle(String title, boolean required) throws EventValidationException {
        if (title == null || title.isBlank()) {
            if (required) {
                throw new EventValidationException("Event title is required", EventCreationStage.BASIC_INFO);
            }
            return;
        }

        if (title.length() < 3) {
            throw new EventValidationException("Title must be at least 3 characters", EventCreationStage.BASIC_INFO);
        }
        if (title.length() > 200) {
            throw new EventValidationException("Title must not exceed 200 characters", EventCreationStage.BASIC_INFO);
        }
    }

    private void validateCategory(UUID categoryId) throws EventValidationException {
        if (categoryId == null) {
            throw new EventValidationException("Category is required", EventCreationStage.BASIC_INFO);
        }

        EventsCategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventValidationException(
                        "Category not found: " + categoryId, EventCreationStage.BASIC_INFO));

        if (!category.getIsActive()) {
            throw new EventValidationException("Cannot use inactive category", EventCreationStage.BASIC_INFO);
        }
    }

    private void validateTimezone(String timezone) throws EventValidationException {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new EventValidationException("Invalid timezone: " + timezone, EventCreationStage.SCHEDULE);
        }
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
                throw new EventValidationException("Event date cannot be in the past: " + day.getDate(), EventCreationStage.SCHEDULE);
            }

            if (!day.getEndTime().isAfter(day.getStartTime())) {
                throw new EventValidationException("End time must be after start time for: " + day.getDate(), EventCreationStage.SCHEDULE);
            }

            if (!seenDates.add(day.getDate())) {
                throw new EventValidationException("Duplicate date: " + day.getDate(), EventCreationStage.SCHEDULE);
            }

            if (day.getDayOrder() == null) {
                day.setDayOrder(i + 1);
            }
        }

        // Check chronological order
        List<LocalDate> sorted = days.stream().map(EventDayRequest::getDate).sorted().collect(Collectors.toList());
        List<LocalDate> provided = days.stream().map(EventDayRequest::getDate).collect(Collectors.toList());

        if (!sorted.equals(provided)) {
            throw new EventValidationException("Days must be in chronological order", EventCreationStage.SCHEDULE);
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
            throw new EventValidationException("Address must not exceed 500 characters", EventCreationStage.LOCATION_DETAILS);
        }
    }

    private void validateVirtualDetails(VirtualDetailsRequest details, boolean required) throws EventValidationException {
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
    }

    private void checkStageCompleted(EventEntity event, EventCreationStage stage) throws EventValidationException {
        if (!event.isStageCompleted(stage)) {
            throw new EventValidationException(stage.name() + " must be completed before publishing", stage);
        }
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

        if (event.getStartDateTime().isBefore(ZonedDateTime.now())) {
            throw new EventValidationException(
                    "Cannot publish event with start date in the past",
                    EventCreationStage.SCHEDULE
            );
        }

        if (event.getEndDateTime().isBefore(event.getStartDateTime())) {
            throw new EventValidationException(
                    "End date cannot be before start date",
                    EventCreationStage.SCHEDULE
            );
        }

        EventFormat format = event.getEventFormat();

        if ((format == EventFormat.IN_PERSON || format == EventFormat.HYBRID) &&
                (event.getVenue() == null || event.getVenue().getName() == null)) {
            throw new EventValidationException("Venue is missing", EventCreationStage.LOCATION_DETAILS);
        }

        if ((format == EventFormat.ONLINE || format == EventFormat.HYBRID) &&
                (event.getVirtualDetails() == null || event.getVirtualDetails().getMeetingLink() == null)) {
            throw new EventValidationException("Virtual details are missing", EventCreationStage.LOCATION_DETAILS);
        }
    }
}