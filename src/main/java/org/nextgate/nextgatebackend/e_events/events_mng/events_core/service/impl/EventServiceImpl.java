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
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.payloads.*;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.service.EventsService;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.DuplicateValidationResult;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.EventDuplicateValidator;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations.EventValidations;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeyService;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeys;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.EventValidationException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final RSAKeyService rsaKeyService;

    @Override
    @Transactional
    public EventEntity createEventDraft(CreateEventDraftRequest request)
            throws ItemNotFoundException, EventValidationException {

        AccountEntity currentUser = getAuthenticatedAccount();

        // One draft at a time
        if (eventsRepo.existsByOrganizerAndStatusAndIsDeletedFalse(currentUser, EventStatus.DRAFT)) {
            throw new EventValidationException(
                    "You already have an event draft. Complete or discard it first.",
                    EventCreationStage.BASIC_INFO
            );
        }

        EventsCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EventValidationException(
                        "Category not found", EventCreationStage.BASIC_INFO));

        EventEntity event = EventEntity.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .category(category)
                .eventFormat(request.getEventFormat())
                .eventVisibility(EventVisibility.PUBLIC)
                .organizer(currentUser)
                .createdBy(currentUser)
                .status(EventStatus.DRAFT)
                .currentStage(EventCreationStage.BASIC_INFO)
                .isDeleted(false)
                .completedStages(new ArrayList<>())
                .build();

        event.markStageCompleted(EventCreationStage.BASIC_INFO);

        log.info("Event draft created for user: {}", currentUser.getUserName());
        return eventsRepo.save(event);
    }

    @Override
    public EventEntity getMyCurrentEventDraft() throws ItemNotFoundException {
        AccountEntity currentUser = getAuthenticatedAccount();
        return eventsRepo.findByOrganizerAndStatusAndIsDeletedFalse(currentUser, EventStatus.DRAFT)
                .orElse(null);
    }

    private EventEntity getMyDraftOrThrow() throws ItemNotFoundException {
        EventEntity draft = getMyCurrentEventDraft();
        if (draft == null) {
            throw new ItemNotFoundException("No event draft found. Create a draft first.");
        }
        return draft;
    }

    @Override
    @Transactional
    public void discardEventDraft() throws ItemNotFoundException {
        EventEntity draft = getMyDraftOrThrow();

        draft.getLinkedProducts().clear();
        draft.getLinkedShops().clear();
        draft.getDays().clear();

        eventsRepo.delete(draft);
        log.info("Event draft discarded: {}", draft.getId());
    }

    @Override
    @Transactional
    public EventEntity updateDraftBasicInfo(UpdateEventBasicInfoRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = getMyDraftOrThrow();
        AccountEntity currentUser = getAuthenticatedAccount();

        if (request.getTitle() != null) {
            draft.setTitle(request.getTitle());
            draft.setSlug(generateUniqueSlug(request.getTitle()));
        }
        if (request.getDescription() != null) {
            draft.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            EventsCategoryEntity category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new EventValidationException(
                            "Category not found", EventCreationStage.BASIC_INFO));
            draft.setCategory(category);
        }
        if (request.getEventVisibility() != null) {
            draft.setEventVisibility(request.getEventVisibility());
        }
        if (request.getEventFormat() != null) {
            draft.setEventFormat(request.getEventFormat());
        }

        draft.setUpdatedBy(currentUser);
        draft.markStageCompleted(EventCreationStage.BASIC_INFO);
        draft.setCurrentStage(EventCreationStage.SCHEDULE);

        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity updateDraftSchedule(ScheduleRequest schedule)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = getMyDraftOrThrow();
        AccountEntity currentUser = getAuthenticatedAccount();

        // Clear existing days
        draft.getDays().clear();
        eventsRepo.saveAndFlush(draft);

        if (schedule.getDays() != null && !schedule.getDays().isEmpty()) {
            List<EventDayEntity> eventDays = mapEventDays(schedule.getDays(), draft);
            draft.setDays(eventDays);

            EventDayEntity firstDay = eventDays.getFirst();
            EventDayEntity lastDay = eventDays.getLast();

            String timezone = schedule.getTimezone() != null ? schedule.getTimezone() : "UTC";

            draft.setStartDateTime(ZonedDateTime.of(
                    firstDay.getDate(), firstDay.getStartTime(), ZoneId.of(timezone)));
            draft.setEndDateTime(ZonedDateTime.of(
                    lastDay.getDate(), lastDay.getEndTime(), ZoneId.of(timezone)));
            draft.setTimezone(timezone);

            draft.markStageCompleted(EventCreationStage.SCHEDULE);
            draft.setCurrentStage(EventCreationStage.LOCATION_DETAILS);
        }

        draft.setUpdatedBy(currentUser);
        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity updateDraftLocation(UpdateEventLocationRequest request)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = getMyDraftOrThrow();
        AccountEntity currentUser = getAuthenticatedAccount();

        if (request.getVenue() != null) {
            draft.setVenue(mapVenue(request.getVenue()));
        }
        if (request.getVirtualDetails() != null) {
            draft.setVirtualDetails(mapVirtualDetails(request.getVirtualDetails()));
        }

        // Check if location complete based on format
        EventFormat format = draft.getEventFormat();
        boolean complete = switch (format) {
            case IN_PERSON -> draft.getVenue() != null && draft.getVenue().getName() != null;
            case ONLINE -> draft.getVirtualDetails() != null && draft.getVirtualDetails().getMeetingLink() != null;
            case HYBRID -> draft.getVenue() != null && draft.getVenue().getName() != null &&
                    draft.getVirtualDetails() != null && draft.getVirtualDetails().getMeetingLink() != null;
        };

        if (complete) {
            draft.markStageCompleted(EventCreationStage.LOCATION_DETAILS);
            draft.setCurrentStage(EventCreationStage.TICKETS);
        }

        draft.setUpdatedBy(currentUser);
        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity updateDraftMedia(MediaRequest media) throws ItemNotFoundException {
        EventEntity draft = getMyDraftOrThrow();
        AccountEntity currentUser = getAuthenticatedAccount();

        if (media != null) {
            draft.setMedia(mapMedia(media));
            draft.markStageCompleted(EventCreationStage.MEDIA);
        }

        draft.setUpdatedBy(currentUser);
        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity attachProductToDraft(UUID productId)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = getMyDraftOrThrow();

        ProductEntity product = productRepo
                .findByProductIdAndIsDeletedFalseAndStatus(productId, ProductStatus.ACTIVE)
                .orElseThrow(() -> new EventValidationException(
                        "Product not found or not active", EventCreationStage.LINKS));

        boolean exists = draft.getLinkedProducts().stream()
                .anyMatch(p -> p.getProductId().equals(productId));

        if (exists) {
            throw new EventValidationException(
                    "Product already attached to draft", EventCreationStage.LINKS);
        }

        draft.getLinkedProducts().add(product);
        draft.markStageCompleted(EventCreationStage.LINKS);

        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity attachShopToDraft(UUID shopId)
            throws ItemNotFoundException, EventValidationException {

        EventEntity draft = getMyDraftOrThrow();

        ShopEntity shop = shopRepo
                .findByShopIdAndIsDeletedFalseAndStatus(shopId, ShopStatus.ACTIVE)
                .orElseThrow(() -> new EventValidationException(
                        "Shop not found or not active", EventCreationStage.LINKS));

        boolean exists = draft.getLinkedShops().stream()
                .anyMatch(s -> s.getShopId().equals(shopId));

        if (exists) {
            throw new EventValidationException(
                    "Shop already attached to draft", EventCreationStage.LINKS);
        }

        draft.getLinkedShops().add(shop);
        draft.markStageCompleted(EventCreationStage.LINKS);

        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity removeProductFromDraft(UUID productId) throws ItemNotFoundException {
        EventEntity draft = getMyDraftOrThrow();

        boolean removed = draft.getLinkedProducts()
                .removeIf(p -> p.getProductId().equals(productId));

        if (!removed) {
            throw new ItemNotFoundException("Product not attached to draft");
        }

        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity removeShopFromDraft(UUID shopId) throws ItemNotFoundException {
        EventEntity draft = getMyDraftOrThrow();

        boolean removed = draft.getLinkedShops()
                .removeIf(s -> s.getShopId().equals(shopId));

        if (!removed) {
            throw new ItemNotFoundException("Shop not attached to draft");
        }

        return eventsRepo.save(draft);
    }

    @Override
    @Transactional
    public EventEntity publishEvent(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException, EventValidationException {

        try {
            log.info("Publishing event: {}", eventId);

            // Validate input
            if (eventId == null) {
                throw new IllegalArgumentException("Event ID cannot be null");
            }

            AccountEntity currentUser = getAuthenticatedAccount();

            EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                    .orElseThrow(() -> new ItemNotFoundException("Event not found with ID: " + eventId));

            if (!event.getOrganizer().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("Only event organizer can publish the event");
            }

            if (event.getStatus() == EventStatus.PUBLISHED) {
                throw new EventValidationException("Event is already published", EventCreationStage.REVIEW);
            }

            // Validate event before publishing
            eventValidations.validateForPublish(event);

            DuplicateValidationResult duplicateCheck = duplicateValidator.validate(event);

            if (duplicateCheck.isBlocked()) {
                log.warn("Duplicate event detected for user: {}", currentUser.getUserName());
                throw new EventValidationException(
                        duplicateCheck.getMessage(),
                        EventCreationStage.BASIC_INFO
                );
            }

            // Generate RSA keys if needed
            try {
                if (event.getRsaKeys() == null) {
                    log.info("Generating RSA key pair for event: {}", eventId);
                    RSAKeys rsaKeys = rsaKeyService.generateKeys();
                    event.setRsaKeys(rsaKeys);
                    log.info("RSA keys generated successfully for event: {}", eventId);
                } else {
                    log.info("Event already has RSA keys, skipping generation");
                }
            } catch (Exception e) {
                log.error("Error generating RSA keys for event: {}", eventId, e);
                throw new EventValidationException("Failed to generate security keys for the event", EventCreationStage.REVIEW);
            }

            // Update event status and metadata
            event.setStatus(EventStatus.PUBLISHED);
            event.setUpdatedBy(currentUser);

            // Update category count
            EventsCategoryEntity category = event.getCategory();
            category.setEventCount(category.getEventCount() + 1);
            categoryRepository.save(category);

            // Save the event
            EventEntity publishedEvent = eventsRepo.save(event);

            log.info("Event published successfully with ID: {}", eventId);
            return publishedEvent;

        } catch (IllegalArgumentException e) {
            log.error("Invalid argument while publishing event: {}", eventId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while publishing event: {}", eventId, e);
            throw new EventValidationException("Failed to publish event: " + e.getMessage(), EventCreationStage.REVIEW);
        }
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


    @Override
    @Transactional(readOnly = true)
    public Page<EventEntity> getMyEvents(int page, int size) throws ItemNotFoundException {
        log.debug("Fetching events for organizer, page: {}, size: {}", page, size);

        AccountEntity currentUser = getAuthenticatedAccount();
        Pageable pageable = PageRequest.of(page - 1, size);

        return eventsRepo.findByOrganizerAndIsDeletedFalseOrderByCreatedAtDesc(currentUser, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventEntity> getMyEventsByStatus(EventStatus status, int page, int size)
            throws ItemNotFoundException {
        log.debug("Fetching events with status: {}, page: {}, size: {}", status, page, size);

        AccountEntity currentUser = getAuthenticatedAccount();
        Pageable pageable = PageRequest.of(page - 1, size);

        return eventsRepo.findByOrganizerAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(currentUser, status, pageable);
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

        // Add a short UUID to ensure uniqueness
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String slug = baseSlug + "-" + uniqueSuffix;

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