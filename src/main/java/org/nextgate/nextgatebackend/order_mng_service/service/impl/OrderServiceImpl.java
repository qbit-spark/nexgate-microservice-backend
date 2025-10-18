package org.nextgate.nextgatebackend.order_mng_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.financial_system.escrow.service.EscrowService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentAgreementEntity;
import org.nextgate.nextgatebackend.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentAgreementRepo;
import org.nextgate.nextgatebackend.notification_system.publisher.NotificationPublisher;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.NotificationEvent;
import org.nextgate.nextgatebackend.notification_system.publisher.dto.Recipient;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationChannel;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationPriority;
import org.nextgate.nextgatebackend.notification_system.publisher.enums.NotificationType;
import org.nextgate.nextgatebackend.notification_system.publisher.mapper.OrderNotificationMapper;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderItemEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.DeliveryStatus;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderSource;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.order_mng_service.repo.OrderRepository;
import org.nextgate.nextgatebackend.order_mng_service.service.DeliveryConfirmationService;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    private final DeliveryConfirmationService deliveryConfirmationService;
    private final EscrowService escrowService;
    private final NotificationPublisher notificationPublisher;

    // ========================================
    // UNIVERSAL ORDER CREATION
    // ========================================

    @Override
    @Transactional
    public List<UUID> createOrdersFromCheckoutSession(UUID checkoutSessionId)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════╗");
        log.info("║  CREATING ORDER FROM CHECKOUT SESSION  ║");
        log.info("╚════════════════════════════════════════╝");
        log.info("Session ID: {}", checkoutSessionId);

        // ========================================
        // 1. FETCH SESSION
        // ========================================
        CheckoutSessionEntity session = checkoutSessionRepo
                .findById(checkoutSessionId)
                .orElseThrow(() -> new ItemNotFoundException("Session not found"));

        log.info("Session Type: {}", session.getSessionType());
        log.info("Customer: {}", session.getCustomer().getUserName());

        // ========================================
        // 2. VALIDATE SESSION
        // ========================================
        validateSessionCanCreateOrder(session);

        // ========================================
        // 3. CHECK IF ORDERS ALREADY EXIST
        // ========================================
        if (session.getCreatedOrderIds() != null && !session.getCreatedOrderIds().isEmpty()) {
            log.warn("⚠ Orders already exist for session: {}", checkoutSessionId);
            log.warn("  Order IDs: {}", session.getCreatedOrderIds());
            log.warn("  Skipping order creation (idempotency check)");
            return session.getCreatedOrderIds(); // ✅ Return existing orders
        }

        log.info("No existing orders found - proceeding with creation");

        // ========================================
        // 4. DELEGATE TO SPECIFIC HANDLER
        // ========================================
        List<UUID> createdOrderIds = switch (session.getSessionType()) {

            case REGULAR_DIRECTLY -> {
                log.info("→ Handling DIRECT PURCHASE (single item, single shop)");
                yield createDirectPurchaseOrder(session);
            }

            case REGULAR_CART -> {
                log.info("→ Handling CART PURCHASE (multiple items, possibly multiple shops)");
                yield createCartPurchaseOrders(session);
            }

            case INSTALLMENT -> {
                log.info("→ Handling INSTALLMENT PURCHASE");
                yield createInstallmentOrder(session);
            }

            case GROUP_PURCHASE -> {
                log.info("→ Handling GROUP PURCHASE");
                yield createGroupOrder(session);
            }
        };

        log.info("✓ Order creation complete");
        log.info("  Created {} order(s)", createdOrderIds.size());
        log.info("  Order IDs: {}", createdOrderIds);

        return createdOrderIds;
    }


    @Override
    @Transactional(readOnly = true)
    public OrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException {


        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Order not found: " + orderId));

        // Validate access
        validateOrderAccess(order, requester);

        return order;
    }


    @Override
    @Transactional(readOnly = true)
    public OrderEntity getOrderByNumber(String orderNumber, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException {


        OrderEntity order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Order not found: " + orderNumber));

        // Validate access
        validateOrderAccess(order, requester);

        return order;
    }


    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getMyOrders(AccountEntity customer) {

        return orderRepo.findByBuyerOrderByOrderedAtDesc(customer);
    }


    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getMyOrdersByStatus(AccountEntity customer, OrderStatus status) {


        return orderRepo.findByBuyerAndOrderStatusOrderByOrderedAtDesc(
                customer, status);
    }


    // ========================================
    // QUERY METHODS - SHOP ORDERS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getShopOrders(ShopEntity shop) {

        return orderRepo.findBySellerOrderByOrderedAtDesc(shop);
    }


    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getShopOrdersByStatus(ShopEntity shop, OrderStatus status) {

        return orderRepo.findBySellerAndOrderStatusOrderByOrderedAtDesc(
                shop, status);
    }


    // ========================================
    // ORDER STATUS UPDATES - SELLER ACTIONS
    // ========================================

    @Override
    @Transactional
    public void markOrderAsShipped(UUID orderId, AccountEntity seller) throws ItemNotFoundException, BadRequestException {

        log.info("Marking order as shipped - Order ID: {}, Seller: {}",
                orderId, seller.getUserName());

        // Step 1: Validate order and seller
        OrderEntity order = validateOrderForShipping(orderId, seller);

        // Step 2: Generate confirmation code FIRST
        String confirmationCode;
        try {
            confirmationCode = deliveryConfirmationService.generateConfirmationCode(order);
            log.info("Confirmation code generated: {}", confirmationCode);
        } catch (Exception e) {
            log.error("Failed to generate confirmation code", e);
            throw new BadRequestException("Failed to generate confirmation code: " + e.getMessage());
        }

        // Step 3: Update order to SHIP
        LocalDateTime now = LocalDateTime.now();
        String trackingNumber = "TRACK-" + orderId.toString().substring(0, 8).toUpperCase();
        String carrier = "NextGate Shipping";

        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        order.setShippedAt(now);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("Order {} marked as SHIPPED", order.getOrderNumber());

        //Todo: Step 4: Placeholder for notification
        sendOrderShippedNotification(order, confirmationCode);

        log.info("Order shipped successfully - Order: {}", order.getOrderNumber());
    }


    @Override
    @Transactional
    public void confirmDelivery(UUID orderId, String confirmationCode, AccountEntity customer, String ipAddress, String deviceInfo) throws ItemNotFoundException, BadRequestException, RandomExceptions {

        // Step 1: Validate order and customer
        OrderEntity order = validateOrderForConfirmation(orderId, customer);

        // Step 2: Verify confirmation code
        verifyConfirmationCode(orderId, confirmationCode, customer, ipAddress, deviceInfo);

        // Step 3: Update order to DELIVERED and COMPLETED
        LocalDateTime now = LocalDateTime.now();


        // Step 4: Release escrow to seller
        releaseEscrow(order);


        order.setIsDeliveryConfirmed(true);
        order.setDeliveryConfirmedAt(now);
        order.setDeliveredAt(now);
        order.setDeliveryStatus(DeliveryStatus.CONFIRMED);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(now);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        //Todo: Step 5: Placeholder for notifications
        // 1. Buyer: Order delivered
        sendOrderDeliveredNotificationToBuyer(order);

        // 2. Seller: Order delivered successfully
        sendOrderDeliveredNotificationToSeller(order);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> getMyOrdersPaged(AccountEntity customer, int page, int size) {

        // Default page to 1 if less than 1
        if (page < 1) page = 1;
        // Default size to 10 if invalid
        if (size <= 0) size = 10;

        log.info("Fetching orders for customer: {} (page {}, size {})",
                customer.getUserName(), page, size);

        // Spring Data uses 0-based index, so subtract 1
        Pageable pageable = PageRequest.of(page - 1, size);

        return orderRepo.findByBuyerOrderByOrderedAtDesc(customer, pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> getMyOrdersByStatusPaged(AccountEntity customer, OrderStatus status, int page, int size) {

        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        log.info("Fetching orders for customer: {} with status: {} (page {}, size {})",
                customer.getUserName(), status, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);

        return orderRepo.findByBuyerAndOrderStatusOrderByOrderedAtDesc(
                customer, status, pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> getShopOrdersPaged(ShopEntity shop, int page, int size) {

        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        log.info("Fetching orders for shop: {} (page {}, size {})",
                shop.getShopName(), page, size);

        Pageable pageable = PageRequest.of(page - 1, size);

        return orderRepo.findBySellerOrderByOrderedAtDesc(shop, pageable);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> getShopOrdersByStatusPaged(ShopEntity shop, OrderStatus status, int page, int size) {

        if (page < 1) page = 1;
        if (size <= 0) size = 10;

        log.info("Fetching orders for shop: {} with status: {} (page {}, size {})",
                shop.getShopName(), status, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);

        return orderRepo.findBySellerAndOrderStatusOrderByOrderedAtDesc(
                shop, status, pageable);
    }


    @Override
    @Transactional
    public void regenerateDeliveryConfirmationCode(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("Regenerating confirmation code - Order ID: {}, Customer: {}",
                orderId, customer.getUserName());

        // Step 1: Validate order and customer
        OrderEntity order = validateOrderForCodeRegeneration(orderId, customer);

        // Step 2: Regenerate code via delivery confirmation service

        String newCode = deliveryConfirmationService.regenerateConfirmationCode(
                orderId,
                customer
        );

        log.info("New confirmation code generated for order: {}", order.getOrderNumber());

        //Todo: Step 3: Placeholder for notification
        //logCodeRegenerationNotification(order, newCode);

    }


    // ========================================
    // HELPER: VALIDATE ORDER ACCESS
    // ========================================

    private void validateOrderAccess(OrderEntity order, AccountEntity requester)
            throws BadRequestException {

        boolean isBuyer = order.getBuyer().getAccountId()
                .equals(requester.getAccountId());

        boolean isSeller = order.getSeller().getOwner() != null &&
                order.getSeller().getOwner().getAccountId()
                        .equals(requester.getAccountId());

        // TODO: Add admin check
        // boolean isAdmin = requester.getRoles().contains(Role.ADMIN);

        if (!isBuyer && !isSeller) {
            throw new BadRequestException(
                    "You do not have access to this order");
        }
    }


    // ========================================
    // DIRECT ORDER CREATION
    // ========================================

    /**
     * Creates order for direct purchase (buy now button).
     * Always creates exactly ONE order with ONE item from ONE shop.
     */
    private List<UUID> createDirectPurchaseOrder(CheckoutSessionEntity session)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════╗");
        log.info("║      DIRECT PURCHASE ORDER             ║");
        log.info("╚════════════════════════════════════════╝");

        // ========================================
        // VALIDATION: Must have exactly 1 item
        // ========================================
        if (session.getItems() == null || session.getItems().size() != 1) {
            throw new BadRequestException(
                    "Direct purchase must have exactly 1 item. Found: " +
                            (session.getItems() != null ? session.getItems().size() : 0)
            );
        }

        CheckoutSessionEntity.CheckoutItem item = session.getItems().get(0);

        log.info("Product: {}", item.getProductName());
        log.info("Quantity: {}", item.getQuantity());
        log.info("Shop: {}", item.getShopName());
        log.info("Total: {} {}", session.getPricing().getTotal(), session.getPricing().getCurrency());

        // ========================================
        // BUILD SINGLE ORDER
        // ========================================
        OrderEntity order = buildSingleOrder(
                session,
                item.getShopId(),
                session.getItems(),  // All items (just 1 in this case)
                OrderSource.DIRECT_PURCHASE
        );

        // ========================================
        // SAVE ORDER
        // ========================================
        OrderEntity savedOrder = orderRepo.save(order);

        log.info("✓ Direct purchase order created");
        log.info("  Order Number: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());

        // ========================================
        // UPDATE CHECKOUT SESSION - NEW WAY
        // ========================================
        session.setCreatedOrderIds(List.of(savedOrder.getOrderId()));
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        sendOrderConfirmationToBuyer(savedOrder);
        sendNewOrderNotificationToSeller(savedOrder);

        log.info("✓ Checkout session completed");

        return List.of(savedOrder.getOrderId());
    }


    /**
     * Creates order(s) for cart purchase.
     * May create MULTIPLE orders if cart has items from different shops.
     * Groups items by shop: one order per shop.
     */
    private List<UUID> createCartPurchaseOrders(CheckoutSessionEntity session)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════╗");
        log.info("║       CART PURCHASE ORDER(S)           ║");
        log.info("╚════════════════════════════════════════╝");

        // ========================================
        // VALIDATION: Must have at least 1 item
        // ========================================
        if (session.getItems() == null || session.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        log.info("Cart has {} items", session.getItems().size());

        // ========================================
        // GROUP ITEMS BY SHOP
        // ========================================
        Map<UUID, List<CheckoutSessionEntity.CheckoutItem>> itemsByShop =
                session.getItems().stream()
                        .collect(Collectors.groupingBy(
                                CheckoutSessionEntity.CheckoutItem::getShopId
                        ));

        int shopCount = itemsByShop.size();
        log.info("Items are from {} different shop(s)", shopCount);

        // Log shop breakdown
        itemsByShop.forEach((shopId, items) -> {
            log.info("  → Shop {}: {} items",
                    items.get(0).getShopName(),
                    items.size());
        });

        // ========================================
        // CREATE ONE ORDER PER SHOP
        // ========================================
        List<UUID> orderIds = new ArrayList<>();
        int orderNumber = 1;

        for (Map.Entry<UUID, List<CheckoutSessionEntity.CheckoutItem>> entry : itemsByShop.entrySet()) {

            UUID shopId = entry.getKey();
            List<CheckoutSessionEntity.CheckoutItem> shopItems = entry.getValue();

            log.info("════════════════════════════════════════");
            log.info("Creating order {}/{} for shop: {}",
                    orderNumber, shopCount, shopItems.getFirst().getShopName());
            log.info("Items: {}", shopItems.size());

            // Build order for this shop
            OrderEntity order = buildSingleOrder(
                    session,
                    shopId,
                    shopItems,
                    OrderSource.CART_PURCHASE
            );

            // Save order
            OrderEntity savedOrder = orderRepo.save(order);
            orderIds.add(savedOrder.getOrderId());

            log.info("✓ Order created: {}", savedOrder.getOrderNumber());
            log.info("  Total: {} {}", savedOrder.getTotalAmount(), savedOrder.getCurrency());

            sendOrderConfirmationToBuyer(savedOrder);
            sendNewOrderNotificationToSeller(savedOrder);

            orderNumber++;
        }

        // ========================================
        // UPDATE CHECKOUT SESSION - NEW WAY
        // ========================================
        session.setCreatedOrderIds(orderIds); // ✅ NEW: Store ALL order IDs

        // Store metadata for convenience
        session.getMetadata().put("orderCount", orderIds.size());
        session.getMetadata().put("primaryOrderId", orderIds.get(0).toString());

        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("════════════════════════════════════════");
        log.info("✓ Cart purchase complete");
        log.info("  Total orders created: {}", orderIds.size());
        log.info("  Order IDs: {}", orderIds);
        log.info("════════════════════════════════════════");

        return orderIds;
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
        OrderItemEntity firstItem = order.getItems().getFirst();
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
        session.setCreatedOrderIds(List.of(savedOrder.getOrderId()));
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("✓ Installment order created: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());

        sendOrderConfirmationToBuyer(savedOrder);
        sendNewOrderNotificationToSeller(savedOrder);

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
        session.setCreatedOrderIds(List.of(savedOrder.getOrderId()));
        session.setCompletedAt(LocalDateTime.now());
        checkoutSessionRepo.save(session);

        log.info("✓ Group order created: {}", savedOrder.getOrderNumber());
        log.info("  Order ID: {}", savedOrder.getOrderId());
        log.info("  Customer: {}", session.getCustomer().getUserName());

        sendOrderConfirmationToBuyer(savedOrder);
        sendNewOrderNotificationToSeller(savedOrder);

        return List.of(savedOrder.getOrderId());
    }

    // ========================================
    // ORDER BUILDER
    // ========================================

    private OrderEntity buildOrderFromCheckoutSession(CheckoutSessionEntity session, OrderSource orderSource) throws ItemNotFoundException {

        // Get shop from first item
        CheckoutSessionEntity.CheckoutItem firstItem = session.getItems().getFirst();
        ShopEntity shop = shopRepo.findById(firstItem.getShopId())
                .orElseThrow(() -> new ItemNotFoundException("Shop not found"));

        // Calculate seller amount (total - platform fee)
        BigDecimal totalAmount = session.getPricing().getTotal();
        BigDecimal platformFeePercent = BigDecimal.ZERO; // 5% platform fee
        BigDecimal platformFee = totalAmount
                .multiply(platformFeePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerAmount = totalAmount.subtract(platformFee);

        // Build order
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
                .totalAmount(totalAmount)
                .platformFee(platformFee)          // ADD THIS
                .sellerAmount(sellerAmount)        // ADD THIS

                // Payment
                .paymentMethod(session.getPaymentIntent().getProvider())
                .amountPaid(totalAmount)
                .amountRemaining(BigDecimal.ZERO)

                // Escrow (will be set if escrow exists)
                .escrowId(null)  // TODO: Get from payment result
                .isEscrowReleased(false)

                // Status
                .orderStatus(OrderStatus.PENDING_SHIPMENT)
                .deliveryStatus(DeliveryStatus.PENDING)

                // Delivery
                .deliveryAddress(serializeAddress(session.getShippingAddress()))
                .deliveryInstructions(null)

                // Tracking
                .trackingNumber(null)
                .carrier(null)

                // Confirmation
                .isDeliveryConfirmed(false)

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

        // FIXED: Initialize the items list before adding items
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        for (CheckoutSessionEntity.CheckoutItem sessionItem : session.getItems()) {

            ProductEntity product = productRepo.findById(sessionItem.getProductId())
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Product not found: " + sessionItem.getProductId()));

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .productName(sessionItem.getProductName())
                    .productImage(sessionItem.getProductImage())
                    .unitPrice(sessionItem.getUnitPrice())
                    .quantity(sessionItem.getQuantity())
                    .build();

            order.getItems().add(orderItem);

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
        if (session.getStatus() != CheckoutSessionStatus.PAYMENT_COMPLETED) {
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


    private OrderEntity buildSingleOrder(
            CheckoutSessionEntity session,
            UUID shopId,
            List<CheckoutSessionEntity.CheckoutItem> items,
            OrderSource orderSource
    ) throws ItemNotFoundException {

        log.info("════════════════════════════════════════");
        log.info("  BUILDING ORDER");
        log.info("════════════════════════════════════════");

        // ========================================
        // 1. FETCH SHOP
        // ========================================
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found: " + shopId));

        log.info("Shop: {}", shop.getShopName());
        log.info("Items: {}", items.size());

        // ========================================
        // 2. CALCULATE TOTALS FOR THESE ITEMS
        // ========================================
        BigDecimal subtotal = items.stream()
                .map(item -> item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // ========================================
        // SHIPPING CALCULATION
        // ========================================
        BigDecimal shippingFee;

        if (orderSource == OrderSource.DIRECT_PURCHASE) {
            // Direct purchase: full shipping cost
            shippingFee = session.getPricing().getShippingCost();
            log.info("Shipping (direct): {} TZS", shippingFee);

        } else {
            // Cart purchase: split shipping among shops
            int totalShops = (int) session.getItems().stream()
                    .map(CheckoutSessionEntity.CheckoutItem::getShopId)
                    .distinct()
                    .count();

            if (totalShops == 1) {
                // Only one shop in cart - full shipping
                shippingFee = session.getPricing().getShippingCost();
            } else {
                // Multiple shops - split shipping equally
                shippingFee = session.getPricing().getShippingCost()
                        .divide(BigDecimal.valueOf(totalShops), 2, RoundingMode.HALF_UP);
            }

            log.info("Shipping (split {}/{}): {} TZS", 1, totalShops, shippingFee);
        }

        BigDecimal tax = BigDecimal.ZERO; // TODO: Calculate tax

        BigDecimal totalAmount = subtotal
                .add(shippingFee)
                .add(tax);

        log.info("Financial breakdown:");
        log.info("  Subtotal: {} TZS", subtotal);
        log.info("  Shipping: {} TZS", shippingFee);
        log.info("  Tax: {} TZS", tax);
        log.info("  Total: {} TZS", totalAmount);

        // ========================================
        // 3. CALCULATE PLATFORM FEE
        // ========================================
        BigDecimal platformFeePercent = BigDecimal.valueOf(5.0);
        BigDecimal platformFee = totalAmount
                .multiply(platformFeePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerAmount = totalAmount.subtract(platformFee);

        log.info("  Platform Fee (5%): {} TZS", platformFee);
        log.info("  Seller Gets (95%): {} TZS", sellerAmount);

        // ========================================
        // 4. CREATE ORDER ENTITY
        // ========================================
        OrderEntity order = OrderEntity.builder()
                // Parties
                .buyer(session.getCustomer())
                .seller(shop)

                // Source & Reference
                .orderSource(orderSource)
                .checkoutSessionId(session.getSessionId())

                // Financial
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .tax(tax)
                .totalAmount(totalAmount)
                .platformFee(platformFee)
                .sellerAmount(sellerAmount)

                // Payment
                .paymentMethod(session.getPaymentIntent().getProvider())
                .amountPaid(totalAmount)
                .amountRemaining(BigDecimal.ZERO)

                // Escrow
                .escrowId(null) // TODO: Link to escrow
                .isEscrowReleased(false)

                // Status
                .orderStatus(OrderStatus.PENDING_SHIPMENT)
                .deliveryStatus(DeliveryStatus.PENDING)

                // Delivery
                .deliveryAddress(serializeAddress(session.getShippingAddress()))
                .deliveryInstructions(null)

                // Tracking
                .trackingNumber(null)
                .carrier(null)

                // Confirmation
                .isDeliveryConfirmed(false)

                .escrowId(session.getEscrowId())

                // Timestamps
                .orderedAt(LocalDateTime.now())

                // Metadata
                .metadata(new HashMap<>())
                .currency(session.getPricing().getCurrency())

                .isDeleted(false)
                .build();

        // ========================================
        // 5. ADD ORDER ITEMS
        // ========================================
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        log.info("Adding order items:");

        for (CheckoutSessionEntity.CheckoutItem sessionItem : items) {

            log.info("  → {} x{}",
                    sessionItem.getProductName(),
                    sessionItem.getQuantity());

            // Fetch product entity
            ProductEntity product = productRepo
                    .findById(sessionItem.getProductId())
                    .orElseThrow(() -> new ItemNotFoundException(
                            "Product not found: " + sessionItem.getProductId()
                    ));

            // Create order item
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .productName(sessionItem.getProductName())
                    .productSlug(sessionItem.getProductSlug())
                    .productImage(sessionItem.getProductImage())
                    .unitPrice(sessionItem.getUnitPrice())
                    .quantity(sessionItem.getQuantity())
                    .build();

            order.getItems().add(orderItem);
        }

        log.info("✓ Order entity built");
        log.info("  Shop: {}", shop.getShopName());
        log.info("  Items: {}", items.size());
        log.info("  Total: {} TZS", totalAmount);

        return order;
    }

    private OrderEntity validateOrderForShipping(UUID orderId, AccountEntity seller)
            throws ItemNotFoundException, BadRequestException {

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found: " + orderId));

        // Validate seller ownership
        if (order.getSeller().getOwner() == null ||
                !order.getSeller().getOwner().getAccountId().equals(seller.getAccountId())) {
            throw new BadRequestException("You are not the seller of this order");
        }

        // Validate order status
        if (order.getOrderStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BadRequestException(
                    String.format("Cannot ship order with status: %s. Order must be PENDING_SHIPMENT",
                            order.getOrderStatus()));
        }

        return order;
    }


    // Verify confirmation code
    private void verifyConfirmationCode(
            UUID orderId,
            String confirmationCode,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException {

        log.info("Verifying confirmation code...");

        try {
            boolean isVerified = deliveryConfirmationService.verifyConfirmationCode(
                    orderId,
                    confirmationCode,
                    customer,
                    ipAddress,
                    deviceInfo
            );

            if (!isVerified) {
                throw new BadRequestException("Invalid confirmation code");
            }

            log.info("Confirmation code verified successfully");

        } catch (BadRequestException e) {
            // Re-throw validation errors (invalid code, max attempts, etc.)
            throw e;
        } catch (Exception e) {
            log.error("Error during code verification", e);
            throw new BadRequestException("Failed to verify confirmation code: " + e.getMessage());
        }
    }

    // Release escrow to seller
    private void releaseEscrow(OrderEntity order) throws ItemNotFoundException, BadRequestException, RandomExceptions {

        if (order.getEscrowId() == null) {
            throw new BadRequestException("No escrow found for this order");
        }

        // Check if already released
        if (order.getIsEscrowReleased() != null && order.getIsEscrowReleased()) {
            log.info("Escrow already released for order {}", order.getOrderNumber());
            return;
        }

        // Call escrow service to release funds
        escrowService.releaseMoney(order.getEscrowId());

        // Update order
        order.setIsEscrowReleased(true);
        order.setEscrowReleasedAt(LocalDateTime.now());
        orderRepo.save(order);


    }

    // Validate order for confirmation
    private OrderEntity validateOrderForConfirmation(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found: " + orderId));

        // Validate customer is the buyer
        if (!order.getBuyer().getAccountId().equals(customer.getAccountId())) {
            throw new BadRequestException("You are not the buyer of this order");
        }

        // Validate order status - must be SHIPPED
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException(
                    String.format("Cannot confirm delivery. Order status: %s. Order must be SHIPPED first.",
                            order.getOrderStatus()));
        }

        // Check if already confirmed
        if (order.getIsDeliveryConfirmed() != null && order.getIsDeliveryConfirmed()) {
            throw new BadRequestException("Delivery already confirmed for this order");
        }

        return order;
    }

    private OrderEntity validateOrderForCodeRegeneration(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found: " + orderId));

        // Validate customer is the buyer
        if (!order.getBuyer().getAccountId().equals(customer.getAccountId())) {
            throw new BadRequestException("You are not the buyer of this order");
        }

        // Validate order status - must be SHIPPED
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException(
                    String.format("Cannot regenerate code. Order status: %s. Order must be SHIPPED.",
                            order.getOrderStatus()));
        }

        // Check if already confirmed
        if (order.getIsDeliveryConfirmed() != null && order.getIsDeliveryConfirmed()) {
            throw new BadRequestException(
                    "Delivery already confirmed - cannot regenerate code");
        }

        return order;
    }

    /**
     * Send order confirmation to BUYER
     * Called after order is created
     */
    private void sendOrderConfirmationToBuyer(OrderEntity order) {
        try {
            log.info("📧 Sending order confirmation to buyer: {}", order.getBuyer().getUserName());

            // 1. Prepare notification data using mapper
            Map<String, Object> data = OrderNotificationMapper.mapOrderConfirmationForBuyer(order);
 
            // 2. Build recipient (BUYER)
            Recipient recipient = Recipient.builder()
                    .userId(order.getBuyer().getId().toString())
                    .email(order.getBuyer().getEmail())
                    .phone(order.getBuyer().getPhoneNumber())
                    .name(order.getBuyer().getFirstName())
                    .language("en")
                    .build();

            // 3. Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.ORDER_CONFIRMATION)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            // 4. Publish notification
            notificationPublisher.publish(event);

            log.info("✅ Order confirmation sent to buyer: order={}, buyer={}",
                    order.getOrderNumber(), order.getBuyer().getUserName());

        } catch (Exception e) {
            log.error("❌ Failed to send order confirmation to buyer: order={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
            // Don't throw - order creation should not fail if notification fails
        }
    }

    /**
     * Send new order notification to SELLER
     * Called after order is created
     */
    private void sendNewOrderNotificationToSeller(OrderEntity order) {
        try {
            log.info("📧 Sending new order notification to seller: {}", order.getSeller().getShopName());

            // Get shop owner account
            AccountEntity shopOwner = order.getSeller().getOwner();

            if (shopOwner == null) {
                log.warn("⚠️ Cannot send notification - shop has no owner: {}", order.getSeller().getShopId());
                return;
            }

            // 1. Prepare notification data using mapper
            Map<String, Object> data = OrderNotificationMapper.mapNewOrderForSeller(order);

            // 2. Build recipient (SELLER/Shop Owner)
            Recipient recipient = Recipient.builder()
                    .userId(shopOwner.getId().toString())
                    .email(shopOwner.getEmail())
                    .phone(shopOwner.getPhoneNumber())
                    .name(shopOwner.getFirstName())
                    .language("en")
                    .build();

            // 3. Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.SHOP_NEW_ORDER)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.HIGH)
                    .data(data)
                    .build();

            // 4. Publish notification
            notificationPublisher.publish(event);

            log.info("✅ New order notification sent to seller: order={}, shop={}, seller={}",
                    order.getOrderNumber(), order.getSeller().getShopName(), shopOwner.getUserName());

        } catch (Exception e) {
            log.error("❌ Failed to send new order notification to seller: order={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
            // Don't throw - order creation should not fail if notification fails
        }
    }


    /**
     * Send order shipped notification to BUYER
     * Called after seller marks order as shipped
     * INCLUDES CONFIRMATION CODE for delivery verification
     */
    private void sendOrderShippedNotification(OrderEntity order, String confirmationCode) {
        try {
            log.info("📧 Sending order shipped notification to buyer: {}", order.getBuyer().getUserName());

            // 1. Prepare notification data using mapper
            Map<String, Object> data = OrderNotificationMapper.mapOrderShippedForBuyer(
                    order,
                    confirmationCode
            );

            // 2. Build recipient (BUYER)
            Recipient recipient = Recipient.builder()
                    .userId(order.getBuyer().getId().toString())
                    .email(order.getBuyer().getEmail())
                    .phone(order.getBuyer().getPhoneNumber())
                    .name(order.getBuyer().getFirstName())
                    .language("en")
                    .build();

            // 3. Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.ORDER_SHIPPED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // 4. Publish notification
            notificationPublisher.publish(event);

            log.info("✅ Order shipped notification sent: order={}, confirmationCode={}",
                    order.getOrderNumber(), confirmationCode);

        } catch (Exception e) {
            log.error("❌ Failed to send order shipped notification: order={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }


    /**
     * Send order delivered notification to SELLER
     * Tells seller that customer confirmed delivery
     */
    private void sendOrderDeliveredNotificationToSeller(OrderEntity order) {
        try {
            log.info("📧 Sending order delivered notification to seller: {}", order.getSeller().getShopName());

            AccountEntity shopOwner = order.getSeller().getOwner();

            if (shopOwner == null) {
                log.warn("⚠️ Cannot send notification - shop has no owner");
                return;
            }

            // 1. Prepare notification data
            Map<String, Object> data = OrderNotificationMapper.mapOrderDeliveredForSeller(order);

            // 2. Build recipient (SELLER)
            Recipient recipient = Recipient.builder()
                    .userId(shopOwner.getId().toString())
                    .email(shopOwner.getEmail())
                    .phone(shopOwner.getPhoneNumber())
                    .name(shopOwner.getFirstName())
                    .language("en")
                    .build();

            // 3. Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.ORDER_DELIVERED)
                    .recipients(List.of(recipient))
                    .channels(List.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.SMS,
                            NotificationChannel.PUSH,
                            NotificationChannel.IN_APP
                    ))
                    .priority(NotificationPriority.NORMAL)
                    .data(data)
                    .build();

            // 4. Publish notification
            notificationPublisher.publish(event);

            log.info("✅ Order delivered notification sent to seller: order={}",
                    order.getOrderNumber());

        } catch (Exception e) {
            log.error("❌ Failed to send order delivered notification to seller: {}", e.getMessage(), e);
        }
    }


    /**
     * Send order delivered notification to BUYER
     * Called after buyer confirms delivery
     */
    private void sendOrderDeliveredNotificationToBuyer(OrderEntity order) {
        try {
            log.info("📧 Sending order delivered notification to buyer: {}", order.getBuyer().getUserName());

            // 1. Prepare notification data using mapper
            Map<String, Object> data = OrderNotificationMapper.mapOrderDeliveredForBuyer(order);

            // 2. Build recipient (BUYER)
            Recipient recipient = Recipient.builder()
                    .userId(order.getBuyer().getId().toString())
                    .email(order.getBuyer().getEmail())
                    .phone(order.getBuyer().getPhoneNumber())
                    .name(order.getBuyer().getFirstName())
                    .language("en")
                    .build();

            // 3. Create notification event
            NotificationEvent event = NotificationEvent.builder()
                    .type(NotificationType.ORDER_DELIVERED)
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

            log.info("✅ Order delivered notification sent: order={}",
                    order.getOrderNumber());

        } catch (Exception e) {
            log.error("❌ Failed to send order delivered notification: order={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }


}