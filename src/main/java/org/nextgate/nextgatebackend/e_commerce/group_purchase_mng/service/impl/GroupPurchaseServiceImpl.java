package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.scheduling.JobScheduler;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.ProductCheckoutSessionRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.jobs.ExpireGroupJobRequest;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.utils.GroupPurchaseValidator;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.GroupNotificationMapper;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupPurchaseServiceImpl implements GroupPurchaseService {

    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final GroupParticipantRepo groupParticipantRepo;
    private final ProductRepo productRepo;
    private final AccountRepo accountRepo;
    private final GroupPurchaseValidator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductCheckoutSessionRepo checkoutSessionRepo;
    private final NotificationPublisher notificationPublisher;
    private final JobScheduler jobScheduler;

    @Override
    @Transactional
    public void createGroupInstance(
            ProductCheckoutSessionEntity checkoutSession)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   CREATING GROUP INSTANCE                             ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Checkout Session: {}", checkoutSession.getSessionId());

        // 1. Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // 2. Validate checkout session
        validator.validateCheckoutSessionForGroupCreation(checkoutSession, authenticatedUser);

        // 3. Extract data
        ProductCheckoutSessionEntity.CheckoutItem item = checkoutSession.getItems().getFirst();
        UUID productId = item.getProductId();
        Integer quantity = item.getQuantity();
        BigDecimal totalPaid = checkoutSession.getPricing().getTotal();
        AccountEntity customer = checkoutSession.getCustomer();

        // 4. Fetch and validate product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        validator.validateProductForGroupBuying(product);
        validator.validateQuantityForGroupBuying(quantity, product);

        // 5. Create group instance
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(product.getGroupTimeLimitHours());

        GroupPurchaseInstanceEntity group = GroupPurchaseInstanceEntity.builder()
                .product(product)
                .shop(product.getShop())
                .initiator(customer)
                .participants(new ArrayList<>())
                .productName(product.getProductName())
                .productImage(product.getProductImages() != null && !product.getProductImages().isEmpty()
                        ? product.getProductImages().getFirst() : null)
                .regularPrice(product.getPrice())
                .groupPrice(product.getGroupPrice())
                .totalSeats(product.getGroupMaxSize())
                .maxPerCustomer(product.getMaxPerCustomer())
                .durationHours(product.getGroupTimeLimitHours())
                .seatsOccupied(quantity)
                .totalParticipants(1)
                .status(GroupStatus.OPEN)
                .createdAt(now)
                .expiresAt(expiresAt)
                .updatedAt(now)
                .isDeleted(false)
                .priority(0)
                .build();

        // 6. Save group
        GroupPurchaseInstanceEntity savedGroup = groupPurchaseInstanceRepo.save(group);
        log.info("✓ Group created: {} ({})", savedGroup.getGroupCode(), savedGroup.getGroupInstanceId());

        // 7. Schedule expiration job
        scheduleExpirationJob(savedGroup);

        // 8. Link checkout session to group
        checkoutSession.setGroupIdToBeJoined(savedGroup.getGroupInstanceId());
        checkoutSessionRepo.save(checkoutSession);
        log.info("✓ Checkout session linked to group");

        // 9. Create participant with purchase history
        GroupParticipantEntity participant = GroupParticipantEntity.builder()
                .groupInstance(savedGroup)
                .user(customer)
                .quantity(0)
                .totalPaid(BigDecimal.ZERO)
                .checkoutSessionId(checkoutSession.getSessionId())
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(now)
                .transferHistory(new ArrayList<>())
                .purchaseHistory(new ArrayList<>())
                .build();

        // Add purchase record
        participant.addPurchaseRecord(
                checkoutSession.getSessionId(),
                quantity,
                totalPaid,
                null
        );

        groupParticipantRepo.save(participant);
        log.info("✓ Participant created");

        // 10. ✅ CHECK COMPLETION BEFORE TRANSACTION ENDS
        boolean isGroupFull = checkAndCompleteGroupInSameTransaction(savedGroup);

        log.info("Group Status: {}", savedGroup.getStatus());
        log.info("  Seats: {}/{}", savedGroup.getSeatsOccupied(), savedGroup.getTotalSeats());
        log.info("  Full: {}", isGroupFull);

        // 11. Send notification (only if group NOT completed)
        if (!isGroupFull) {
            sendGroupCreatedNotification(savedGroup, customer, quantity);
        }

        log.info("╚════════════════════════════════════════════════════════╝");

        // Transaction commits here - all data persisted
    }

    @Override
    @Transactional
    public void joinGroup(
            ProductCheckoutSessionEntity checkoutSession)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   JOINING GROUP                                       ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Checkout Session: {}", checkoutSession.getSessionId());
        log.info("Target Group: {}", checkoutSession.getGroupIdToBeJoined());

        // 1. Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // 2. Validate checkout session
        validator.validateCheckoutSessionForGroupCreation(checkoutSession, authenticatedUser);

        // 3. Extract data
        ProductCheckoutSessionEntity.CheckoutItem item = checkoutSession.getItems().get(0);
        UUID productId = item.getProductId();
        Integer quantity = item.getQuantity();
        BigDecimal totalPaid = checkoutSession.getPricing().getTotal();
        AccountEntity customer = checkoutSession.getCustomer();

        // 4. Fetch group
        GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                .findById(checkoutSession.getGroupIdToBeJoined())
                .orElseThrow(() -> new ItemNotFoundException("Group not found"));

        // 5. Validate group is joinable
        validator.validateGroupIsJoinable(group);

        // 6. Validate product matches
        if (!group.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException("Product mismatch with group");
        }

        // 7. Fetch and validate product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        validator.validateProductForGroupBuying(product);
        validator.validateQuantityForGroupBuying(quantity, product);
        validator.validateSeatsAvailable(group, quantity);

        // 8. Check if user already in group
        Optional<GroupParticipantEntity> existingParticipant =
                groupParticipantRepo.findByUserAndGroupInstance(customer, group);

        LocalDateTime now = LocalDateTime.now();

        if (existingParticipant.isPresent()) {
            // User already in group - add more seats
            GroupParticipantEntity participant = existingParticipant.get();

            if (participant.getStatus() != ParticipantStatus.ACTIVE) {
                throw new BadRequestException("Your participation status is: " + participant.getStatus());
            }

            log.info("User already in group. Adding {} more seats.", quantity);

            // Add purchase record
            participant.addPurchaseRecord(
                    checkoutSession.getSessionId(),
                    quantity,
                    totalPaid,
                    null
            );

            groupParticipantRepo.save(participant);
            log.info("✓ Participant updated");

        } else {
            // New participant
            log.info("Creating new participant");

            GroupParticipantEntity participant = GroupParticipantEntity.builder()
                    .groupInstance(group)
                    .user(customer)
                    .quantity(0)
                    .totalPaid(BigDecimal.ZERO)
                    .checkoutSessionId(checkoutSession.getSessionId())
                    .status(ParticipantStatus.ACTIVE)
                    .joinedAt(now)
                    .transferHistory(new ArrayList<>())
                    .purchaseHistory(new ArrayList<>())
                    .build();

            // Add purchase record
            participant.addPurchaseRecord(
                    checkoutSession.getSessionId(),
                    quantity,
                    totalPaid,
                    null
            );

            groupParticipantRepo.save(participant);

            // Increment participant count
            group.setTotalParticipants(group.getTotalParticipants() + 1);
            log.info("✓ New participant created");
        }

        // 9. Update group seats
        group.setSeatsOccupied(group.getSeatsOccupied() + quantity);
        group.setUpdatedAt(now);

        log.info("Group seats updated: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());

        // 10. Save group
        GroupPurchaseInstanceEntity savedGroup = groupPurchaseInstanceRepo.save(group);

        // 11. ✅ CHECK COMPLETION BEFORE TRANSACTION ENDS
        boolean isGroupFull = checkAndCompleteGroupInSameTransaction(savedGroup);

        log.info("Group Status: {}", savedGroup.getStatus());
        log.info("  Seats: {}/{}", savedGroup.getSeatsOccupied(), savedGroup.getTotalSeats());
        log.info("  Full: {}", isGroupFull);

        // 12. Send notifications (only if NOT completed)
        if (!isGroupFull) {
            sendMemberJoinedNotifications(savedGroup, customer, quantity);
        }

        log.info("╚════════════════════════════════════════════════════════╝");

        // Transaction commits here
    }

    @Override
    @Transactional
    public GroupParticipantEntity transferToGroup(
            UUID sourceGroupId,
            UUID targetGroupId,
            Integer quantity) throws ItemNotFoundException, BadRequestException {

        log.info("Transferring {} seats from group {} to group {}",
                quantity, sourceGroupId, targetGroupId);

        // 1. Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        log.debug("Authenticated user: {}", authenticatedUser.getAccountId());

        // 2. Validate quantity
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        // 3. Validate source and target are different
        if (sourceGroupId.equals(targetGroupId)) {
            throw new BadRequestException("Source and target groups must be different");
        }

        // 4. Fetch source group
        GroupPurchaseInstanceEntity sourceGroup = groupPurchaseInstanceRepo
                .findById(sourceGroupId)
                .orElseThrow(() -> new ItemNotFoundException("Source group not found"));

        // 5. Fetch target group
        GroupPurchaseInstanceEntity targetGroup = groupPurchaseInstanceRepo
                .findById(targetGroupId)
                .orElseThrow(() -> new ItemNotFoundException("Target group not found"));

        // 6. Validate target group is joinable
        validator.validateGroupIsJoinable(targetGroup);

        // 7. Validate transfer compatibility
        validator.validateGroupTransfer(sourceGroup, targetGroup, quantity);

        // 8. Find user's participation in source group
        GroupParticipantEntity sourceParticipant = groupParticipantRepo
                .findByUserAndGroupInstance(authenticatedUser, sourceGroup)
                .orElseThrow(() -> new ItemNotFoundException(
                        "You are not a participant in the source group"
                ));

        // 9. Validate participant is ACTIVE
        if (sourceParticipant.getStatus() != ParticipantStatus.ACTIVE) {
            throw new BadRequestException(
                    "Cannot transfer. Your participation status is not active in this source group"
            );
        }

        // 10. Validate user has enough seats to transfer
        if (sourceParticipant.getQuantity() < quantity) {
            throw new BadRequestException(
                    String.format("Not enough seats to transfer. You have: %d, requested: %d",
                            sourceParticipant.getQuantity(), quantity)
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // 11. Check if user already in target group
        Optional<GroupParticipantEntity> existingTargetParticipant =
                groupParticipantRepo.findByUserAndGroupInstance(authenticatedUser, targetGroup);

        GroupParticipantEntity targetParticipant;

        if (existingTargetParticipant.isPresent()) {
            // User already in target - merge seats
            targetParticipant = existingTargetParticipant.get();

            if (targetParticipant.getStatus() != ParticipantStatus.ACTIVE) {
                throw new BadRequestException(
                        "Cannot transfer. Your target participation status is: " +
                                targetParticipant.getStatus()
                );
            }

            log.info("User already in target group. Merging {} seats.",
                    quantity);

            // Validate maxPerCustomer after merge
            if (targetGroup.getMaxPerCustomer() != null && targetGroup.getMaxPerCustomer() > 0) {
                int totalAfterMerge = targetParticipant.getQuantity() + quantity;
                if (totalAfterMerge > targetGroup.getMaxPerCustomer()) {
                    throw new BadRequestException(
                            String.format("Transfer would exceed max per customer (%d). " +
                                            "Current: %d, Transfer: %d, Total: %d",
                                    targetGroup.getMaxPerCustomer(),
                                    targetParticipant.getQuantity(),
                                    quantity,
                                    totalAfterMerge)
                    );
                }
            }

            // Update existing target participant
            targetParticipant.setQuantity(targetParticipant.getQuantity() + quantity);
            targetParticipant.setTransferredAt(now);

            // Add transfer record to target
            targetParticipant.addTransferHistory(
                    sourceGroupId,
                    targetGroupId,
                    String.format("Transferred %d seats from group %s",
                            quantity, sourceGroup.getGroupCode())
            );

        } else {
            // User not in target - create new participant
            log.info("User not in target group. Creating new participation.");

            targetParticipant = GroupParticipantEntity.builder()
                    .groupInstance(targetGroup)
                    .user(authenticatedUser)
                    .quantity(quantity)
                    .totalPaid(BigDecimal.ZERO)  // No payment for transfer
                    .checkoutSessionId(sourceParticipant.getCheckoutSessionId())  // Keep original
                    .status(ParticipantStatus.ACTIVE)
                    .joinedAt(now)
                    .transferHistory(new ArrayList<>())
                    .purchaseHistory(new ArrayList<>())
                    .build();

            // Add transfer record
            targetParticipant.addTransferHistory(
                    sourceGroupId,
                    targetGroupId,
                    String.format("Transferred %d seats from group %s",
                            quantity, sourceGroup.getGroupCode())
            );

            // Increment target group participants count
            targetGroup.setTotalParticipants(targetGroup.getTotalParticipants() + 1);
        }

        // 12. Update source participant
        int remainingSeats = sourceParticipant.getQuantity() - quantity;

        if (remainingSeats == 0) {
            // Transferring all seats - mark as TRANSFERRED_OUT
            log.info("User transferring all {} seats. Marking source participant as TRANSFERRED_OUT.",
                    quantity);

            sourceParticipant.setStatus(ParticipantStatus.TRANSFERRED_OUT);
            sourceParticipant.setQuantity(0);

            // Decrement source group participants count
            sourceGroup.setTotalParticipants(
                    Math.max(0, sourceGroup.getTotalParticipants() - 1)
            );

        } else {
            // Partial transfer - reduce quantity
            log.info("User transferring {} seats, keeping {} seats in source.",
                    quantity, remainingSeats);

            sourceParticipant.setQuantity(remainingSeats);
        }

        // Add transfer record to source
        sourceParticipant.addTransferHistory(
                sourceGroupId,
                targetGroupId,
                String.format("Transferred %d seats to group %s",
                        quantity, targetGroup.getGroupCode())
        );

        sourceParticipant.setTransferredAt(now);

        // 13. Update source group seats
        sourceGroup.setSeatsOccupied(sourceGroup.getSeatsOccupied() - quantity);
        sourceGroup.setUpdatedAt(now);

        // 14. Check if source group is now empty - soft delete
        if (sourceGroup.getSeatsOccupied() == 0) {
            log.info("Source group {} is now empty. Soft deleting.",
                    sourceGroupId);

            sourceGroup.setIsDeleted(true);
            sourceGroup.setStatus(GroupStatus.DELETED);
            sourceGroup.setDeletedAt(now);
            sourceGroup.setDeletedBy(authenticatedUser.getAccountId());
            sourceGroup.setDeleteReason("All participants transferred out");
        }

        // 15. Update target group seats
        targetGroup.setSeatsOccupied(targetGroup.getSeatsOccupied() + quantity);
        targetGroup.setUpdatedAt(now);


        // 17. Save everything
        groupParticipantRepo.save(sourceParticipant);
        groupParticipantRepo.save(targetParticipant);
        groupPurchaseInstanceRepo.save(sourceGroup);
        groupPurchaseInstanceRepo.save(targetGroup);


        boolean isGroupFull = targetGroup.isFull();
        log.info("Transfer complete. Target group full: {} ({}/{})",
                isGroupFull,
                targetGroup.getSeatsOccupied(),
                targetGroup.getTotalSeats());

        sendSeatsTransferredNotification(sourceGroup, targetGroup, authenticatedUser, quantity);

        boolean isTargetFull = checkAndCompleteGroupInSameTransaction(targetGroup);

        log.info("Transfer complete. Target group full: {}", isTargetFull);

        if (!isTargetFull) {
            sendSeatsTransferredNotification(sourceGroup, targetGroup, authenticatedUser, quantity);
        }

        return targetParticipant;

    }


    @Override
    @Transactional(readOnly = true)
    public GroupPurchaseInstanceEntity getGroupById(UUID groupInstanceId)
            throws ItemNotFoundException {

        log.debug("Fetching group by ID: {}", groupInstanceId);

        return groupPurchaseInstanceRepo.findById(groupInstanceId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Group not found with ID: " + groupInstanceId
                ));
    }


    @Override
    @Transactional(readOnly = true)
    public GroupPurchaseInstanceEntity getGroupByCode(String groupCode)
            throws ItemNotFoundException {

        log.debug("Fetching group by code: {}", groupCode);

        return groupPurchaseInstanceRepo.findByGroupCode(groupCode)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Group not found with code: " + groupCode
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupPurchaseInstanceEntity> getAvailableGroupsForProduct(
            ProductEntity product) {
        log.debug("Fetching available groups for product: {}", product.getProductId());

        // Get OPEN groups for this product
        List<GroupPurchaseInstanceEntity> openGroups =
                groupPurchaseInstanceRepo.findByProductAndStatusAndIsDeletedFalseOrderByCreatedAtDesc(
                        product, GroupStatus.OPEN
                );

        // Filter: not expired, not full
        List<GroupPurchaseInstanceEntity> availableGroups = openGroups.stream()
                .filter(group -> !group.isExpired())
                .filter(group -> !group.isFull())
                .sorted((g1, g2) -> g1.getCreatedAt().compareTo(g2.getCreatedAt())) // Oldest first
                .toList();

        log.debug("Found {} available groups for product: {}",
                availableGroups.size(), product.getProductId());

        return availableGroups;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupPurchaseInstanceEntity> getGroupsUserBelongsTo(
            AccountEntity user,
            GroupStatus status
    ) {
        log.debug("Fetching groups for user: {}, status: {}", user.getAccountId(), status);

        // Get all active participations for user
        List<GroupParticipantEntity> participations =
                groupParticipantRepo.findByUserAndStatusOrderByJoinedAtDesc(
                        user, ParticipantStatus.ACTIVE
                );

        // Extract groups from participations
        List<GroupPurchaseInstanceEntity> groups = participations.stream()
                .map(GroupParticipantEntity::getGroupInstance)
                .filter(group -> !group.getIsDeleted()) // Exclude deleted groups
                .filter(group -> status == null || group.getStatus() == status) // Filter by status if provided
                .sorted((g1, g2) -> g2.getCreatedAt().compareTo(g1.getCreatedAt())) // Newest first
                .toList();

        log.debug("Found {} groups for user: {}", groups.size(), user.getAccountId());

        return groups;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupParticipantEntity> getMyActiveParticipations(AccountEntity user) {

        log.debug("Fetching active participations for user: {}", user.getAccountId());

        List<GroupParticipantEntity> participations =
                groupParticipantRepo.findByUserAndStatusOrderByJoinedAtDesc(
                        user, ParticipantStatus.ACTIVE
                );

        log.debug("Found {} active participations for user: {}",
                participations.size(), user.getAccountId());

        return participations;
    }


    @Override
    @Transactional(readOnly = true)
    public List<GroupParticipantEntity> getGroupParticipants(
            GroupPurchaseInstanceEntity group
    ) {
        log.debug("Fetching participants for group: {}", group.getGroupInstanceId());

        List<GroupParticipantEntity> participants =
                groupParticipantRepo.findByGroupInstanceOrderByJoinedAtAsc(group);

        log.debug("Found {} participants for group: {}",
                participants.size(), group.getGroupInstanceId());

        return participants;
    }


    @Override
    @Transactional(readOnly = true)
    public boolean canTransferToGroup(
            AccountEntity user,
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup
    ) {
        log.debug("Checking if user can transfer from {} to {}",
                sourceGroup.getGroupInstanceId(), targetGroup.getGroupInstanceId());

        try {
            // Check if user is in source group
            GroupParticipantEntity sourceParticipant =
                    groupParticipantRepo.findByUserAndGroupInstance(user, sourceGroup)
                            .orElse(null);

            if (sourceParticipant == null ||
                    sourceParticipant.getStatus() != ParticipantStatus.ACTIVE) {
                return false;
            }

            // Validate target group is joinable
            if (targetGroup.getStatus() != GroupStatus.OPEN ||
                    targetGroup.isExpired() ||
                    targetGroup.getIsDeleted()) {
                return false;
            }

            // Validate compatibility
            if (!sourceGroup.getProduct().getProductId()
                    .equals(targetGroup.getProduct().getProductId())) {
                return false;
            }

            if (!sourceGroup.getShop().getShopId()
                    .equals(targetGroup.getShop().getShopId())) {
                return false;
            }

            if (sourceGroup.getGroupPrice().compareTo(targetGroup.getGroupPrice()) != 0) {
                return false;
            }

            // Check target has space
            if (targetGroup.getSeatsRemaining() < sourceParticipant.getQuantity()) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking transfer eligibility", e);
            return false;
        }
    }


    @Override
    @Transactional(readOnly = true)
    public boolean canJoinGroup(
            GroupPurchaseInstanceEntity group,
            AccountEntity user
    ) {
        log.debug("Checking if user {} can join group {}",
                user.getAccountId(), group.getGroupInstanceId());

        try {
            // Check group is joinable
            if (group.getStatus() != GroupStatus.OPEN) {
                return false;
            }

            if (group.isExpired()) {
                return false;
            }

            if (group.getIsDeleted() != null && group.getIsDeleted()) {
                return false;
            }

            if (group.isFull()) {
                return false;
            }

            // User CAN join even if already a member (to add more seats)
            // So we return true here

            return true;

        } catch (Exception e) {
            log.error("Error checking join eligibility", e);
            return false;
        }
    }


    // Helper method
    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }


    /**
     * Checks if group is full and handles completion.
     * Publishes GroupCompletedEvent if group just became full.
     * <p>
     * IMPORTANT: This method is idempotent - safe to call multiple times.
     * <p>
     * Call this after any operation that changes seatsOccupied:
     * - createGroupInstance()
     * - joinGroup()
     * - transferToGroup()
     */
    private void checkAndCompleteGroup(GroupPurchaseInstanceEntity group) {

        // ========================================
        // 1. CHECK IF GROUP IS FULL
        // ========================================
        if (!group.isFull()) {
            log.debug("Group {} not full yet: {}/{}",
                    group.getGroupCode(),
                    group.getSeatsOccupied(),
                    group.getTotalSeats());
            return;
        }

        // ========================================
        // 2. CHECK IF ALREADY PROCESSED (IDEMPOTENCY)
        // ========================================
        // Allow processing if:
        // - Status is OPEN (group just became full)
        // - Status is COMPLETED but completedAt is null (edge case: status set but not saved)

        if (group.getStatus() == GroupStatus.COMPLETED && group.getCompletedAt() != null) {
            // Already completed AND saved - event already published
            log.debug("Group {} already completed and processed at {}, skipping",
                    group.getGroupCode(), group.getCompletedAt());
            return;
        }

        if (group.getStatus() != GroupStatus.OPEN && group.getStatus() != GroupStatus.COMPLETED) {
            // Status is FAILED or DELETED - don't process
            log.debug("Group {} has status {}, cannot complete",
                    group.getGroupCode(), group.getStatus());
            return;
        }

        // ========================================
        // 3. LOG COMPLETION
        // ========================================
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         GROUP COMPLETED                                ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", group.getGroupInstanceId());
        log.info("Group Code: {}", group.getGroupCode());
        log.info("Product: {}", group.getProductName());
        log.info("Participants: {}", group.getTotalParticipants());
        log.info("Total Seats: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());
        log.info("Previous Status: {}", group.getStatus());

        // ========================================
        // 4. MARK AS COMPLETED
        // ========================================
        LocalDateTime now = LocalDateTime.now();
        group.setStatus(GroupStatus.COMPLETED);
        group.setCompletedAt(now);
        group.setUpdatedAt(now);

        // ========================================
        // 5. SAVE IMMEDIATELY
        // ========================================
        try {
            groupPurchaseInstanceRepo.save(group);
            log.info("✓ Group marked as COMPLETED");
            log.info("  Completed At: {}", now);

        } catch (Exception e) {
            log.error("Failed to save completed group", e);
            // This is critical - if we can't save, don't publish event
            return;
        }

        // ========================================
        // 6. PUBLISH EVENT FOR ASYNC PROCESSING
        // ========================================
        try {
            GroupCompletedEvent event = new GroupCompletedEvent(
                    this,
                    group.getGroupInstanceId(),
                    group,
                    now
            );

            eventPublisher.publishEvent(event);

            log.info("✓ GroupCompletedEvent published");
            log.info("  Event will trigger:");
            log.info("    - Order creation for {} participants",
                    group.getTotalParticipants());
            log.info("    - Notification to all participants");
            log.info("    - Analytics tracking");

        } catch (Exception e) {
            log.error("Failed to publish GroupCompletedEvent", e);

            // Don't throw - group is still marked as completed
            // Event listeners can pick this up via scheduled job

            // TODO: Create admin task for manual intervention
            log.error("⚠️  MANUAL ACTION REQUIRED:");
            log.error("   Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
            log.error("   Status: COMPLETED but event not published");
            log.error("   Action: Run scheduled job to process completed groups");
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   GROUP COMPLETION PROCESSING COMPLETE                ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


    /**
     * Check if group is complete and publish event if needed.
     * Called from ProductPaymentCompletedListener AFTER transaction commits.
     *
     * This is now a SAFETY NET for edge cases.
     * Normal flow should complete the group in createGroupInstance()/joinGroup()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndPublishGroupCompletion(UUID groupInstanceId) {

        log.info("Safety check for group completion: {}", groupInstanceId);

        try {
            // Fetch group in NEW transaction
            GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                    .findById(groupInstanceId)
                    .orElse(null);

            if (group == null) {
                log.warn("Group not found: {}", groupInstanceId);
                return;
            }

            log.info("Group status: {}, Seats: {}/{}, Full: {}",
                    group.getStatus(),
                    group.getSeatsOccupied(),
                    group.getTotalSeats(),
                    group.isFull());

            // Check and complete (idempotent)
            checkAndCompleteGroupInSameTransaction(group);

        } catch (Exception e) {
            log.error("Error in safety completion check for group: {}", groupInstanceId, e);
        }
    }


    /**
     * Send notification to shop owner when new group is created
     */
    private void sendGroupCreatedNotification(
            GroupPurchaseInstanceEntity group,
            AccountEntity creator,
            Integer initialSeats) {

        log.info("📧 Sending group creation notification to shop owner");

        AccountEntity shopOwner = group.getShop().getOwner();

        if (shopOwner == null) {
            log.warn("⚠️ Cannot send notification - shop has no owner");
            return;
        }

        // 1. Prepare notification data using EventCategoryMapper
        Map<String, Object> data = GroupNotificationMapper.mapNewGroupCreated(
                group, creator, initialSeats);

        // 2. Build recipient (shop owner)
        Recipient recipient = Recipient.builder()
                .userId(shopOwner.getId().toString())
                .email(shopOwner.getEmail())
                .phone(shopOwner.getPhoneNumber())
                .name(shopOwner.getFirstName())
                .language("en")
                .build();

        // 3. Create notification event
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.GROUP_PURCHASE_CREATED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.SMS,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.NORMAL)
                .data(data)
                .build();

        // 4. Publish notification
        notificationPublisher.publish(event);

        log.info("✅ Group creation notification sent to shop owner: {}", shopOwner.getUserName());
    }

    /**
     * Send notifications when member joins group
     * Notifies: 1) Shop owner, 2) Existing group members
     */
    private void sendMemberJoinedNotifications(
            GroupPurchaseInstanceEntity group,
            AccountEntity newMember,
            Integer quantity) {

        log.info("📧 Sending member joined notifications for group: {}", group.getGroupCode());

        // 1. Notify shop owner
        sendMemberJoinedToShopOwner(group, newMember, quantity);

        // 2. Notify existing members
        sendMemberJoinedToExistingMembers(group, newMember, quantity);
    }

    /**
     * Notify shop owner about new member
     */
    private void sendMemberJoinedToShopOwner(
            GroupPurchaseInstanceEntity group,
            AccountEntity newMember,
            Integer quantity) {

        try {
            AccountEntity shopOwner = group.getShop().getOwner();

            if (shopOwner == null) {
                log.warn("⚠️ Cannot notify shop owner - shop has no owner");
                return;
            }

            // Prepare data
            Map<String, Object> data = GroupNotificationMapper.mapMemberJoinedForShopOwner(
                    group, newMember, quantity);

            // Build recipient
            Recipient recipient = Recipient.builder()
                    .userId(shopOwner.getId().toString())
                    .email(shopOwner.getEmail())
                    .phone(shopOwner.getPhoneNumber())
                    .name(shopOwner.getFirstName())
                    .language("en")
                    .build();

            // Create event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.GROUP_MEMBER_JOINED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // Publish
            notificationPublisher.publish(event);

            log.info("✅ Shop owner notified about new member: {}", newMember.getUserName());

        } catch (Exception e) {
            log.error("Failed to notify shop owner about new member", e);
        }
    }

    /**
     * Notify existing members about new member joining
     */
    private void sendMemberJoinedToExistingMembers(
            GroupPurchaseInstanceEntity group,
            AccountEntity newMember,
            Integer quantity) {

        try {
            UUID newMemberId = newMember.getId(); // Get ID once

            // Get all active participants except the new member
            List<GroupParticipantEntity> existingParticipants =
                    groupParticipantRepo.findByGroupInstanceOrderByJoinedAtAsc(group)
                            .stream()
                            .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                            .filter(p -> {
                                UUID participantUserId = p.getUser().getId();
                                boolean isNewMember = participantUserId.equals(newMemberId);
                                if (isNewMember) {
                                    log.debug("Excluding new member from notification: {}", p.getUser().getUserName());
                                }
                                return !isNewMember; // Exclude if it's the new member
                            })
                            .toList();

            log.info("Found {} existing members (excluding new member)", existingParticipants.size());

            if (existingParticipants.isEmpty()) {
                log.info("No existing members to notify (first member in group)");
                return;
            }

            log.info("Notifying {} existing members about new join", existingParticipants.size());

            // Send notification to each existing member
            for (GroupParticipantEntity participant : existingParticipants) {
                try {
                    log.info("Notifying existing member: {}", participant.getUser().getUserName());
                    sendMemberJoinedToExistingMember(group, participant.getUser(), newMember, quantity);
                } catch (Exception e) {
                    log.error("Failed to notify existing member: {}",
                            participant.getUser().getUserName(), e);
                }
            }

            log.info("✅ Existing members notified about new join");

        } catch (Exception e) {
            log.error("Failed to notify existing members", e);
        }
    }

    /**
     * Send notification to a single existing member
     */
    private void sendMemberJoinedToExistingMember(
            GroupPurchaseInstanceEntity group,
            AccountEntity existingMember,
            AccountEntity newMember,
            Integer quantity) {

        // Prepare data
        Map<String, Object> data = GroupNotificationMapper.mapMemberJoinedForExistingMembers(
                group, existingMember, newMember, quantity);

        // Build recipient
        Recipient recipient = Recipient.builder()
                .userId(existingMember.getId().toString())
                .email(existingMember.getEmail())
                .phone(existingMember.getPhoneNumber())
                .name(existingMember.getFirstName())
                .language("en")
                .build();

        // Create event
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.GROUP_MEMBER_JOINED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.LOW)
                .data(data)
                .build();

        // Publish
        notificationPublisher.publish(event);
    }

    /**
     * Send notification when seats are transferred between groups
     */
    private void sendSeatsTransferredNotification(
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup,
            AccountEntity user,
            Integer quantity) {

        log.info("📧 Sending seats transferred notification to user: {}", user.getUserName());

        // 1. Prepare notification data using EventCategoryMapper
        Map<String, Object> data = GroupNotificationMapper.mapSeatsTransferred(
                sourceGroup, targetGroup, user, quantity);

        // 2. Build recipient (the user who transferred)
        Recipient recipient = Recipient.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .name(user.getFirstName())
                .language("en")
                .build();

        // 3. Create notification event
        NotificationEvent event = NotificationEvent.builder()
                .type(NotificationType.GROUP_SEATS_TRANSFERRED)
                .recipients(List.of(recipient))
                .channels(List.of(
                        NotificationChannel.EMAIL,
                        NotificationChannel.PUSH,
                        NotificationChannel.IN_APP
                ))
                .priority(NotificationPriority.NORMAL)
                .data(data)
                .build();

        // 4. Publish notification
        notificationPublisher.publish(event);

        log.info("✅ Seats transferred notification sent to: {}", user.getUserName());
    }



    private void scheduleExpirationJob(GroupPurchaseInstanceEntity group) {

        LocalDateTime expiresAt = group.getExpiresAt();
        UUID groupId = group.getGroupInstanceId();

        log.info("════════════════════════════════════════════════════════");
        log.info("SCHEDULING EXPIRATION JOB");
        log.info("Group ID: {}", groupId);
        log.info("Group Code: {}", group.getGroupCode());
        log.info("Expires At: {}", expiresAt);
        log.info("════════════════════════════════════════════════════════");

        try {
            // Convert to Instant
            Instant expirationInstant = expiresAt
                    .atZone(ZoneId.systemDefault())
                    .toInstant();

            log.info("Expiration instant (UTC): {}", expirationInstant);

            // ✅ USE BackgroundJobRequest.schedule() for JobRequest pattern
            ExpireGroupJobRequest jobRequest = new ExpireGroupJobRequest(groupId);
            JobId jobId = BackgroundJobRequest.schedule(expirationInstant, jobRequest);

            log.info("✅ SUCCESS - Job scheduled!");
            log.info("   JobId: {}", jobId);
            log.info("   Method: ExpireGroupJobRequest");
            log.info("   Group ID: {}", groupId);

            // Store jobId in metadata
            if (group.getMetadata() == null) {
                group.setMetadata(new HashMap<>());
            }
            group.getMetadata().put("expirationJobId", jobId.toString());
            group.getMetadata().put("expirationScheduledAt", LocalDateTime.now().toString());
            groupPurchaseInstanceRepo.save(group);

            log.info("✅ JobId stored in group metadata");

        } catch (Exception e) {
            log.error("❌ FAILED TO SCHEDULE EXPIRATION JOB", e);
            log.error("   Group: {}", group.getGroupCode());
            log.error("   Error: {}", e.getMessage());
            throw e;
        }

        log.info("════════════════════════════════════════════════════════");
    }


    /**
     * Check and complete group within the SAME transaction.
     * This prevents race conditions.
     *
     * @return true if group is full and completed, false otherwise
     */
    private boolean checkAndCompleteGroupInSameTransaction(GroupPurchaseInstanceEntity group) {

        // Check if group is full
        if (!group.isFull()) {
            log.debug("Group not full yet: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());
            return false;
        }

        // Check if already processed (idempotency)
        if (group.getStatus() == GroupStatus.COMPLETED && group.getCompletedAt() != null) {
            log.debug("Group already completed at {}", group.getCompletedAt());
            return true;
        }

        if (group.getStatus() != GroupStatus.OPEN && group.getStatus() != GroupStatus.COMPLETED) {
            log.debug("Group status is {}, cannot complete", group.getStatus());
            return false;
        }

        // Mark as completed
        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   GROUP COMPLETED!                                    ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group: {} ({})", group.getGroupCode(), group.getGroupInstanceId());
        log.info("Participants: {}", group.getTotalParticipants());
        log.info("Seats: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());

        LocalDateTime now = LocalDateTime.now();
        group.setStatus(GroupStatus.COMPLETED);
        group.setCompletedAt(now);
        group.setUpdatedAt(now);

        // Save immediately
        groupPurchaseInstanceRepo.save(group);
        log.info("✓ Group marked as COMPLETED");

        // Publish event (will be processed AFTER transaction commits)
        try {
            GroupCompletedEvent event = new GroupCompletedEvent(
                    this,
                    group.getGroupInstanceId(),
                    group,
                    now
            );

            eventPublisher.publishEvent(event);
            log.info("✓ GroupCompletedEvent published");

        } catch (Exception e) {
            log.error("Failed to publish GroupCompletedEvent", e);
        }

        return true;
    }
}