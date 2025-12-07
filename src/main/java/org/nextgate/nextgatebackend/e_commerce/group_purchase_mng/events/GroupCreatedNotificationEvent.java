package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Notification events for group purchases.
 * These are published WITHIN transactions and processed AFTER commit.
 * This decouples notifications from business logic - notification failures won't rollback transactions.
 */

// ========================================
// GROUP CREATED NOTIFICATION EVENT
// ========================================
@Getter
public class GroupCreatedNotificationEvent extends ApplicationEvent {

    private final UUID groupInstanceId;
    private final String groupCode;
    private final String productName;
    private final String shopName;
    private final UUID shopOwnerId;
    private final String shopOwnerEmail;
    private final String shopOwnerPhone;
    private final String shopOwnerName;
    private final UUID creatorId;
    private final String creatorName;
    private final Integer initialSeats;
    private final Integer totalSeats;

    public GroupCreatedNotificationEvent(
            Object source,
            GroupPurchaseInstanceEntity group,
            AccountEntity creator,
            Integer initialSeats
    ) {
        super(source);
        this.groupInstanceId = group.getGroupInstanceId();
        this.groupCode = group.getGroupCode();
        this.productName = group.getProductName();
        this.shopName = group.getShop().getShopName();

        AccountEntity shopOwner = group.getShop().getOwner();
        this.shopOwnerId = shopOwner != null ? shopOwner.getId() : null;
        this.shopOwnerEmail = shopOwner != null ? shopOwner.getEmail() : null;
        this.shopOwnerPhone = shopOwner != null ? shopOwner.getPhoneNumber() : null;
        this.shopOwnerName = shopOwner != null ? shopOwner.getFirstName() : null;

        this.creatorId = creator.getId();
        this.creatorName = creator.getUserName();
        this.initialSeats = initialSeats;
        this.totalSeats = group.getTotalSeats();
    }
}

// ========================================
// MEMBER JOINED NOTIFICATION EVENT
// ========================================
@Getter
class MemberJoinedNotificationEvent extends ApplicationEvent {

    private final UUID groupInstanceId;
    private final String groupCode;
    private final String productName;
    private final UUID newMemberId;
    private final String newMemberName;
    private final Integer quantity;
    private final Integer seatsOccupied;
    private final Integer totalSeats;
    private final UUID shopOwnerId;
    private final String shopOwnerEmail;
    private final String shopOwnerName;

    public MemberJoinedNotificationEvent(
            Object source,
            GroupPurchaseInstanceEntity group,
            AccountEntity newMember,
            Integer quantity
    ) {
        super(source);
        this.groupInstanceId = group.getGroupInstanceId();
        this.groupCode = group.getGroupCode();
        this.productName = group.getProductName();
        this.newMemberId = newMember.getId();
        this.newMemberName = newMember.getUserName();
        this.quantity = quantity;
        this.seatsOccupied = group.getSeatsOccupied();
        this.totalSeats = group.getTotalSeats();

        AccountEntity shopOwner = group.getShop().getOwner();
        this.shopOwnerId = shopOwner != null ? shopOwner.getId() : null;
        this.shopOwnerEmail = shopOwner != null ? shopOwner.getEmail() : null;
        this.shopOwnerName = shopOwner != null ? shopOwner.getFirstName() : null;
    }
}

// ========================================
// SEATS TRANSFERRED NOTIFICATION EVENT
// ========================================
@Getter
class SeatsTransferredNotificationEvent extends ApplicationEvent {

    private final UUID userId;
    private final String userEmail;
    private final String userName;
    private final UUID sourceGroupId;
    private final String sourceGroupCode;
    private final UUID targetGroupId;
    private final String targetGroupCode;
    private final String productName;
    private final Integer quantity;

    public SeatsTransferredNotificationEvent(
            Object source,
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup,
            AccountEntity user,
            Integer quantity
    ) {
        super(source);
        this.userId = user.getId();
        this.userEmail = user.getEmail();
        this.userName = user.getUserName();
        this.sourceGroupId = sourceGroup.getGroupInstanceId();
        this.sourceGroupCode = sourceGroup.getGroupCode();
        this.targetGroupId = targetGroup.getGroupInstanceId();
        this.targetGroupCode = targetGroup.getGroupCode();
        this.productName = targetGroup.getProductName();
        this.quantity = quantity;
    }
}