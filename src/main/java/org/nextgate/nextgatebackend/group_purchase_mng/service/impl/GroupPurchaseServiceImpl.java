package org.nextgate.nextgatebackend.group_purchase_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.events.GroupCompletedEvent;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.group_purchase_mng.utils.GroupPurchaseValidator;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    @Transactional
    public GroupPurchaseInstanceEntity createGroupInstance(
            CheckoutSessionEntity checkoutSession) throws ItemNotFoundException, BadRequestException {

        log.info("Creating group instance from checkout session: {}",
                checkoutSession.getSessionId());

        // 1. Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        log.debug("Authenticated user: {}", authenticatedUser.getAccountId());

        // 2. Validate checkout session for group creation
        validator.validateCheckoutSessionForGroupCreation(checkoutSession, authenticatedUser);

        // 3. Extract data from checkout session
        CheckoutSessionEntity.CheckoutItem item = checkoutSession.getItems().get(0);
        UUID productId = item.getProductId();
        Integer quantity = item.getQuantity();
        BigDecimal totalPaid = checkoutSession.getPricing().getTotal();
        AccountEntity customer = checkoutSession.getCustomer();

        log.debug("Extracted data - Product: {}, Quantity: {}, TotalPaid: {}",
                productId, quantity, totalPaid);

        // 4. Fetch product entity
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // 5. Validate product for group buying
        validator.validateProductForGroupBuying(product);

        // 6. Validate quantity
        validator.validateQuantityForGroupBuying(quantity, product);

        // 7. Create group instance with product snapshot
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(product.getGroupTimeLimitHours());

        GroupPurchaseInstanceEntity group = GroupPurchaseInstanceEntity.builder()
                .product(product)
                .shop(product.getShop())
                .initiator(customer)
                .participants(new ArrayList<>())
                .productName(product.getProductName())
                .productImage(product.getProductImages() != null && !product.getProductImages().isEmpty()
                        ? product.getProductImages().get(0) : null)
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

        // 8. Save group
        GroupPurchaseInstanceEntity savedGroup = groupPurchaseInstanceRepo.save(group);

        log.info("Group instance created: {} with code: {}",
                savedGroup.getGroupInstanceId(), savedGroup.getGroupCode());

        // 9. Create participant record with purchase history
        GroupParticipantEntity participant = GroupParticipantEntity.builder()
                .groupInstance(savedGroup)
                .user(customer)
                .quantity(0)  // Start at 0, will be updated by addPurchaseRecord()
                .totalPaid(BigDecimal.ZERO)  // Start at 0, will be updated by addPurchaseRecord()
                .checkoutSessionId(checkoutSession.getSessionId())
                .status(ParticipantStatus.ACTIVE)
                .joinedAt(now)
                .transferHistory(new ArrayList<>())
                .purchaseHistory(new ArrayList<>())
                .build();

        // Add purchase record (this updates quantity and totalPaid automatically)
        participant.addPurchaseRecord(
                checkoutSession.getSessionId(),
                quantity,
                totalPaid,
                null  // transactionId - will be set later from payment response
        );

        groupParticipantRepo.save(participant);

        log.info("Participant created for user: {} in group: {}",
                customer.getAccountId(), savedGroup.getGroupInstanceId());

        // 10. Check if group is already complete (user bought all seats)
        if (savedGroup.isFull()) {
            log.info("Group {} is already full, marking as COMPLETED",
                    savedGroup.getGroupInstanceId());
            savedGroup.setStatus(GroupStatus.COMPLETED);
            savedGroup.setCompletedAt(now);
            groupPurchaseInstanceRepo.save(savedGroup);

            // TODO: Trigger order creation for all participants
            log.info("Group completed - orders should be created");
        }

        // Publish event if group completed
        checkAndCompleteGroup(savedGroup);

        // 11. Return created group
        return savedGroup;
    }

    @Override
    @Transactional
    public GroupPurchaseInstanceEntity joinGroup(
            CheckoutSessionEntity checkoutSession) throws ItemNotFoundException, BadRequestException {

        log.info("User joining group: {} from checkout session: {}",
                checkoutSession.getGroupIdToBeJoined(), checkoutSession.getSessionId());

        // 1. Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        log.debug("Authenticated user: {}", authenticatedUser.getAccountId());

        // 2. Validate checkout session
        validator.validateCheckoutSessionForGroupCreation(checkoutSession, authenticatedUser);

        // 3. Extract data from checkout session
        CheckoutSessionEntity.CheckoutItem item = checkoutSession.getItems().get(0);
        UUID productId = item.getProductId();
        Integer quantity = item.getQuantity();
        BigDecimal totalPaid = checkoutSession.getPricing().getTotal();
        AccountEntity customer = checkoutSession.getCustomer();

        log.debug("Extracted data - Product: {}, Quantity: {}, Group: {}",
                productId, quantity, checkoutSession.getGroupIdToBeJoined());

        // 4. Fetch group instance
        GroupPurchaseInstanceEntity group = groupPurchaseInstanceRepo
                .findById(checkoutSession.getGroupIdToBeJoined())
                .orElseThrow(() -> new ItemNotFoundException("Group not found"));

        // 5. Validate group is joinable
        validator.validateGroupIsJoinable(group);

        // 6. CRITICAL: Validate product belongs to this group
        if (!group.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException(
                    String.format("Product mismatch. Group has product: %s, but checkout has product: %s",
                            group.getProduct().getProductId(), productId)
            );
        }

        log.debug("Product validation passed - product matches group");

        // 7. Fetch product and validate for group buying
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        validator.validateProductForGroupBuying(product);

        // 8. Validate quantity
        validator.validateQuantityForGroupBuying(quantity, product);

        // 9. Validate enough seats available
        validator.validateSeatsAvailable(group, quantity);

        // 10. Check if user already in this group
        Optional<GroupParticipantEntity> existingParticipant =
                groupParticipantRepo.findByUserAndGroupInstance(customer, group);

        LocalDateTime now = LocalDateTime.now();

        if (existingParticipant.isPresent()) {
            // User already in group - add more seats (Hybrid Approach)
            GroupParticipantEntity participant = existingParticipant.get();

            if (participant.getStatus() != ParticipantStatus.ACTIVE) {
                throw new BadRequestException(
                        String.format("Your participation status is: %s. Cannot add more seats.",
                                participant.getStatus())
                );
            }

            log.info("User {} already in group {}. Adding {} more seats.",
                    customer.getAccountId(), checkoutSession.getGroupIdToBeJoined(), quantity);

            // Add new purchase record (automatically updates quantity and totalPaid)
            participant.addPurchaseRecord(
                    checkoutSession.getSessionId(),
                    quantity,
                    totalPaid,
                    null  // transactionId
            );

            groupParticipantRepo.save(participant);

            log.info("Participant updated: user={}, oldQuantity={}, newQuantity={}",
                    customer.getAccountId(),
                    participant.getQuantity() - quantity,
                    participant.getQuantity());

        } else {
            // User not in group - create new participant
            log.info("User {} joining group {} for the first time with {} seats.",
                    customer.getAccountId(), checkoutSession.getGroupIdToBeJoined(), quantity);

            GroupParticipantEntity participant = GroupParticipantEntity.builder()
                    .groupInstance(group)
                    .user(customer)
                    .quantity(0)  // Start at 0
                    .totalPaid(BigDecimal.ZERO)  // Start at 0
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
                    null  // transactionId
            );

            groupParticipantRepo.save(participant);

            // Increment total participants count (only for new participants)
            group.setTotalParticipants(group.getTotalParticipants() + 1);

            log.info("New participant created for user: {} in group: {}",
                    customer.getAccountId(), checkoutSession.getGroupIdToBeJoined());
        }

        // 11. Update group seats
        group.setSeatsOccupied(group.getSeatsOccupied() + quantity);
        group.setUpdatedAt(now);

        log.info("Group {} seats updated: {}/{}",
                checkoutSession.getGroupIdToBeJoined(), group.getSeatsOccupied(), group.getTotalSeats());

        // 12. Check if group is now complete
        if (group.isFull()) {
            log.info("Group {} is now full, marking as COMPLETED", checkoutSession.getGroupIdToBeJoined());
            group.setStatus(GroupStatus.COMPLETED);
            group.setCompletedAt(now);

            // TODO: Trigger order creation for ALL participants
            log.info("Group completed - orders should be created for all participants");
        }

        // 13. Save and return group
        GroupPurchaseInstanceEntity savedGroup = groupPurchaseInstanceRepo.save(group);

        log.info("User {} successfully joined group {}",
                customer.getAccountId(), checkoutSession.getGroupIdToBeJoined());

        // Publish event if group completed
        checkAndCompleteGroup(savedGroup);

        return savedGroup;
    }


    @Override
    @Transactional
    public GroupParticipantEntity transferToGroup(
            UUID sourceGroupId,
            UUID targetGroupId,
            Integer quantity
    ) throws ItemNotFoundException, BadRequestException {

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

        // 16. Check if target group is now complete
        if (targetGroup.isFull()) {
            log.info("Target group {} is now full, marking as COMPLETED",
                    targetGroupId);

            targetGroup.setStatus(GroupStatus.COMPLETED);
            targetGroup.setCompletedAt(now);

            // TODO: Trigger order creation for ALL participants
            log.info("Target group completed - orders should be created");
        }

        // 17. Save everything
        groupParticipantRepo.save(sourceParticipant);
        groupParticipantRepo.save(targetParticipant);
        groupPurchaseInstanceRepo.save(sourceGroup);
        groupPurchaseInstanceRepo.save(targetGroup);


        // Publish events if groups completed
        checkAndCompleteGroup(targetGroup);

        log.info("Transfer completed successfully. User {} moved {} seats from {} to {}",
                authenticatedUser.getAccountId(), quantity,
                sourceGroup.getGroupCode(), targetGroup.getGroupCode());

        // 18. Return target participant
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
     *
     * Call this after any operation that changes seatsOccupied:
     * - createGroupInstance()
     * - joinGroup()
     * - transferToGroup()
     */
    private void checkAndCompleteGroup(GroupPurchaseInstanceEntity group) {

        // Check if group is full
        if (!group.isFull()) {
            log.debug("Group {} not full yet: {}/{}",
                    group.getGroupCode(),
                    group.getSeatsOccupied(),
                    group.getTotalSeats());
            return;
        }

        // Check if already completed (avoid duplicate events)
        if (group.getStatus() != GroupStatus.OPEN) {
            log.debug("Group {} already completed/failed, skipping",
                    group.getGroupCode());
            return;
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         GROUP COMPLETED                                ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Group ID: {}", group.getGroupInstanceId());
        log.info("Group Code: {}", group.getGroupCode());
        log.info("Product: {}", group.getProductName());
        log.info("Participants: {}", group.getTotalParticipants());
        log.info("Total Seats: {}/{}", group.getSeatsOccupied(), group.getTotalSeats());

        // Mark as completed
        LocalDateTime now = LocalDateTime.now();
        group.setStatus(GroupStatus.COMPLETED);
        group.setCompletedAt(now);
        group.setUpdatedAt(now);

        // Save immediately
        groupPurchaseInstanceRepo.save(group);

        log.info("✓ Group marked as COMPLETED");

        // Publish event for async processing
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
        }

        log.info("╚════════════════════════════════════════════════════════╝");
    }
}