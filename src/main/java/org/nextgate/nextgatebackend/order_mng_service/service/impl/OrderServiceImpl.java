package org.nextgate.nextgatebackend.order_mng_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderItemEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.DeliveryStatus;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderSource;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.order_mng_service.repo.OrderRepository;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final CheckoutSessionRepo checkoutSessionRepo;
    private final GroupPurchaseInstanceRepo groupRepo;
    private final InstallmentAgreementRepo agreementRepo;
    private final ProductRepo productRepo;
    private final ShopRepo shopRepo;

    // ========================================
    // UNIVERSAL ORDER CREATION
    // ========================================

    @Override
    @Transactional
    public List<UUID> createOrdersFromCheckoutSession(UUID checkoutSessionId)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         CREATING ORDER FROM CHECKOUT SESSION          ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Checkout Session ID: {}", checkoutSessionId);

        // ========================================
        // 1. FETCH CHECKOUT SESSION
        // ========================================
        CheckoutSessionEntity session = checkoutSessionRepo.findById(checkoutSessionId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found: " + checkoutSessionId));

        log.info("Session Details:");
        log.info("  Type: {}", session.getSessionType());
        log.info("  Status: {}", session.getStatus());
        log.info("  Customer: {}", session.getCustomer().getUserName());
        log.info("  Total: {} {}", session.getPricing().getTotal(),
                session.getPricing().getCurrency());

        // ========================================
        // 2. VALIDATE SESSION
        // ========================================
        validateSessionCanCreateOrder(session);

        // ========================================
        // 3. CHECK IF ORDER ALREADY EXISTS
        // ========================================
        if (session.getCreatedOrderId() != null) {
            log.warn("Order already exists: {}", session.getCreatedOrderId());
            return List.of(session.getCreatedOrderId());
        }

        // ========================================
        // 4. DELEGATE BY SESSION TYPE
        // ========================================
        log.info("Delegating to {} order creation handler", session.getSessionType());

        return switch (session.getSessionType()) {
            case REGULAR_DIRECTLY, REGULAR_CART -> createDirectOrder(session);
            case INSTALLMENT -> createInstallmentOrder(session);
            case GROUP_PURCHASE -> createGroupOrder(session);
        };
    }

    // ========================================
    // DIRECT ORDER CREATION
    // ========================================

    private List<UUID> createDirectOrder(CheckoutSessionEntity session)
            throws ItemNotFoundException {

        log.info("Creating DIRECT order");

        // Build order
        OrderEntity order = buildOrderFromCheckoutSession(
                session,
                OrderSource.DIRECT_PURCHASE
        );

        // Save order
        OrderEntity savedOrder = orderRepo.save(order);

        // Update session
        session.setCreatedOrderId(savedOrder.getOrderId());
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("✓ Direct order created: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());

        return List.of(savedOrder.getOrderId());
    }

    // ========================================
    // INSTALLMENT ORDER CREATION
    // ========================================

    private List<UUID> createInstallmentOrder(CheckoutSessionEntity session)
            throws ItemNotFoundException, BadRequestException {

        log.info("Creating INSTALLMENT order");

        // ========================================
        // 1. GET AGREEMENT
        // ========================================
        InstallmentAgreementEntity agreement = agreementRepo
                .findByCheckoutSessionId(session.getSessionId())
                .orElseThrow(() -> new ItemNotFoundException(
                        "Installment agreement not found for session"));

        log.info("Found agreement: {}", agreement.getAgreementNumber());
        log.info("  Fulfillment: {}", agreement.getFulfillmentTiming());
        log.info("  Amount Paid: {} TZS", agreement.getAmountPaid());
        log.info("  Amount Remaining: {} TZS", agreement.getAmountRemaining());

        // ========================================
        // 2. VALIDATE FULFILLMENT TIMING
        // ========================================
        if (agreement.getFulfillmentTiming() == FulfillmentTiming.AFTER_PAYMENT &&
                !agreement.isCompleted()) {
            throw new BadRequestException(
                    "Cannot create order - agreement not fully paid. " +
                            "Fulfillment timing: AFTER_PAYMENT requires completion.");
        }

        log.info("✓ Fulfillment validation passed");

        // ========================================
        // 3. BUILD ORDER
        // ========================================
        OrderEntity order = buildOrderFromCheckoutSession(
                session,
                OrderSource.INSTALLMENT
        );

        // ========================================
        // 4. LINK TO AGREEMENT
        // ========================================
        OrderItemEntity firstItem = order.getItems().get(0);
        firstItem.setInstallmentAgreementId(agreement.getAgreementId());
        firstItem.setFulfillmentTiming(agreement.getFulfillmentTiming());

        log.info("✓ Order item linked to agreement");

        // ========================================
        // 5. SET PAYMENT TRACKING
        // ========================================
        order.setAmountPaid(agreement.getAmountPaid());
        order.setAmountRemaining(agreement.getAmountRemaining());

        log.info("✓ Payment tracking configured:");
        log.info("  Paid: {} TZS", order.getAmountPaid());
        log.info("  Remaining: {} TZS", order.getAmountRemaining());

        // ========================================
        // 6. SAVE ORDER
        // ========================================
        OrderEntity savedOrder = orderRepo.save(order);

        // ========================================
        // 7. UPDATE AGREEMENT
        // ========================================
        agreement.setOrderId(savedOrder.getOrderId());
        agreementRepo.save(agreement);

        log.info("✓ Agreement updated with order ID");

        // ========================================
        // 8. UPDATE SESSION
        // ========================================
        session.setCreatedOrderId(savedOrder.getOrderId());
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("✓ Installment order created: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());

        return List.of(savedOrder.getOrderId());
    }

    // ========================================
    // GROUP ORDER CREATION
    // ========================================

    private List<UUID> createGroupOrder(CheckoutSessionEntity session)
            throws ItemNotFoundException, BadRequestException {

        log.info("Creating GROUP order");

        // ========================================
        // 1. GET GROUP ID FROM SESSION
        // ========================================
        UUID groupInstanceId = session.getGroupIdToBeJoined();

        if (groupInstanceId == null) {
            throw new BadRequestException(
                    "No group instance linked to this checkout session");
        }

        log.info("Group Instance ID: {}", groupInstanceId);

        // ========================================
        // 2. FETCH AND VALIDATE GROUP
        // ========================================
        GroupPurchaseInstanceEntity group = groupRepo.findById(groupInstanceId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Group not found: " + groupInstanceId));

        log.info("Group Details:");
        log.info("  Product: {}", group.getProduct().getProductName());
        log.info("  Status: {}", group.getStatus());
        log.info("  Participants: {}/{}",
                group.getSeatsOccupied(), group.getTotalSeats());

        // ========================================
        // 3. VALIDATE GROUP IS COMPLETED
        // ========================================
        if (group.getStatus() != GroupStatus.COMPLETED) {
            throw new BadRequestException(
                    String.format("Group not completed yet. Status: %s. " +
                                    "Orders can only be created after group completion.",
                            group.getStatus()));
        }

        log.info("✓ Group completion validated");

        // ========================================
        // 4. BUILD ORDER
        // ========================================
        OrderEntity order = buildOrderFromCheckoutSession(
                session,
                OrderSource.GROUP_PURCHASE
        );

        // ========================================
        // 5. STORE GROUP METADATA
        // ========================================
        order.getMetadata().put("groupInstanceId", groupInstanceId.toString());
        order.getMetadata().put("groupPrice",
                session.getItems().get(0).getUnitPrice().toString());
        order.getMetadata().put("regularPrice",
                group.getProduct().getPrice().toString());

        BigDecimal savings = group.getProduct().getPrice()
                .subtract(group.getProduct().getGroupPrice())
                .multiply(BigDecimal.valueOf(session.getItems().get(0).getQuantity()));
        order.getMetadata().put("savings", savings.toString());

        log.info("✓ Group metadata added (savings: {} TZS)", savings);

        // ========================================
        // 6. SAVE ORDER
        // ========================================
        OrderEntity savedOrder = orderRepo.save(order);

        // ========================================
        // 7. UPDATE SESSION
        // ========================================
        session.setCreatedOrderId(savedOrder.getOrderId());
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("✓ Group order created: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());
        log.info("  Customer: {}", session.getCustomer().getUserName());

        return List.of(savedOrder.getOrderId());
    }

    // ========================================
    // ORDER BUILDER
    // ========================================

    private OrderEntity buildOrderFromCheckoutSession(
            CheckoutSessionEntity session,
            OrderSource orderSource) throws ItemNotFoundException {

        log.debug("Building order from session");

        // ========================================
        // 1. GET SHOP FROM FIRST ITEM
        // ========================================
        CheckoutSessionEntity.CheckoutItem firstItem = session.getItems().get(0);
        ShopEntity shop = shopRepo.findById(firstItem.getShopId())
                .orElseThrow(() -> new ItemNotFoundException(
                        "Shop not found: " + firstItem.getShopId()));

        log.debug("Shop: {}", shop.getShopName());

        // ========================================
        // 2. BUILD ORDER ENTITY
        // ========================================
        OrderEntity order = OrderEntity.builder()
                // Parties
                .buyer(session.getCustomer())
                .seller(shop)

                // Source & Reference
                .orderSource(orderSource)
                .checkoutSessionId(session.getSessionId())

                // Financial
                .subtotal(session.getPricing().getSubtotal())
                .shippingFee(session.getPricing().getShippingCost())
                .tax(session.getPricing().getTax())
                .discount(session.getPricing().getDiscount())
                .totalAmount(session.getPricing().getTotal())

                // Payment
                .paymentMethod(session.getPaymentIntent().getProvider())
                .amountPaid(session.getPricing().getTotal())  // Full for direct/group
                .amountRemaining(BigDecimal.ZERO)            // Zero for direct/group

                // Status
                .orderStatus(OrderStatus.PENDING_SHIPMENT)
                .deliveryStatus(DeliveryStatus.PENDING)

                // Delivery
                .deliveryAddress(serializeAddress(session.getShippingAddress()))
                .deliveryInstructions(null)

                // Flags
                .isDeliveryConfirmed(false)
                .isEscrowReleased(false)

                // Timestamps
                .orderedAt(LocalDateTime.now())

                // Metadata
                .metadata(new HashMap<>())

                // Currency
                .currency(session.getPricing().getCurrency())

                // Soft delete
                .isDeleted(false)

                .build();

        log.debug("✓ Order entity built");

        // ========================================
        // 3. BUILD ORDER ITEMS
        // ========================================
        for (CheckoutSessionEntity.CheckoutItem sessionItem : session.getItems()) {

            ProductEntity product = productRepo.findById(sessionItem.getProductId())
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Product not found: " + sessionItem.getProductId()));

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .productName(sessionItem.getProductName())
                    .productImage(sessionItem.getProductImage())
                    .priceAtPurchase(sessionItem.getUnitPrice())
                    .quantity(sessionItem.getQuantity())
                    .discount(sessionItem.getDiscountAmount())
                    .build();

            order.addItem(orderItem);

            log.debug("✓ Order item added: {} x{}",
                    sessionItem.getProductName(),
                    sessionItem.getQuantity());
        }

        log.debug("✓ All order items added ({})", session.getItems().size());

        return order;
    }

    // ========================================
    // VALIDATION
    // ========================================

    private void validateSessionCanCreateOrder(CheckoutSessionEntity session)
            throws BadRequestException {

        log.debug("Validating session can create order");

        // Check status
        if (session.getStatus() != CheckoutSessionStatus.PAYMENT_COMPLETED &&
                session.getStatus() != CheckoutSessionStatus.COMPLETED) {
            throw new BadRequestException(
                    "Session must have completed payment. Current status: " +
                            session.getStatus());
        }

        // Check items exist
        if (session.getItems() == null || session.getItems().isEmpty()) {
            throw new BadRequestException("Session has no items");
        }

        // Check pricing exists
        if (session.getPricing() == null) {
            throw new BadRequestException("Session has no pricing information");
        }

        // Check customer exists
        if (session.getCustomer() == null) {
            throw new BadRequestException("Session has no customer");
        }

        log.debug("✓ Session validation passed");
    }

    // ========================================
    // HELPER
    // ========================================

    private String serializeAddress(CheckoutSessionEntity.ShippingAddress address) {
        if (address == null) return null;

        return String.format("%s, %s, %s, %s %s, %s",
                address.getAddressLine1(),
                address.getAddressLine2() != null ? address.getAddressLine2() : "",
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()).replaceAll(", ,", ",");
    }
}