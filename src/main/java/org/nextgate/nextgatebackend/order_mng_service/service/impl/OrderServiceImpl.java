package org.nextgate.nextgatebackend.order_mng_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
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
import java.math.RoundingMode;
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
    // QUERY METHODS - SINGLE ORDER
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public OrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException {

        log.info("Fetching order by ID: {} for user: {}",
                orderId, requester.getUserName());

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

        log.info("Fetching order by number: {} for user: {}",
                orderNumber, requester.getUserName());

        OrderEntity order = orderRepo.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Order not found: " + orderNumber));

        // Validate access
        validateOrderAccess(order, requester);

        return order;
    }


    // ========================================
    // QUERY METHODS - CUSTOMER ORDERS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getMyOrders(AccountEntity customer) {

        log.info("Fetching orders for customer: {}", customer.getUserName());

        return orderRepo.findByBuyerOrderByOrderedAtDesc(customer);
    }


    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getMyOrdersByStatus(
            AccountEntity customer,
            OrderStatus status) {

        log.info("Fetching orders for customer: {} with status: {}",
                customer.getUserName(), status);

        return orderRepo.findByBuyerAndOrderStatusOrderByOrderedAtDesc(
                customer, status);
    }


    // ========================================
    // QUERY METHODS - SHOP ORDERS
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getShopOrders(ShopEntity shop) {

        log.info("Fetching orders for shop: {}", shop.getShopName());

        return orderRepo.findBySellerOrderByOrderedAtDesc(shop);
    }


    @Override
    @Transactional(readOnly = true)
    public List<OrderEntity> getShopOrdersByStatus(
            ShopEntity shop,
            OrderStatus status) {

        log.info("Fetching orders for shop: {} with status: {}",
                shop.getShopName(), status);

        return orderRepo.findBySellerAndOrderStatusOrderByOrderedAtDesc(
                shop, status);
    }



    // ========================================
    // ORDER STATUS UPDATES - SELLER ACTIONS
    // ========================================

    @Override
    @Transactional
    public void markOrderAsShipped(
            UUID orderId,
            String trackingNumber,
            String carrier,
            AccountEntity seller
    ) throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         MARKING ORDER AS SHIPPED                      ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Seller: {}", seller.getUserName());
        log.info("Tracking: {} ({})", trackingNumber, carrier);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Current Status: {}", order.getOrderStatus());
        log.info("Delivery Status: {}", order.getDeliveryStatus());

        // ========================================
        // 2. VALIDATE SELLER
        // ========================================
        if (order.getSeller().getOwner() == null ||
                !order.getSeller().getOwner().getAccountId()
                        .equals(seller.getAccountId())) {
            throw new BadRequestException("You are not the seller of this order");
        }

        log.info("✓ Seller validated");

        // ========================================
        // 3. VALIDATE ORDER STATUS
        // ========================================
        if (order.getOrderStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BadRequestException(
                    String.format("Cannot ship order with status: %s. " +
                                    "Order must be in PENDING_SHIPMENT status.",
                            order.getOrderStatus()));
        }

        log.info("✓ Order status validated");

        // ========================================
        // 4. UPDATE ORDER
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setOrderStatus(OrderStatus.SHIPPED);
        order.setDeliveryStatus(DeliveryStatus.IN_TRANSIT);
        order.setTrackingNumber(trackingNumber);
        order.setCarrier(carrier);
        order.setShippedAt(now);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("✓ Order marked as SHIPPED");
        log.info("  Tracking Number: {}", trackingNumber);
        log.info("  Carrier: {}", carrier);
        log.info("  Shipped At: {}", now);

        // ========================================
        // 5. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send notification to customer
        // notificationService.sendOrderShippedNotification(
        //     order.getBuyer(),
        //     order,
        //     trackingNumber,
        //     carrier
        // );

        log.info("[TODO] Send shipping notification to customer");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         ORDER SHIPPED SUCCESSFULLY                    ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


    @Override
    @Transactional
    public void markOrderAsDelivered(UUID orderId, AccountEntity seller)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         MARKING ORDER AS DELIVERED                    ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Seller: {}", seller.getUserName());

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Current Status: {}", order.getOrderStatus());

        // ========================================
        // 2. VALIDATE SELLER
        // ========================================
        if (order.getSeller().getOwner() == null ||
                !order.getSeller().getOwner().getAccountId()
                        .equals(seller.getAccountId())) {
            throw new BadRequestException("You are not the seller of this order");
        }

        log.info("✓ Seller validated");

        // ========================================
        // 3. VALIDATE ORDER STATUS
        // ========================================
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException(
                    String.format("Cannot mark as delivered. Order status: %s. " +
                                    "Order must be SHIPPED first.",
                            order.getOrderStatus()));
        }

        log.info("✓ Order status validated");

        // ========================================
        // 4. UPDATE ORDER
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED);
        order.setDeliveredAt(now);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("✓ Order marked as DELIVERED");
        log.info("  Delivered At: {}", now);

        // ========================================
        // 5. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send notification to customer to confirm delivery
        // notificationService.sendDeliveryConfirmationRequest(
        //     order.getBuyer(),
        //     order
        // );

        log.info("[TODO] Send delivery confirmation request to customer");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         ORDER DELIVERED - AWAITING CONFIRMATION       ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Customer must confirm to release escrow");
    }


     // ========================================
     // ORDER STATUS UPDATES - CUSTOMER ACTIONS
     // ========================================

    @Override
    @Transactional
    public void confirmDelivery(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         CONFIRMING DELIVERY (CUSTOMER)                ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Customer: {}", customer.getUserName());

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Order Status: {}", order.getOrderStatus());
        log.info("Order Source: {}", order.getOrderSource());
        log.info("Total Amount: {} {}", order.getTotalAmount(), order.getCurrency());

        // ========================================
        // 2. VALIDATE CUSTOMER
        // ========================================
        if (!order.getBuyer().getAccountId().equals(customer.getAccountId())) {
            throw new BadRequestException("You are not the buyer of this order");
        }

        log.info("✓ Customer validated");

        // ========================================
        // 3. VALIDATE ORDER STATUS
        // ========================================
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    String.format("Cannot confirm delivery. Order status: %s. " +
                                    "Order must be DELIVERED first.",
                            order.getOrderStatus()));
        }

        log.info("✓ Order status validated");

        // ========================================
        // 4. CHECK IF ALREADY CONFIRMED
        // ========================================
        if (order.getIsDeliveryConfirmed() != null && order.getIsDeliveryConfirmed()) {
            log.warn("Delivery already confirmed");
            return;
        }

        // ========================================
        // 5. UPDATE ORDER
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setIsDeliveryConfirmed(true);
        order.setDeliveryConfirmedAt(now);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("✓ Delivery confirmed by customer");
        log.info("  Confirmed At: {}", now);
        log.info("  Order Status: COMPLETED");

        // ========================================
        // 6. RELEASE ESCROW (CRITICAL!)
        // ========================================
        try {
            log.info("Releasing escrow for order...");
            releaseEscrowForOrder(orderId);

        } catch (Exception e) {
            log.error("✗ Failed to release escrow", e);

            // TODO: Create admin task for manual escrow release
            // adminTaskService.createUrgentTask(
            //     "Escrow Release Failed",
            //     String.format("Order %s - Customer confirmed but escrow failed",
            //         order.getOrderNumber()),
            //     orderId
            // );

            log.error("[TODO] Create admin task for manual escrow release");

            // Don't throw - delivery is confirmed, escrow can be released manually
        }

        // ========================================
        // 7. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send completion notifications
        // notificationService.sendOrderCompletedNotification(
        //     order.getBuyer(),
        //     order.getSeller(),
        //     order
        // );

        log.info("[TODO] Send order completion notifications");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         DELIVERY CONFIRMED - ORDER COMPLETE           ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


     // ========================================
     // ESCROW MANAGEMENT (CRITICAL!)
     // ========================================

    @Override
    @Transactional
    public void releaseEscrowForOrder(UUID orderId)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         RELEASING ESCROW TO SELLER                    ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Buyer: {}", order.getBuyer().getUserName());
        log.info("Seller: {}", order.getSeller().getShopName());
        log.info("Total Amount: {} {}", order.getTotalAmount(), order.getCurrency());

        // ========================================
        // 2. VALIDATE DELIVERY CONFIRMED
        // ========================================
        if (order.getIsDeliveryConfirmed() == null ||
                !order.getIsDeliveryConfirmed()) {
            throw new BadRequestException(
                    "Cannot release escrow - delivery not confirmed by customer");
        }

        log.info("✓ Delivery confirmed - proceeding with escrow release");

        // ========================================
        // 3. CHECK IF ALREADY RELEASED
        // ========================================
        if (order.getIsEscrowReleased() != null && order.getIsEscrowReleased()) {
            log.warn("Escrow already released for this order");
            return;
        }

        // ========================================
        // 4. GET ESCROW ID
        // ========================================
        UUID escrowId = order.getEscrowId();

        if (escrowId == null) {
            log.error("Order has no escrow ID!");
            throw new BadRequestException(
                    "Cannot release escrow - no escrow found for this order");
        }

        log.info("Escrow ID: {}", escrowId);

        // ========================================
        // 5. RELEASE ESCROW VIA ESCROW SERVICE
        // ========================================
        try {
            log.info("Calling EscrowService to release funds...");

            // TODO: Call EscrowService
            // escrowService.releaseEscrow(escrowId, order.getBuyer());

            log.warn("[TODO] EscrowService.releaseEscrow() not implemented yet");
            log.info("This should:");
            log.info("  1. Move money from escrow to seller wallet");
            log.info("  2. Deduct platform fee");
            log.info("  3. Create ledger entries");
            log.info("  4. Create transaction history");

            // For now, just mark as released
            // TODO: Remove this when EscrowService is implemented
            LocalDateTime now = LocalDateTime.now();
            order.setIsEscrowReleased(true);
            order.setEscrowReleasedAt(now);
            order.setUpdatedAt(now);

            orderRepo.save(order);

            log.info("✓ Order marked as escrow released");

        } catch (Exception e) {
            log.error("Failed to release escrow", e);
            throw new BadRequestException(
                    "Failed to release escrow: " + e.getMessage());
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         ESCROW RELEASED SUCCESSFULLY                  ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Seller: {} received {} {}",
                order.getSeller().getShopName(),
                order.getCurrency());
    }



    // ========================================
