package org.nextgate.nextgatebackend.group_purchase_mng.utils.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.ParticipantStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.payloads.GroupParticipantResponse;
import org.nextgate.nextgatebackend.group_purchase_mng.payloads.GroupParticipantSummaryResponse;
import org.nextgate.nextgatebackend.group_purchase_mng.payloads.GroupPurchaseResponse;
import org.nextgate.nextgatebackend.group_purchase_mng.payloads.GroupPurchaseSummaryResponse;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupParticipantRepo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupPurchaseMapper {

    private final GroupParticipantRepo groupParticipantRepo;

    // ========================================
    // GROUP PURCHASE RESPONSES
    // ========================================

    public GroupPurchaseSummaryResponse toSummaryResponse(
            GroupPurchaseInstanceEntity entity,
            AccountEntity authenticatedUser
    ) {
        if (entity == null) return null;

        // Check if user is member
        boolean isUserMember = isUserMemberOfGroup(entity, authenticatedUser);

        // Calculate progress percentage
        double progressPercentage = calculateProgressPercentage(
                entity.getSeatsOccupied(),
                entity.getTotalSeats()
        );

        // Map participant previews
        List<GroupPurchaseSummaryResponse.ParticipantPreview> participantPreviews =
                mapParticipantPreviews(entity, entity.getSeatsOccupied());

        return GroupPurchaseSummaryResponse.builder()
                .groupInstanceId(entity.getGroupInstanceId())
                .groupCode(entity.getGroupCode())
                .productName(entity.getProductName())
                .productImage(entity.getProductImage())
                .shopName(entity.getShop() != null ? entity.getShop().getShopName() : null)
                .groupPrice(entity.getGroupPrice())
                .savingsPercentage(entity.calculateSavingsPercentage())
                .currency("TZS")
                .totalSeats(entity.getTotalSeats())
                .seatsOccupied(entity.getSeatsOccupied())
                .seatsRemaining(entity.getSeatsRemaining())
                .totalParticipants(entity.getTotalParticipants())
                .progressPercentage(progressPercentage)
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .isExpired(entity.isExpired())
                .isUserMember(isUserMember)
                .participants(participantPreviews)
                .build();
    }

    public GroupPurchaseResponse toFullResponse(
            GroupPurchaseInstanceEntity entity,
            AccountEntity authenticatedUser
    ) {
        if (entity == null) return null;

        // Check if user is member and get their details
        Optional<GroupParticipantEntity> userParticipation =
                groupParticipantRepo.findByUserAndGroupInstance(authenticatedUser, entity);

        boolean isUserMember = userParticipation.isPresent() &&
                userParticipation.get().getStatus() == ParticipantStatus.ACTIVE;

        UUID myParticipantId = isUserMember ?
                userParticipation.get().getParticipantId() : null;

        Integer myQuantity = isUserMember ?
                userParticipation.get().getQuantity() : null;

        // Calculate progress percentage
        double progressPercentage = calculateProgressPercentage(
                entity.getSeatsOccupied(),
                entity.getTotalSeats()
        );

        // Map detailed participants
        List<GroupPurchaseResponse.ParticipantDetail> participantDetails =
                mapParticipantDetails(entity, authenticatedUser);

        return GroupPurchaseResponse.builder()
                .groupInstanceId(entity.getGroupInstanceId())
                .groupCode(entity.getGroupCode())
                .productId(entity.getProduct() != null ? entity.getProduct().getProductId() : null)
                .productName(entity.getProductName())
                .productImage(entity.getProductImage())
                .shopId(entity.getShop() != null ? entity.getShop().getShopId() : null)
                .shopName(entity.getShop() != null ? entity.getShop().getShopName() : null)
                .shopLogo(entity.getShop() != null ? entity.getShop().getLogoUrl() : null)
                .regularPrice(entity.getRegularPrice())
                .groupPrice(entity.getGroupPrice())
                .savingsAmount(entity.calculateSavings())
                .savingsPercentage(entity.calculateSavingsPercentage())
                .currency("TZS")
                .totalSeats(entity.getTotalSeats())
                .seatsOccupied(entity.getSeatsOccupied())
                .seatsRemaining(entity.getSeatsRemaining())
                .totalParticipants(entity.getTotalParticipants())
                .progressPercentage(progressPercentage)
                .status(entity.getStatus())
                .isExpired(entity.isExpired())
                .isFull(entity.isFull())
                .initiatorId(entity.getInitiator() != null ? entity.getInitiator().getAccountId() : null)
                .initiatorName(entity.getInitiator() != null ? entity.getInitiator().getUserName() : null)
                .durationHours(entity.getDurationHours())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .completedAt(entity.getCompletedAt())
                .maxPerCustomer(entity.getMaxPerCustomer())
                .isUserMember(isUserMember)
                .myParticipantId(myParticipantId)
                .myQuantity(myQuantity)
                .participants(participantDetails)
                .build();
    }

    public List<GroupPurchaseSummaryResponse> toSummaryResponseList(
            List<GroupPurchaseInstanceEntity> entities,
            AccountEntity authenticatedUser
    ) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(entity -> toSummaryResponse(entity, authenticatedUser))
                .collect(Collectors.toList());
    }

    // ========================================
    // PARTICIPANT RESPONSES
    // ========================================

    public GroupParticipantResponse toParticipantResponse(
            GroupParticipantEntity entity,
            boolean includeHistory
    ) {
        if (entity == null) return null;

        GroupParticipantResponse.GroupParticipantResponseBuilder builder =
                GroupParticipantResponse.builder()
                        .participantId(entity.getParticipantId())
                        .userId(entity.getUser().getAccountId())
                        .userName(entity.getUser().getUserName())
                        .userProfilePicture(getUserProfilePicture(entity.getUser()))
                        .quantity(entity.getQuantity())
                        .totalPaid(entity.getTotalPaid())
                        .status(entity.getStatus())
                        .joinedAt(entity.getJoinedAt())
                        .purchaseCount(entity.getPurchaseCount())
                        .hasTransferred(entity.hasTransferred())
                        .checkoutSessionId(entity.getCheckoutSessionId());

        // Include history only if requested (owner only)
        if (includeHistory) {
            builder.purchaseHistory(mapPurchaseHistory(entity))
                    .transferHistory(mapTransferHistory(entity));
        }

        return builder.build();
    }

    public GroupParticipantSummaryResponse toParticipantSummaryResponse(
            GroupParticipantEntity entity
    ) {
        if (entity == null) return null;

        return GroupParticipantSummaryResponse.builder()
                .participantId(entity.getParticipantId())
                .userId(entity.getUser().getAccountId())
                .userName(entity.getUser().getUserName())
                .userProfilePicture(getUserProfilePicture(entity.getUser()))
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .build();
    }

    public List<GroupParticipantResponse> toParticipantResponseList(
            List<GroupParticipantEntity> entities,
            AccountEntity authenticatedUser
    ) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(entity -> {
                    boolean isOwner = entity.getUser().getAccountId()
                            .equals(authenticatedUser.getAccountId());
                    return toParticipantResponse(entity, isOwner);
                })
                .collect(Collectors.toList());
    }

    public List<GroupParticipantSummaryResponse> toParticipantSummaryResponseList(
            List<GroupParticipantEntity> entities
    ) {
        if (entities == null) return new ArrayList<>();

        return entities.stream()
                .map(this::toParticipantSummaryResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private boolean isUserMemberOfGroup(
            GroupPurchaseInstanceEntity group,
            AccountEntity user
    ) {
        Optional<GroupParticipantEntity> participation =
                groupParticipantRepo.findByUserAndGroupInstance(user, group);

        return participation.isPresent() &&
                participation.get().getStatus() == ParticipantStatus.ACTIVE;
    }

    private double calculateProgressPercentage(int occupied, int total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(occupied)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double calculateContributionPercentage(int memberQuantity, int totalOccupied) {
        if (totalOccupied == 0) return 0.0;
        return BigDecimal.valueOf(memberQuantity)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalOccupied), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<GroupPurchaseSummaryResponse.ParticipantPreview> mapParticipantPreviews(
            GroupPurchaseInstanceEntity group,
            int totalOccupied
    ) {
        if (group.getParticipants() == null) return new ArrayList<>();

        return group.getParticipants().stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACTIVE)
                .map(p -> GroupPurchaseSummaryResponse.ParticipantPreview.builder()
                        .userId(p.getUser().getAccountId())
                        .userName(p.getUser().getUserName())
                        .userProfilePicture(getUserProfilePicture(p.getUser()))
                        .quantity(p.getQuantity())
                        .contributionPercentage(calculateContributionPercentage(
                                p.getQuantity(), totalOccupied))
                        .build())
                .collect(Collectors.toList());
    }

    private List<GroupPurchaseResponse.ParticipantDetail> mapParticipantDetails(
            GroupPurchaseInstanceEntity group,
            AccountEntity authenticatedUser
    ) {
        if (group.getParticipants() == null) return new ArrayList<>();

        int totalOccupied = group.getSeatsOccupied();

        return group.getParticipants().stream()
                .map(p -> {
                    boolean isOwner = p.getUser().getAccountId()
                            .equals(authenticatedUser.getAccountId());

                    GroupPurchaseResponse.ParticipantDetail.ParticipantDetailBuilder builder =
                            GroupPurchaseResponse.ParticipantDetail.builder()
                                    .participantId(p.getParticipantId())
                                    .userId(p.getUser().getAccountId())
                                    .userName(p.getUser().getUserName())
                                    .userProfilePicture(getUserProfilePicture(p.getUser()))
                                    .quantity(p.getQuantity())
                                    .totalPaid(p.getTotalPaid())
                                    .status(p.getStatus())
                                    .joinedAt(p.getJoinedAt())
                                    .contributionPercentage(calculateContributionPercentage(
                                            p.getQuantity(), totalOccupied))
                                    .purchaseCount(p.getPurchaseCount())
                                    .hasTransferred(p.hasTransferred());

                    // Include history only for owner
                    if (isOwner) {
                        builder.purchaseHistory(mapPurchaseHistoryForResponse(p))
                                .transferHistory(mapTransferHistoryForResponse(p));
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private List<GroupParticipantResponse.PurchaseHistoryItem> mapPurchaseHistory(
            GroupParticipantEntity participant
    ) {
        if (participant.getPurchaseHistory() == null) return new ArrayList<>();

        return participant.getPurchaseHistory().stream()
                .map(ph -> GroupParticipantResponse.PurchaseHistoryItem.builder()
                        .checkoutSessionId(ph.getCheckoutSessionId())
                        .quantity(ph.getQuantity())
                        .amountPaid(ph.getAmountPaid())
                        .purchasedAt(ph.getPurchasedAt())
                        .transactionId(ph.getTransactionId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<GroupParticipantResponse.TransferHistoryItem> mapTransferHistory(
            GroupParticipantEntity participant
    ) {
        if (participant.getTransferHistory() == null) return new ArrayList<>();

        return participant.getTransferHistory().stream()
                .map(th -> GroupParticipantResponse.TransferHistoryItem.builder()
                        .fromGroupId(th.getFromGroupId())
                        .fromGroupCode(null) // Can be fetched if needed
                        .toGroupId(th.getToGroupId())
                        .toGroupCode(null) // Can be fetched if needed
                        .transferredAt(th.getTransferredAt())
                        .reason(th.getReason())
                        .build())
                .collect(Collectors.toList());
    }

    private List<GroupPurchaseResponse.PurchaseHistoryItem> mapPurchaseHistoryForResponse(
            GroupParticipantEntity participant
    ) {
        if (participant.getPurchaseHistory() == null) return new ArrayList<>();

        return participant.getPurchaseHistory().stream()
                .map(ph -> GroupPurchaseResponse.PurchaseHistoryItem.builder()
                        .checkoutSessionId(ph.getCheckoutSessionId())
                        .quantity(ph.getQuantity())
                        .amountPaid(ph.getAmountPaid())
                        .purchasedAt(ph.getPurchasedAt())
                        .transactionId(ph.getTransactionId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<GroupPurchaseResponse.TransferHistoryItem> mapTransferHistoryForResponse(
            GroupParticipantEntity participant
    ) {
        if (participant.getTransferHistory() == null) return new ArrayList<>();

        return participant.getTransferHistory().stream()
                .map(th -> GroupPurchaseResponse.TransferHistoryItem.builder()
                        .fromGroupId(th.getFromGroupId())
                        .fromGroupCode(null) // Can be fetched if needed
                        .toGroupId(th.getToGroupId())
                        .toGroupCode(null) // Can be fetched if needed
                        .transferredAt(th.getTransferredAt())
                        .reason(th.getReason())
                        .build())
                .collect(Collectors.toList());
    }

    private String getUserProfilePicture(AccountEntity user) {
        if (user.getProfilePictureUrls() != null && !user.getProfilePictureUrls().isEmpty()) {
            return user.getProfilePictureUrls().get(0);
        }
        return null;
    }
}