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

