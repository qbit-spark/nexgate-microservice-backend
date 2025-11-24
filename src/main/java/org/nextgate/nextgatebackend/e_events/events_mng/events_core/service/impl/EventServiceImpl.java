package org.nextgate.nextgatebackend.e_events.events_mng.events_core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.repo.EventsCategoryRepository;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventDayEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventCreationStage;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventType;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.service.EventsService;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.DuplicateValidationResult;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.EventDuplicateValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.EventValidations;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventsService {

    private final AccountRepo accountRepo;
    private final EventsRepo eventsRepo;
    private final EventsCategoryRepository categoryRepository;
    private final EventValidations eventValidations;
    private final EventDuplicateValidator duplicateValidator;
    private final ShopRepo shopRepo;
    private final ProductRepo productRepo;



    @Override
    @Transactional
    public EventEntity createEvent(CreateEventRequest createEventRequest, EventSubmissionAction action)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating event with action: {}", action);

        // Get an authenticated user
        AccountEntity currentUser = getAuthenticatedAccount();

        // Validate user has permission to create events
        validateRole(currentUser, "ADMIN", "EVENT_MANAGER", "ORGANIZER");

        // Route to the appropriate method based on action
        EventEntity eventEntity = switch (action) {
            case SAVE_DRAFT -> saveEventAsDraft(createEventRequest, currentUser);
            case PUBLISH -> saveAndPublishEvent(createEventRequest, currentUser);
        };

        log.info("Event created successfully with ID: {}", eventEntity.getId());
        return eventEntity;


        /***
         *
         * // User is creating a recurring event in draft mode
         * Recurrence recurrence = Recurrence.builder()
         *     .frequency(RecurrenceFrequency.WEEKLY)
         *     .interval(1)
         *     .daysOfWeek(Set.of("MONDAY", "WEDNESDAY"))
         *     .recurrenceStartDate(LocalDate.of(2025, 3, 1))
         *     .recurrenceEndDate(LocalDate.of(2025, 6, 30))
         *     .build();
         *
         * // Preview WITHOUT saving
         * SessionPreviewResponse preview = sessionGenerator.previewSessions(
         *     recurrence,
         *     LocalTime.of(18, 0),
         *     LocalTime.of(19, 30)
         * );
         *
         * // Return to frontend
         * return preview;
         *
         *
         */
    }

    /**
     * Save event as DRAFT
     * - Deep validation for Basic Info
     * - Soft validation for other sections
     */
    private EventEntity saveEventAsDraft(CreateEventRequest request, AccountEntity currentUser)
            throws EventValidationException {

        log.debug("Saving event as DRAFT for user: {}", currentUser.getUserName());

        // 1. DEEP validation for Basic Info (always required)
        eventValidations.hardValidateBasicInfo(request);

        // 2. SOFT validation for other sections (format checks only)
        eventValidations.softValidateSchedule(request);
        eventValidations.softValidateLocation(request);
        eventValidations.softValidateMedia(request);

        // 3. Get category
        EventsCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EventValidationException(
                        "Category not found",
                        EventCreationStage.BASIC_INFO
                ));

        // 4. Build event entity
        EventEntity event = buildEventEntity(request, currentUser, category);

        // 5. Set as DRAFT
        event.setStatus(EventStatus.DRAFT);
        event.setCurrentStage(EventCreationStage.BASIC_INFO);
        event.setCompletedStages(new ArrayList<>());
        event.markStageCompleted(EventCreationStage.BASIC_INFO);
        event.setSlug(generateUniqueSlug(request.getTitle()));

        // 7. Save
        EventEntity savedEvent = eventsRepo.save(event);

        log.info("Event saved as DRAFT with ID: {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Save and publish event
     * - Deep validation for ALL sections
     * - Check for duplicates
     * - Update category count
     */
    private EventEntity saveAndPublishEvent(CreateEventRequest request, AccountEntity currentUser)
            throws EventValidationException {

        log.debug("Publishing event for user: {}", currentUser.getUserName());

        // 1. HARD validation for ALL sections
        eventValidations.hardValidateBasicInfo(request);
        eventValidations.hardValidateSchedule(request);
        eventValidations.hardValidateLocation(request);
        eventValidations.hardValidateMedia(request);
        eventValidations.hardValidateLinks(request);

        // 2. Check for duplicates (fraud prevention)
        DuplicateValidationResult duplicateCheck = duplicateValidator.validateNoDuplicate(
                request,
                currentUser.getId()
        );

        if (duplicateCheck.isBlocked()) {
            log.warn("Duplicate event detected for user: {}", currentUser.getUserName());
            throw new EventValidationException(
                    duplicateCheck.getMessage(),
                    EventCreationStage.BASIC_INFO
            );
        }

        // Log warning if similar event exists (but allow)
        if (duplicateCheck.isWarning()) {
            log.warn("Similar event exists: {}", duplicateCheck.getMessage());
            // Could store this warning to return in response
        }

        // 3. Get category
        EventsCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EventValidationException(
                        "Category not found",
                        EventCreationStage.BASIC_INFO
                ));

        // 4. Build event entity
        EventEntity event = buildEventEntity(request, currentUser, category);

        // 5. Set as PUBLISHED
        event.setStatus(EventStatus.PUBLISHED);
        markAllStagesCompleted(event);

        // 6. Generate slug if not provided
        if (event.getSlug() == null || event.getSlug().isBlank()) {
            event.setSlug(generateUniqueSlug(request.getTitle()));
        }

        // 7. Save event
        EventEntity savedEvent = eventsRepo.save(event);

        // 8. Update category event count
        category.setEventCount(category.getEventCount() + 1);
        categoryRepository.save(category);

        log.info("Event published successfully with ID: {}", savedEvent.getId());
        return savedEvent;
    }

    /**
     * Build EventEntity from CreateEventRequest
     */
    private EventEntity buildEventEntity(
            CreateEventRequest request,
            AccountEntity organizer,
            EventsCategoryEntity category) throws EventValidationException {

        EventEntity.EventEntityBuilder builder = EventEntity.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .description(request.getDescription())
                .category(category)
                .eventType(request.getEventType())
                .eventFormat(request.getEventFormat())
                .organizer(organizer)
                .createdBy(organizer)
                .isDeleted(false)
                .completedStages(new ArrayList<>());

        // Map schedule
        if (request.getSchedule() != null) {
            builder.startDateTime(request.getSchedule().getStartDateTime())
                    .endDateTime(request.getSchedule().getEndDateTime())
                    .timezone(request.getSchedule().getTimezone());

            // Map recurrence if present
            if (request.getSchedule().getRecurrence() != null) {
                builder.recurrence(mapRecurrence(request.getSchedule().getRecurrence()));
            }
        }

        // Map venue (for IN_PERSON and HYBRID)
        if (request.getVenue() != null) {
            builder.venue(mapVenue(request.getVenue()));
        }

        // Map virtual details (for ONLINE and HYBRID)
        if (request.getVirtualDetails() != null) {
            builder.virtualDetails(mapVirtualDetails(request.getVirtualDetails()));
        }

        // Map media
        if (request.getMedia() != null) {
            builder.media(mapMedia(request.getMedia()));
        }

        // Build the event first
        EventEntity event = builder.build();

        // ========== NEW: Map EventDays for MULTI_DAY events ==========
        if (request.getEventType() == EventType.MULTI_DAY &&
                request.getSchedule() != null &&
                request.getSchedule().getDays() != null &&
                !request.getSchedule().getDays().isEmpty()) {

            List<EventDayEntity> eventDays = mapEventDays(
                    request.getSchedule().getDays(),
                    event
            );
            event.setDays(eventDays);

            log.debug("Added {} days to MULTI_DAY event", eventDays.size());
        }
        // ============================================================

        // Map linked products
        if (request.getLinkedProductIds() != null && !request.getLinkedProductIds().isEmpty()) {
            event.setLinkedProducts(mapLinkedProducts(request.getLinkedProductIds()));
        }

        // Map linked shops
        if (request.getLinkedShopIds() != null && !request.getLinkedShopIds().isEmpty()) {
            event.setLinkedShops(mapLinkedShops(request.getLinkedShopIds()));
        }

        return event;
    }


    /**
     * Map linked product IDs to the ProductEntity list
     */private List<ProductEntity> mapLinkedProducts(List<UUID> productIds) throws EventValidationException {
        log.debug("Mapping {} linked products", productIds.size());

        List<ProductEntity> products = new ArrayList<>();

        for (UUID productId : productIds) {
            ProductEntity product = productRepo
                    .findByProductIdAndIsDeletedFalseAndStatus(productId, ProductStatus.ACTIVE)
                    .orElseThrow(() -> new EventValidationException(
                            "Product not found or not active: " + productId,
                            EventCreationStage.LINKS
                    ));

            products.add(product);
        }

        log.debug("Successfully mapped {} products", products.size());
        return products;
    }

    /**
     * Map linked shop IDs to ShopEntity list
     */
    private List<ShopEntity> mapLinkedShops(List<UUID> shopIds) throws EventValidationException {
        log.debug("Mapping {} linked shops", shopIds.size());

        List<ShopEntity> shops = new ArrayList<>();

        for (UUID shopId : shopIds) {
            ShopEntity shop = shopRepo
                    .findByShopIdAndIsDeletedFalseAndStatus(shopId, ShopStatus.ACTIVE)
                    .orElseThrow(() -> new EventValidationException(
                            "Shop not found or not active: " + shopId,
                            EventCreationStage.LINKS
                    ));

            shops.add(shop);
        }

        log.debug("Successfully mapped {} shops", shops.size());
        return shops;
    }

    /**
     * Map VenueRequest to Venue (embedded)
     */
    private Venue mapVenue(VenueRequest request) {
        if (request == null) return null;

        Coordinates coordinates = null;
        if (request.getLatitude() != null && request.getLongitude() != null) {
            coordinates = Coordinates.builder()
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .build();
        }

        return Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .coordinates(coordinates)
                .build();
    }

    /**
     * Map VirtualDetailsRequest to VirtualDetails (embedded)
     */
    private VirtualDetails mapVirtualDetails(VirtualDetailsRequest request) {
        if (request == null) return null;

        return VirtualDetails.builder()
                .meetingLink(request.getMeetingLink())
                .meetingId(request.getMeetingId())
                .passcode(request.getPasscode())
                .build();
    }

    /**
     * Map EventDayRequest list to EventDayEntity list
     * For MULTI_DAY events
     */
    private List<EventDayEntity> mapEventDays(
            List<EventDayRequest> dayRequests,
            EventEntity parentEvent) {

        if (dayRequests == null || dayRequests.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("Mapping {} event days for event", dayRequests.size());

        List<EventDayEntity> eventDays = new ArrayList<>();

        for (EventDayRequest dayRequest : dayRequests) {
            EventDayEntity eventDay = EventDayEntity.builder()
                    .eventEntity(parentEvent)
                    .date(dayRequest.getDate())
                    .startTime(dayRequest.getStartTime())
                    .endTime(dayRequest.getEndTime())
                    .description(dayRequest.getDescription())
                    .dayOrder(dayRequest.getDayOrder())
                    .build();

            eventDays.add(eventDay);
        }

        log.debug("Successfully mapped {} event days", eventDays.size());
        return eventDays;
    }

    /**
     * Map RecurrenceRequest to Recurrence
     */
    private Recurrence mapRecurrence(RecurrenceRequest request) {
        if (request == null) return null;

        return Recurrence.builder()
                .frequency(request.getFrequency())
                .interval(request.getInterval())
                .daysOfWeek(request.getDaysOfWeek())
                .dayOfMonth(request.getDayOfMonth())
                .recurrenceStartDate(request.getRecurrenceStartDate())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .exceptions(request.getExceptions())
                .build();
    }

    /**
     * Map MediaRequest to Media
     */
    private Media mapMedia(MediaRequest request) {
        if (request == null) return null;

        return Media.builder()
                .banner(request.getBanner())
                .thumbnail(request.getThumbnail())
                .gallery(request.getGallery() != null ? new ArrayList<>(request.getGallery()) : new ArrayList<>())
                .build();
    }

    /**
     * Generate unique slug from title
     */
    private String generateUniqueSlug(String title) {
        // Base slug from title
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        // Check if slug exists
        String slug = baseSlug;
        int counter = 1;

        while (eventsRepo.existsBySlugAndIsDeletedFalse(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        log.debug("Generated unique slug: {}", slug);
        return slug;
    }

    /**
     * Mark all stages as completed (for publish)
     */
    private void markAllStagesCompleted(EventEntity event) {
        event.markStageCompleted(EventCreationStage.BASIC_INFO);
        event.markStageCompleted(EventCreationStage.SCHEDULE);
        event.markStageCompleted(EventCreationStage.LOCATION_DETAILS);
        event.markStageCompleted(EventCreationStage.TICKETS);
        event.markStageCompleted(EventCreationStage.MEDIA);
        event.markStageCompleted(EventCreationStage.LINKS);
        event.markStageCompleted(EventCreationStage.REVIEW);
    }

    /**
     * Get authenticated account from security context
     */
    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ItemNotFoundException("User not authenticated");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userName = userDetails.getUsername();

        return accountRepo.findByUserName(userName)
                .orElseThrow(() -> new ItemNotFoundException("User not found: " + userName));
    }

    /**
     * Validate user has required roles
     */
    public void validateRole(AccountEntity account, String... requiredRoles) throws AccessDeniedException {
        if (account == null) {
            throw new AccessDeniedException("Account not found");
        }

        if (account.getRoles() == null || account.getRoles().isEmpty()) {
            throw new AccessDeniedException("Account has no roles assigned");
        }

        // Get account's role names
        Set<String> accountRoleNames = account.getRoles().stream()
                .map(Roles::getRoleName)
                .collect(Collectors.toSet());

        // Check if account has any of the required roles
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(accountRoleNames::contains);

        if (!hasRequiredRole) {
            log.warn("Access denied for user: {}. Required roles: {}, User roles: {}",
                    account.getUserName(), Arrays.toString(requiredRoles), accountRoleNames);
            throw new AccessDeniedException("Access denied. Required roles: " + Arrays.toString(requiredRoles));
        }
    }
}