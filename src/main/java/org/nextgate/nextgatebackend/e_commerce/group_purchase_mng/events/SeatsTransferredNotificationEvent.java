package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

// ========================================
// SEATS TRANSFERRED NOTIFICATION EVENT
// ========================================
@Getter
public class SeatsTransferredNotificationEvent extends ApplicationEvent {

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
