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
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventFormat;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventSubmissionAction;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    public EventEntity createEvent(CreateEventRequest createEventRequest)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Creating event as draft");

        AccountEntity currentUser = getAuthenticatedAccount();

        log.debug("Saving event as DRAFT for user: {}", currentUser.getUserName());

        eventValidations.hardValidateBasicInfo(createEventRequest);
        eventValidations.softValidateSchedule(createEventRequest);
        eventValidations.softValidateLocation(createEventRequest);
        eventValidations.softValidateMedia(createEventRequest);

        EventsCategoryEntity category = categoryRepository.findById(createEventRequest.getCategoryId())
                .orElseThrow(() -> new EventValidationException(
                        "Category not found",
                        EventCreationStage.BASIC_INFO
                ));

        EventEntity event = buildEventEntity(createEventRequest, currentUser, category);

        event.setStatus(EventStatus.DRAFT);
        event.setSlug(generateUniqueSlug(createEventRequest.getTitle()));

        markCompletedStagesForDraft(event, createEventRequest);

        EventEntity savedEvent = eventsRepo.save(event);

        log.info("Event saved as DRAFT with ID: {}", savedEvent.getId());
        return savedEvent;
    }


    @Override
    @Transactional
    public EventEntity publishEvent(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        log.info("Publishing event: {}", eventId);

        AccountEntity currentUser = getAuthenticatedAccount();

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found with ID: " + eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only event organizer can publish the event");
        }

        if (event.getStatus() == EventStatus.PUBLISHED) {
            throw new EventValidationException("Event is already published", EventCreationStage.REVIEW);
        }

        eventValidations.validateEventEntityForPublish(event);

        DuplicateValidationResult duplicateCheck = duplicateValidator.validateNoDuplicateForEntity(event);

        if (duplicateCheck.isBlocked()) {
            log.warn("Duplicate event detected for user: {}", currentUser.getUserName());
            throw new EventValidationException(
                    duplicateCheck.getMessage(),
                    EventCreationStage.BASIC_INFO
            );
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setUpdatedBy(currentUser);

        EventsCategoryEntity category = event.getCategory();
        category.setEventCount(category.getEventCount() + 1);
        categoryRepository.save(category);

        EventEntity publishedEvent = eventsRepo.save(event);

        log.info("Event published successfully with ID: {}", eventId);
        return publishedEvent;
    }

    @Override
    @Transactional(readOnly = true)
    public EventEntity getEventById(UUID eventId) throws ItemNotFoundException, AccessDeniedException {
        log.debug("Fetching event with ID: {}", eventId);

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found with ID: " + eventId));


        // Only organizer can view DRAFT events
        if (event.getStatus() == EventStatus.DRAFT) {
            AccountEntity currentUser = getAuthenticatedAccount();

            if (!event.getOrganizer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Only event organizer can view draft events");
            }
        }

        // PUBLISHED events are public - anyone can view
        return event;
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
                .eventFormat(request.getEventFormat())
                .organizer(organizer)
                .createdBy(organizer)
                .eventVisibility(request.getEventVisibility())
                .isDeleted(false)
                .completedStages(new ArrayList<>());

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
        if (request.getSchedule() != null &&
                request.getSchedule().getDays() != null &&
                !request.getSchedule().getDays().isEmpty()) {

            List<EventDayEntity> eventDays = mapEventDays(
                    request.getSchedule().getDays(),
                    event
            );
            event.setDays(eventDays);

            // Auto-calculate startDateTime and endDateTime from days
            EventDayEntity firstDay = eventDays.getFirst();
            EventDayEntity lastDay = eventDays.getLast();

            String timezone = request.getSchedule().getTimezone() != null
                    ? request.getSchedule().getTimezone()
                    : "UTC";

            event.setStartDateTime(ZonedDateTime.of(
                    firstDay.getDate(),
                    firstDay.getStartTime(),
                    ZoneId.of(timezone)
            ));

            event.setEndDateTime(ZonedDateTime.of(
                    lastDay.getDate(),
                    lastDay.getEndTime(),
                    ZoneId.of(timezone)
            ));

            event.setTimezone(timezone);

            log.debug("Added {} days to event, start: {}, end: {}",
                    eventDays.size(), event.getStartDateTime(), event.getEndDateTime());
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


    private void markCompletedStagesForDraft(EventEntity event, CreateEventRequest request) {
        List<String> completedStages = new ArrayList<>();

        completedStages.add(EventCreationStage.BASIC_INFO.name());

        if (request.getSchedule() != null &&
                request.getSchedule().getDays() != null &&
                !request.getSchedule().getDays().isEmpty()) {
            completedStages.add(EventCreationStage.SCHEDULE.name());
        }

        if (isLocationComplete(request)) {
            completedStages.add(EventCreationStage.LOCATION_DETAILS.name());
        }

        if (request.getMedia() != null &&
                (request.getMedia().getBanner() != null || request.getMedia().getThumbnail() != null)) {
            completedStages.add(EventCreationStage.MEDIA.name());
        }

        if ((request.getLinkedProductIds() != null && !request.getLinkedProductIds().isEmpty()) ||
                (request.getLinkedShopIds() != null && !request.getLinkedShopIds().isEmpty())) {
            completedStages.add(EventCreationStage.LINKS.name());
        }

        event.setCompletedStages(completedStages);
        event.setCurrentStage(determineCurrentStage(completedStages));
    }

    private boolean isLocationComplete(CreateEventRequest request) {
        EventFormat format = request.getEventFormat();

        if (format == EventFormat.IN_PERSON) {
            return request.getVenue() != null && request.getVenue().getName() != null;
        } else if (format == EventFormat.ONLINE) {
            return request.getVirtualDetails() != null && request.getVirtualDetails().getMeetingLink() != null;
        } else if (format == EventFormat.HYBRID) {
            return request.getVenue() != null && request.getVenue().getName() != null &&
                    request.getVirtualDetails() != null && request.getVirtualDetails().getMeetingLink() != null;
        }

        return false;
    }

    private EventCreationStage determineCurrentStage(List<String> completedStages) {
        if (!completedStages.contains(EventCreationStage.BASIC_INFO.name())) {
            return EventCreationStage.BASIC_INFO;
        }
        if (!completedStages.contains(EventCreationStage.SCHEDULE.name())) {
            return EventCreationStage.SCHEDULE;
        }
        if (!completedStages.contains(EventCreationStage.LOCATION_DETAILS.name())) {
            return EventCreationStage.LOCATION_DETAILS;
        }
        if (!completedStages.contains(EventCreationStage.TICKETS.name())) {
            return EventCreationStage.TICKETS;
        }

        return EventCreationStage.REVIEW;
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
        // Only create coordinates if they exist
        if (request.getCoordinates() != null &&
                request.getCoordinates().getLatitude() != null &&
                request.getCoordinates().getLongitude() != null) {
            coordinates = Coordinates.builder()
                    .latitude(request.getCoordinates().getLatitude())
                    .longitude(request.getCoordinates().getLongitude())
                    .build();
        }

        return Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .coordinates(coordinates)  // ← Will be null if not provided, that's fine
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

        for (int i = 0; i < dayRequests.size(); i++) {
            EventDayRequest dayRequest = dayRequests.get(i);

            EventDayEntity eventDay = EventDayEntity.builder()
                    .eventEntity(parentEvent)
                    .date(dayRequest.getDate())
                    .startTime(dayRequest.getStartTime())
                    .endTime(dayRequest.getEndTime())
                    .description(dayRequest.getDescription())
                    .dayOrder(dayRequest.getDayOrder() != null ? dayRequest.getDayOrder() : i + 1)
                    .build();

            eventDays.add(eventDay);
        }

        log.debug("Successfully mapped {} event days", eventDays.size());
        return eventDays;
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