package org.nextgate.nextgatebackend.group_purchase_mng.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;

import java.util.List;
import java.util.UUID;

public interface GroupPurchaseService {

    // Core Operations
    void createGroupInstance(CheckoutSessionEntity checkoutSession) throws ItemNotFoundException, BadRequestException;

    void joinGroup(CheckoutSessionEntity checkoutSession) throws ItemNotFoundException, BadRequestException;

     GroupParticipantEntity transferToGroup(UUID sourceGroupId, UUID targetGroupId, Integer quantity) throws ItemNotFoundException, BadRequestException;


   // Query Operations - Groups
    GroupPurchaseInstanceEntity getGroupById(UUID groupInstanceId)
            throws ItemNotFoundException;


    GroupPurchaseInstanceEntity getGroupByCode(String groupCode)
            throws ItemNotFoundException;


    List<GroupPurchaseInstanceEntity> getAvailableGroupsForProduct(
            ProductEntity product
    );

    List<GroupPurchaseInstanceEntity> getGroupsUserBelongsTo(
            AccountEntity user,
            GroupStatus status
    );

    // Query Operations - Participants
    List<GroupParticipantEntity> getMyActiveParticipations(AccountEntity user);

    List<GroupParticipantEntity> getGroupParticipants(
            GroupPurchaseInstanceEntity group
    );

    // Validation Operations
    boolean canTransferToGroup(
            AccountEntity user,
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup
    );

    boolean canJoinGroup(
            GroupPurchaseInstanceEntity group,
            AccountEntity user);

    void checkAndPublishGroupCompletion(UUID groupInstanceId);

}