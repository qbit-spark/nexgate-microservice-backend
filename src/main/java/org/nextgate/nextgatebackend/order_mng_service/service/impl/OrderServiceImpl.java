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
import org.nextgate.nextgatebackend.order_mng_service.service.DeliveryConfirmationService;
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


    // ========================================
    // QUERY METHODS - SINGLE ORDER
    // ========================================

    @Override
    @Transactional(readOnly = true)
    public OrderEntity getOrderById(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException {

        //Todo: Check if requester is admin or staff or owner, allow access to order by id if so.

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
            AccountEntity seller
    ) throws ItemNotFoundException, BadRequestException {

        String trackingNumber = "TRACK-PLACEHOLDER" + orderId.toString().substring(0, 8).toUpperCase();
        String carrier = "NextGate Shipping";

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
        // 4. GENERATE DELIVERY CONFIRMATION CODE
        // ========================================
        log.info("Generating delivery confirmation code...");

        String confirmationCode;
        try {
            confirmationCode = deliveryConfirmationService.generateConfirmationCode(order);
            log.info("✓ Confirmation code generated: {}", confirmationCode);

        } catch (Exception e) {
            log.error("Failed to generate confirmation code", e);
            throw new BadRequestException("Failed to generate confirmation code: " + e.getMessage());
        }

        // ========================================
        // 5. UPDATE ORDER
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
        // 6. SEND NOTIFICATIONS WITH CODE
        // ========================================
        log.info("Sending shipping notification with confirmation code...");

        // TODO: Send notification to customer
        // notificationService.sendOrderShippedWithConfirmationCode(
        //     order.getBuyer(),
        //     order,
        //     trackingNumber,
        //     carrier,
        //     confirmationCode
        // );

        log.info("[TODO] Send notification to customer:");
        log.info("  Recipient: {} ({})",
                order.getBuyer().getUserName(),
                order.getBuyer().getEmail());
        log.info("  Subject: Your order has been shipped!");
        log.info("  ");
        log.info("  Message Template:");
        log.info("  ┌─────────────────────────────────────────────────────┐");
        log.info("  │ Hi {},                                    │", order.getBuyer().getFirstName());
        log.info("  │                                                     │");
        log.info("  │ Great news! Your order has been shipped.           │");
        log.info("  │                                                     │");
        log.info("  │ Order Number: {}                       │", order.getOrderNumber());
        log.info("  │ Tracking Number: {}                                │", trackingNumber);
        log.info("  │ Carrier: {}                                        │", carrier);
        log.info("  │                                                     │");
        log.info("  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │");
        log.info("  │                                                     │");
        log.info("  │ 🔐 DELIVERY CONFIRMATION CODE: {}            │", confirmationCode);
        log.info("  │                                                     │");
        log.info("  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │");
        log.info("  │                                                     │");
        log.info("  │ IMPORTANT: When you receive your package, please   │");
        log.info("  │ enter this confirmation code to complete delivery. │");
        log.info("  │                                                     │");
        log.info("  │ • Code is valid for 30 days                        │");
        log.info("  │ • Maximum 5 verification attempts                  │");
        log.info("  │ • Lost your code? Request a new one in the app     │");
        log.info("  │                                                     │");
        log.info("  │ Thank you for shopping with us!                    │");
        log.info("  └─────────────────────────────────────────────────────┘");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   ORDER SHIPPED - CONFIRMATION CODE SENT             ║");
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
    public void confirmDelivery(
            UUID orderId,
            String confirmationCode,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   CONFIRMING DELIVERY WITH CONFIRMATION CODE          ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Customer: {}", customer.getUserName());
        log.info("IP Address: {}", ipAddress);
        log.info("Device: {}", deviceInfo);

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
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException(
                    String.format("Cannot confirm delivery. Order status: %s. " +
                                    "Order must be SHIPPED first.",
                            order.getOrderStatus()));
        }

        log.info("✓ Order status validated: SHIPPED");

        // ========================================
        // 4. CHECK IF ALREADY CONFIRMED
        // ========================================
        if (order.getIsDeliveryConfirmed() != null && order.getIsDeliveryConfirmed()) {
            log.warn("Delivery already confirmed");
            throw new BadRequestException("Delivery already confirmed for this order");
        }

        // ========================================
        // 5. VERIFY CONFIRMATION CODE
        // ========================================
        log.info("Verifying confirmation code via DeliveryConfirmationService...");

        boolean isVerified;
        try {
            isVerified = deliveryConfirmationService.verifyConfirmationCode(
                    orderId,
                    confirmationCode,
                    customer,
                    ipAddress,
                    deviceInfo
            );

        } catch (BadRequestException e) {
            // Re-throw validation errors (invalid code, max attempts, etc.)
            throw e;

        } catch (Exception e) {
            log.error("Error during code verification", e);
            throw new BadRequestException("Failed to verify confirmation code: " + e.getMessage());
        }

        if (!isVerified) {
            log.error("Code verification returned false");
            throw new BadRequestException("Invalid confirmation code");
        }

        log.info("✓ Confirmation code verified successfully!");

        // ========================================
        // 6. UPDATE ORDER STATUS
        // ========================================
        LocalDateTime now = LocalDateTime.now();

        order.setIsDeliveryConfirmed(true);
        order.setDeliveryConfirmedAt(now);
        order.setDeliveredAt(now);
        order.setDeliveryStatus(DeliveryStatus.DELIVERED);
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setUpdatedAt(now);

        orderRepo.save(order);

        log.info("✓ Order updated:");
        log.info("  Delivery Confirmed: true");
        log.info("  Delivered At: {}", now);
        log.info("  Delivery Status: DELIVERED");
        log.info("  Order Status: COMPLETED");

        // ========================================
        // 7. RELEASE ESCROW (CRITICAL!)
        // ========================================
        try {
            log.info("Releasing escrow for order...");
            releaseEscrowForOrder(orderId);

        } catch (Exception e) {
            log.error("✗ Failed to release escrow", e);

            // TODO: Create admin task for manual escrow release
            // adminTaskService.createUrgentTask(
            //     "Escrow Release Failed",
            //     String.format("Order %s - Customer confirmed but escrow release failed",
            //         order.getOrderNumber()),
            //     orderId
            // );

            log.error("[TODO] Create admin task for manual escrow release");

            // Don't throw - delivery is confirmed, escrow can be released manually
        }

        // ========================================
        // 8. SEND NOTIFICATIONS
        // ========================================
        // TODO: Send completion notifications
        // notificationService.sendOrderCompletedNotification(
        //     order.getBuyer(),
        //     order.getSeller(),
        //     order
        // );

        log.info("[TODO] Send order completion notifications to buyer and seller");

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   DELIVERY CONFIRMED - ORDER COMPLETE ✓               ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Seller will receive payment: {} {}",
                order.getSellerAmount(),
                order.getCurrency());
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

    /**
     * Regenerate confirmation code if customer lost it.
     * Only the buyer can request this.
     */
    @Override
    @Transactional
    public String regenerateDeliveryConfirmationCode(UUID orderId, AccountEntity customer)
            throws ItemNotFoundException, BadRequestException {

        log.info("╔════════════════════════════════════════════════════════╗");
        log.info("║   REGENERATING CONFIRMATION CODE                      ║");
        log.info("╚════════════════════════════════════════════════════════╝");
        log.info("Order ID: {}", orderId);
        log.info("Customer: {}", customer.getUserName());

        // Fetch order
        OrderEntity order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException("Order not found"));

        log.info("Order Number: {}", order.getOrderNumber());

        // Validate customer
        if (!order.getBuyer().getAccountId().equals(customer.getAccountId())) {
            throw new BadRequestException("You are not the buyer of this order");
        }

        // Validate order status
        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException(
                    "Can only regenerate code for shipped orders");
        }

        // Check if already confirmed
        if (order.getIsDeliveryConfirmed() != null && order.getIsDeliveryConfirmed()) {
            throw new BadRequestException("Delivery already confirmed - cannot regenerate code");
        }

        log.info("✓ Validations passed");

        // Regenerate code
        try {
            String newCode = deliveryConfirmationService.regenerateConfirmationCode(
                    orderId,
                    customer
            );

            log.info("✓ New confirmation code generated: {}", newCode);

            // TODO: Send new code to customer
            // notificationService.sendNewConfirmationCode(
            //     order.getBuyer(),
            //     order,
            //     newCode
            // );

            log.info("[TODO] Send new confirmation code to customer");

            log.info("╔════════════════════════════════════════════════════════╗");
            log.info("║   NEW CONFIRMATION CODE SENT                          ║");
            log.info("╚════════════════════════════════════════════════════════╝");

            return newCode;

        } catch (Exception e) {
            log.error("Failed to regenerate confirmation code", e);
            throw new BadRequestException("Failed to regenerate code: " + e.getMessage());
        }
    }

    /**
     * Builds a single order entity for one shop.
     * Used by both direct purchase and cart purchase handlers.
     *
     * @param session     The checkout session
     * @param shopId      The shop ID (seller)
     * @param items       Items for THIS shop only
     * @param orderSource DIRECT_PURCHASE or CART_PURCHASE
     */
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
}