// INSTALLMENT-SPECIFIC METHODS
// ========================================

    @Override
    @Transactional
    public void updateInstallmentPaymentProgress(
            UUID orderId,
            BigDecimal amountPaid,
            BigDecimal totalAmount,
            Integer paymentsCompleted,
            Integer totalPayments
    ) throws ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   UPDATING INSTALLMENT PAYMENT PROGRESS               ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Payments: {}/{}", paymentsCompleted, totalPayments);
        log.info("Amount Paid: {} / {}", amountPaid, totalAmount);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Order Source: {}", order.getOrderSource());

        // ========================================
        // 2. VALIDATE ORDER SOURCE
        // ========================================
        if (order.getOrderSource() != OrderSource.INSTALLMENT) {
            log.warn("Order is not an installment order. Skipping update.");
            return;
        }

        log.info("✓ Order source validated: INSTALLMENT");

        // ========================================
        // 3. UPDATE PAYMENT PROGRESS
        // ========================================
        BigDecimal previousAmountPaid = order.getAmountPaid();
        BigDecimal previousAmountRemaining = order.getAmountRemaining();

        order.setAmountPaid(amountPaid);
        order.setAmountRemaining(totalAmount.subtract(amountPaid));
        order.setUpdatedAt(LocalDateTime.now());

        // Store payment progress in metadata
        order.getMetadata().put("paymentsCompleted", paymentsCompleted);
        order.getMetadata().put("totalPayments", totalPayments);
        order.getMetadata().put("paymentProgress",
                String.format("%d/%d", paymentsCompleted, totalPayments));

        orderRepo.save(order);

        log.info("✓ Payment progress updated:");
        log.info("  Amount Paid: {} → {}", previousAmountPaid, amountPaid);
        log.info("  Amount Remaining: {} → {}",
                previousAmountRemaining, order.getAmountRemaining());
        log.info("  Progress: {}/{} payments", paymentsCompleted, totalPayments);

        // ========================================
        // 4. CHECK IF FULLY PAID
        // ========================================
        if (order.getAmountRemaining().compareTo(BigDecimal.ZERO) == 0 ||
                paymentsCompleted.equals(totalPayments)) {

            log.info("🎉 Installment fully paid!");
            markOrderAsFullyPaid(orderId);
        }

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   PAYMENT PROGRESS UPDATE COMPLETE                    ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


    @Override
    @Transactional
    public void markOrderAsFullyPaid(UUID orderId) throws ItemNotFoundException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         MARKING ORDER AS FULLY PAID                   ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Order Source: {}", order.getOrderSource());
        log.info("Total Amount: {} {}", order.getTotalAmount(), order.getCurrency());

        // ========================================
        // 2. UPDATE ORDER
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setAmountPaid(order.getTotalAmount());
        order.setAmountRemaining(BigDecimal.ZERO);
        order.setUpdatedAt(now);

        // Add fully paid flag to metadata
        order.getMetadata().put("fullyPaid", true);
        order.getMetadata().put("fullyPaidAt", now.toString());

        orderRepo.save(order);

        log.info("✓ Order marked as fully paid");
        log.info("  Amount Paid: {} {}", order.getAmountPaid(), order.getCurrency());
        log.info("  Amount Remaining: {} {}", order.getAmountRemaining(), order.getCurrency());
        log.info("  Fully Paid At: {}", now);

        // ========================================
        // 3. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send fully paid notification
        // notificationService.sendInstallmentFullyPaidNotification(
        //     order.getBuyer(),
        //     order
        // );

        log.info("[TODO] Send fully paid notification to customer");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         ORDER FULLY PAID                              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


