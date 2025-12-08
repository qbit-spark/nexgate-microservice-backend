package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

// ========================================
// MEMBER JOINED NOTIFICATION EVENT
// ========================================
@Getter
public class MemberJoinedNotificationEvent extends ApplicationEvent {

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
