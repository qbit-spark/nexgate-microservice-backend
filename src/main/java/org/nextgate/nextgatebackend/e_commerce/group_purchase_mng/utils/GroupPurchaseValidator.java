package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GroupPurchaseValidator {

    // ========================================
    // VALIDATIONS FOR createGroupInstance()
    // ========================================

    public void validateCheckoutSessionForGroupCreation(
            ProductCheckoutSessionEntity session,
            AccountEntity authenticatedUser
    ) throws BadRequestException {

        // 1. Validate session exists
        if (session == null) {
            throw new BadRequestException("Checkout session is null");
        }

        // 2. Validate session type
        if (session.getSessionType() != CheckoutSessionType.GROUP_PURCHASE) {
            throw new BadRequestException(
                    String.format("Invalid session type: %s. Expected: GROUP_PURCHASE",
                            session.getSessionType())
            );
        }

        // 3. Validate session status
        if (session.getStatus() != CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException(
                    String.format("Payment not completed. Session status: %s",
                            session.getStatus())
            );
        }

        // 4. Validate payment intent status
        if (session.getPaymentIntent() == null) {
            throw new BadRequestException("Payment intent is missing");
        }

        if (!"READY".equals(session.getPaymentIntent().getStatus()) &&
                !"COMPLETED".equals(session.getPaymentIntent().getStatus())) {
            throw new BadRequestException(
                    String.format("Invalid payment intent status: %s",
                            session.getPaymentIntent().getStatus())
            );
        }

        // 5. Validate session has customer
        if (session.getCustomer() == null) {
            throw new BadRequestException("Checkout session has no customer");
        }

        // 6. Validate authenticated user matches session customer
        if (!authenticatedUser.getAccountId().equals(session.getCustomer().getAccountId())) {
            throw new BadRequestException(
                    "Authenticated user does not match checkout session customer"
            );
        }

        // 7. Validate session has exactly 1 item
        if (session.getItems() == null || session.getItems().isEmpty()) {
            throw new BadRequestException("Checkout session has no items");
        }

        if (session.getItems().size() != 1) {
            throw new BadRequestException(
                    "GROUP_PURCHASE must have exactly 1 item"
            );
        }

        // 8. Validate session has pricing
        if (session.getPricing() == null || session.getPricing().getTotal() == null) {
            throw new BadRequestException("Checkout session has no pricing information");
        }

        // 9. Validate metadata does NOT contain groupInstanceId
        if (session.getMetadata() != null &&
                session.getMetadata().containsKey("groupInstanceId")) {
            throw new BadRequestException(
                    "Checkout session already has groupInstanceId. Use joinGroup() instead."
            );
        }

        log.debug("Checkout session validation passed for group creation: {}",
                session.getSessionId());
    }

    public void validateProductForGroupBuying(ProductEntity product) throws BadRequestException {

        // 1. Validate product is not deleted
        if (product.getIsDeleted() != null && product.getIsDeleted()) {
            throw new BadRequestException("Product has been deleted");
        }

        // 2. Validate product status is ACTIVE
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException(
                    String.format("Product is not active. Status: %s", product.getStatus())
            );
        }

        // 3. Validate group buying is enabled
        if (product.getGroupBuyingEnabled() == null || !product.getGroupBuyingEnabled()) {
            throw new BadRequestException(
                    "Group buying is not enabled for this product"
            );
        }

        // 4. Validate groupPrice is set
        if (product.getGroupPrice() == null) {
            throw new BadRequestException("Product group price is not set");
        }

        // 5. Validate groupMaxSize is set
        if (product.getGroupMaxSize() == null || product.getGroupMaxSize() <= 0) {
            throw new BadRequestException(
                    "Product group max size is not set or invalid"
            );
        }

        // 6. Validate groupTimeLimitHours is set
        if (product.getGroupTimeLimitHours() == null || product.getGroupTimeLimitHours() <= 0) {
            throw new BadRequestException(
                    "Product group time limit is not set or invalid"
            );
        }

        log.debug("Product validation passed for group buying: {}", product.getProductId());
    }

    public void validateQuantityForGroupBuying(
            Integer quantity,
            ProductEntity product
    ) throws BadRequestException {

        // 1. Validate quantity is positive
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        // 2. Validate quantity does not exceed group max size
        if (quantity > product.getGroupMaxSize()) {
            throw new BadRequestException(
                    String.format("Quantity (%d) exceeds group max size (%d)",
                            quantity, product.getGroupMaxSize())
            );
        }

        // 3. Validate quantity respects maxPerCustomer limit
        if (product.getMaxPerCustomer() != null && product.getMaxPerCustomer() > 0) {
            if (quantity > product.getMaxPerCustomer()) {
                throw new BadRequestException(
                        String.format("Quantity (%d) exceeds max per customer (%d)",
                                quantity, product.getMaxPerCustomer())
                );
            }
        }

        log.debug("Quantity validation passed: {} for product: {}",
                quantity, product.getProductId());
    }

    // ========================================
    // VALIDATIONS FOR joinGroup()
    // ========================================

    public void validateCheckoutSessionForGroupJoin(
            ProductCheckoutSessionEntity session,
            AccountEntity authenticatedUser
    ) throws BadRequestException {

        // Reuse most validations from create
        validateCheckoutSessionForGroupCreation(session, authenticatedUser);

        // Additional validation: metadata MUST contain groupInstanceId
        if (session.getMetadata() == null ||
                !session.getMetadata().containsKey("groupInstanceId")) {
            throw new BadRequestException(
                    "Checkout session missing groupInstanceId. Use createGroupInstance() instead."
            );
        }

        // Validate groupInstanceId is valid UUID
        Object groupIdObj = session.getMetadata().get("groupInstanceId");
        if (groupIdObj == null) {
            throw new BadRequestException("groupInstanceId is null");
        }

        try {
            UUID.fromString(groupIdObj.toString());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid groupInstanceId format");
        }

        log.debug("Checkout session validation passed for group join: {}",
                session.getSessionId());
    }

    public void validateGroupIsJoinable(GroupPurchaseInstanceEntity group)
            throws BadRequestException {

        // 1. Validate group status is OPEN
        if (group.getStatus() != GroupStatus.OPEN) {
            throw new BadRequestException(
                    String.format("Group is not open for joining. Status: %s",
                            group.getStatus())
            );
        }

        // 2. Validate group is not expired
        if (group.isExpired()) {
            throw new BadRequestException(
                    String.format("Group has expired at: %s", group.getExpiresAt())
            );
        }

        // 3. Validate group is not deleted
        if (group.getIsDeleted() != null && group.getIsDeleted()) {
            throw new BadRequestException("Group has been deleted");
        }

        // 4. Validate group is not full
        if (group.isFull()) {
            throw new BadRequestException(
                    String.format("Group is full. Seats occupied: %d/%d",
                            group.getSeatsOccupied(), group.getTotalSeats())
            );
        }

        log.debug("Group validation passed for joining: {}", group.getGroupInstanceId());
    }

    public void validateSeatsAvailable(
            GroupPurchaseInstanceEntity group,
            Integer quantity
    ) throws BadRequestException {

        Integer seatsRemaining = group.getSeatsRemaining();

        if (seatsRemaining < quantity) {
            throw new BadRequestException(
                    String.format("Not enough seats available. Requested: %d, Available: %d",
                            quantity, seatsRemaining)
            );
        }

        log.debug("Seats availability validated: {} seats available for {} requested",
                seatsRemaining, quantity);
    }

    // ========================================
    // VALIDATIONS FOR transferToGroup()
    // ========================================

    public void validateGroupTransfer(
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup,
            Integer quantity
    ) throws BadRequestException {

        // 1. Validate target group is joinable
        validateGroupIsJoinable(targetGroup);

        // 2. Validate same product
        if (!sourceGroup.getProduct().getProductId()
                .equals(targetGroup.getProduct().getProductId())) {
            throw new BadRequestException(
                    "Cannot transfer between groups with different products"
            );
        }

        // 3. Validate same shop
        if (!sourceGroup.getShop().getShopId()
                .equals(targetGroup.getShop().getShopId())) {
            throw new BadRequestException(
                    "Cannot transfer between groups from different shops"
            );
        }

        // 4. Validate same price (snapshot comparison)
        if (sourceGroup.getGroupPrice().compareTo(targetGroup.getGroupPrice()) != 0) {
            throw new BadRequestException(
                    String.format("Cannot transfer. Price mismatch: %s vs %s",
                            sourceGroup.getGroupPrice(), targetGroup.getGroupPrice())
            );
        }

        // 5. Validate target group has enough seats
        validateSeatsAvailable(targetGroup, quantity);

        log.debug("Group transfer validation passed from {} to {}",
                sourceGroup.getGroupInstanceId(), targetGroup.getGroupInstanceId());
    }
}