// ========================================
// ORDER CANCELLATION
// ========================================

    @Override
    @Transactional
    public void cancelOrder(UUID orderId, String reason, AccountEntity actor)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         CANCELLING ORDER                              ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Actor: {}", actor.getUserName());
        log.info("Reason: {}", reason);

        // ========================================
        // 1. FETCH ORDER
        // ========================================
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());
        log.info("Current Status: {}", order.getOrderStatus());
        log.info("Order Source: {}", order.getOrderSource());
        log.info("Total Amount: {} {}", order.getTotalAmount(), order.getCurrency());

        // ========================================
        // 2. VALIDATE ACTOR HAS PERMISSION
        // ========================================
        boolean isBuyer = order.getBuyer().getAccountId()
                .equals(actor.getAccountId());

        boolean isSeller = order.getSeller().getOwner() != null &&
                order.getSeller().getOwner().getAccountId()
                        .equals(actor.getAccountId());

        // TODO: Add admin check
        // boolean isAdmin = actor.getRoles().contains(Role.ADMIN);

        if (!isBuyer && !isSeller) {
            throw new BadRequestException(
                    "You do not have permission to cancel this order");
        }

        log.info("✓ Actor validated: {}", isBuyer ? "BUYER" : "SELLER");

        // ========================================
        // 3. VALIDATE ORDER CAN BE CANCELLED
        // ========================================

        // Cannot cancel if already shipped/delivered/completed
        if (order.getOrderStatus() == OrderStatus.SHIPPED ||
                order.getOrderStatus() == OrderStatus.DELIVERED ||
                order.getOrderStatus() == OrderStatus.COMPLETED) {

            throw new BadRequestException(
                    String.format("Cannot cancel order with status: %s. " +
                                    "Order has already been shipped/delivered.",
                            order.getOrderStatus()));
        }

        // Cannot cancel if already cancelled
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.warn("Order already cancelled");
            return;
        }

        // Buyer can only cancel if not yet shipped
        if (isBuyer && order.getOrderStatus() != OrderStatus.PENDING_SHIPMENT) {
            throw new BadRequestException(
                    "Customer can only cancel before order is shipped");
        }

        log.info("✓ Order can be cancelled");

        // ========================================
        // 4. UPDATE ORDER STATUS
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(now);
        order.setCancelledBy(actor.getAccountId());
        order.setCancellationReason(reason);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("✓ Order marked as CANCELLED");
        log.info("  Cancelled At: {}", now);
        log.info("  Cancelled By: {}", actor.getUserName());
        log.info("  Reason: {}", reason);

        // ========================================
        // 5. HANDLE REFUND
        // ========================================
        log.info("Processing refund...");

        try {
            processRefundForCancelledOrder(order);

        } catch (Exception e) {
            log.error("✗ Failed to process refund", e);

            // TODO: Create admin task for manual refund
            // adminTaskService.createUrgentTask(
            //     "Refund Failed",
            //     String.format("Order %s cancelled but refund failed",
            //         order.getOrderNumber()),
            //     orderId
            // );

            log.error("[TODO] Create admin task for manual refund");

            // Don't throw - order is cancelled, refund can be processed manually
        }

        // ========================================
        // 6. RESTORE INVENTORY
        // ========================================
        // TODO: Restore inventory for cancelled order
        // inventoryService.restoreInventory(order);

        log.info("[TODO] Restore inventory for cancelled order");

        // ========================================
        // 7. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send cancellation notifications
        // notificationService.sendOrderCancelledNotification(
        //     order.getBuyer(),
        //     order.getSeller(),
        //     order,
        //     reason
        // );

        log.info("[TODO] Send cancellation notifications");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║         ORDER CANCELLED SUCCESSFULLY                  ║");
        log.info("╚════════════════════════════════════════════════════════╝");
    }


// ========================================
// HELPER: PROCESS REFUND FOR CANCELLED ORDER
// ========================================

    private void processRefundForCancelledOrder(OrderEntity order)
            throws BadRequestException {

        log.info("Processing refund for cancelled order: {}", order.getOrderNumber());

        // ========================================
        // DETERMINE REFUND AMOUNT
        // ========================================
        BigDecimal refundAmount = order.getAmountPaid();

        if (refundAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("No payment made yet - no refund needed");
            return;
        }

        log.info("Refund Amount: {} {}", refundAmount, order.getCurrency());

        // ========================================
        // HANDLE DIFFERENT ORDER SOURCES
        // ========================================

        switch (order.getOrderSource()) {

            case DIRECT_PURCHASE, CART_PURCHASE -> {
                // Full refund from escrow
                log.info("Direct/Cart purchase - refunding from escrow");

                if (order.getEscrowId() == null) {
                    throw new BadRequestException("No escrow found for this order");
                }

                // TODO: Call EscrowService to refund
                // escrowService.refundEscrow(order.getEscrowId(), order.getBuyer());

                log.warn("[TODO] EscrowService.refundEscrow() not implemented");
                log.info("Should refund {} {} from escrow {} to customer wallet",
                        refundAmount, order.getCurrency(), order.getEscrowId());
            }

            case INSTALLMENT -> {
                // Refund amount already paid
                log.info("Installment purchase - refunding {} {}",
                        refundAmount, order.getCurrency());

                // TODO: Refund to wallet
                // walletService.refund(order.getBuyer(), refundAmount,
                //     "Installment order cancelled");

                log.warn("[TODO] WalletService.refund() not implemented");
                log.info("Should refund {} {} to customer wallet",
                        refundAmount, order.getCurrency());

                // TODO: Also cancel installment agreement
                // installmentService.cancelAgreement(agreementId, reason);

                log.info("[TODO] Cancel associated installment agreement");
            }

            case GROUP_PURCHASE -> {
                // Refund from escrow
                log.info("Group purchase - refunding from escrow");

                if (order.getEscrowId() == null) {
                    throw new BadRequestException("No escrow found for this order");
                }

                // TODO: Call EscrowService to refund
                // escrowService.refundEscrow(order.getEscrowId(), order.getBuyer());

                log.warn("[TODO] EscrowService.refundEscrow() not implemented");
                log.info("Should refund {} {} from escrow {} to customer wallet",
                        refundAmount, order.getCurrency(), order.getEscrowId());
            }
        }

        log.info("✓ Refund processed successfully");
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
            OrderSource orderSource
    ) throws ItemNotFoundException {

        // Get shop from first item
        CheckoutSessionEntity.CheckoutItem firstItem = session.getItems().get(0);
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
                .discount(session.getPricing().getDiscount())
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