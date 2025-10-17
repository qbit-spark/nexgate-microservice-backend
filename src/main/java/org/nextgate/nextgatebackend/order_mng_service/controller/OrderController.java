package org.nextgate.nextgatebackend.order_mng_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.authentication_service.service.AccountService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.order_mng_service.entity.DeliveryConfirmationEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;
import org.nextgate.nextgatebackend.order_mng_service.enums.OrderStatus;
import org.nextgate.nextgatebackend.order_mng_service.payloads.*;
import org.nextgate.nextgatebackend.order_mng_service.repo.DeliveryConfirmationRepo;
import org.nextgate.nextgatebackend.order_mng_service.service.DeliveryConfirmationService;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.nextgate.nextgatebackend.order_mng_service.utils.OrderMapper;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final DeliveryConfirmationService deliveryConfirmationService;
    private final AccountService accountService;
    private final OrderMapper orderMapper;
    private final AccountRepo accountRepo;
    private final ShopRepo shopRepo;

    // ========================================
    // QUERY ENDPOINTS
    // ========================================

    /**
     * Get order by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrderById(
            @PathVariable UUID orderId,
            Authentication authentication
    ) throws ItemNotFoundException, BadRequestException {


        AccountEntity requester = getAuthenticatedAccount();

        OrderEntity order = orderService.getOrderById(orderId, requester);

        return ResponseEntity.ok(orderMapper.toOrderResponse(order));
    }


    /**
     * Get order by order number
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication
    ) throws ItemNotFoundException, BadRequestException {


        AccountEntity requester = getAuthenticatedAccount();

        OrderEntity order = orderService.getOrderByNumber(orderNumber, requester);

        return ResponseEntity.ok(orderMapper.toOrderResponse(order));
    }


    /**
     * Get my orders (customer)
     */
    @GetMapping("/my-orders")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrders()
            throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<OrderEntity> orders = orderService.getMyOrders(customer);

        return ResponseEntity.ok(orderMapper.toOrderResponseList(orders));
    }


    /**
     * Get my orders by status
     */
    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersByStatus(
            @PathVariable OrderStatus status
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<OrderEntity> orders = orderService.getMyOrdersByStatus(customer, status);

        return ResponseEntity.ok(orderMapper.toOrderResponseList(orders));
    }


    // ========================================
    // SELLER ACTIONS
    // ========================================

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<GlobeSuccessResponseBuilder> markOrderAsShipped(
            @PathVariable UUID orderId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity seller = getAuthenticatedAccount();

        orderService.markOrderAsShipped(
                orderId,
                seller
        );

        // Get active confirmation to include in response
        DeliveryConfirmationEntity confirmation =
                deliveryConfirmationService.getActiveConfirmation(orderId);

        OrderShippedResponse response = OrderShippedResponse.builder()
                .orderId(orderId)
                .orderNumber(confirmation.getOrder().getOrderNumber())
                //.trackingNumber(confirmation.getTrackingNumber())
                //.carrier(request.getCarrier())
                .shippedAt(confirmation.getOrder().getShippedAt())
                .message("Order marked as shipped. Confirmation code sent to customer.")
                .confirmationCodeSent(true)
                .confirmationCodeDestination("email")
                .codeExpiresAt(confirmation.getExpiresAt())
                .maxVerificationAttempts(confirmation.getMaxAttempts())
                .build();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .message("Order shipped successfully")
                        .data(response)
                        .build()
        );
    }


    // ========================================
    // CUSTOMER ACTIONS
    // ========================================

    /**
     * Confirm delivery with confirmation code (Customer only)
     */
    @PostMapping("/{orderId}/confirm-delivery")
    public ResponseEntity<DeliveryConfirmedResponse> confirmDelivery(
            @PathVariable UUID orderId,
            @Valid @RequestBody ConfirmDeliveryRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) throws ItemNotFoundException, BadRequestException {


        AccountEntity customer = getAuthenticatedAccount();

        // Get IP address
        String ipAddress = getClientIpAddress(httpRequest);

        // Get device info (User-Agent)
        String deviceInfo = httpRequest.getHeader("User-Agent");

        orderService.confirmDelivery(
                orderId,
                request.getConfirmationCode(),
                customer,
                ipAddress,
                deviceInfo
        );

        // Get order to build response
        OrderEntity order = orderService.getOrderById(orderId, customer);

        DeliveryConfirmedResponse response = DeliveryConfirmedResponse.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .deliveredAt(order.getDeliveredAt())
                .confirmedAt(order.getDeliveryConfirmedAt())
                .escrowReleased(order.getIsEscrowReleased())
                .sellerAmount(order.getSellerAmount())
                .currency(order.getCurrency())
                .message("Delivery confirmed successfully. Order completed!")
                .build();

        return ResponseEntity.ok(response);
    }


    /**
     * Regenerate confirmation code (Customer only)
     */
    @PostMapping("/{orderId}/regenerate-code")
    public ResponseEntity<GlobeSuccessResponseBuilder> regenerateConfirmationCode(
            @PathVariable UUID orderId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        String newCode = orderService.regenerateDeliveryConfirmationCode(orderId, customer);

        // Get new confirmation
        DeliveryConfirmationEntity confirmation =
                deliveryConfirmationService.getActiveConfirmation(orderId);

        ConfirmationCodeRegeneratedResponse response =
                ConfirmationCodeRegeneratedResponse.builder()
                        .orderId(orderId)
                        .orderNumber(confirmation.getOrder().getOrderNumber())
                        .codeSent(true)
                        .destination("email")
                        .codeExpiresAt(confirmation.getExpiresAt())
                        .maxAttempts(confirmation.getMaxAttempts())
                        .message("New confirmation code sent to your email")
                        .build();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .message("Confirmation code regenerated successfully")
                        .data(response)
                        .build()
        );
    }


    /**
     * Cancel order
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<GlobeSuccessResponseBuilder> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity actor = getAuthenticatedAccount();

        orderService.cancelOrder(orderId, request.getReason(), actor);

        // Get order to build response
        OrderEntity order = orderService.getOrderById(orderId, actor);

        OrderCancelledResponse response = OrderCancelledResponse.builder()
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .cancelledAt(order.getCancelledAt())
                .reason(request.getReason())
                .refundProcessed(true) // TODO: Get actual status from refund service
                .refundAmount(order.getAmountPaid())
                .currency(order.getCurrency())
                .message("Order cancelled successfully. Refund will be processed within 3-5 business days.")
                .build();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .message("Order cancelled successfully")
                        .data(response)
                        .build()
        );
    }


    /**
     * Get my orders with pagination
     */
    @GetMapping("/my-orders/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        Page<OrderEntity> orderPage = orderService.getMyOrdersPaged(customer, page, size);

        return ResponseEntity.ok(orderMapper.toOrderPageResponse(orderPage));
    }


    /**
     * Get my orders by status with pagination
     */
    @GetMapping("/my-orders/status/{status}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersByStatusPaged(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        Page<OrderEntity> orderPage = orderService.getMyOrdersByStatusPaged(
                customer, status, page, size);

        return ResponseEntity.ok(orderMapper.toOrderPageResponse(orderPage));
    }

    // ADD these endpoints to OrderController.java

    /**
     * Get shop orders with pagination (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrdersPaged(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        Page<OrderEntity> orderPage = orderService.getShopOrdersPaged(shop, page, size);

        return ResponseEntity.ok(orderMapper.toOrderPageResponse(orderPage));
    }


    /**
     * Get shop orders by status with pagination (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders/status/{status}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrdersByStatusPaged(
            @PathVariable UUID shopId,
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        Page<OrderEntity> orderPage = orderService.getShopOrdersByStatusPaged(
                shop, status, page, size);

        return ResponseEntity.ok(orderMapper.toOrderPageResponse(orderPage));
    }


    /**
     * Get all non-paginated shop orders (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrders(
            @PathVariable UUID shopId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        List<OrderEntity> orders = orderService.getShopOrders(shop);

        return ResponseEntity.ok(orderMapper.toOrderResponseList(orders));
    }


    /**
     * Get shop orders by status non-paginated (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrdersByStatus(
            @PathVariable UUID shopId,
            @PathVariable OrderStatus status
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        List<OrderEntity> orders = orderService.getShopOrdersByStatus(shop, status);

        return ResponseEntity.ok(orderMapper.toOrderResponseList(orders));
    }


    // ========================================
    // HELPER METHODS
    // ========================================

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }


    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }

    private ShopEntity validateShopOwnership(UUID shopId, AccountEntity user)
            throws ItemNotFoundException, BadRequestException {

        log.info("Validating shop ownership for shop: {} by user: {}",
                shopId, user.getUserName());

        // Fetch shop
        ShopEntity shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new ItemNotFoundException("Shop not found: " + shopId));

        // Check if shop is deleted
        if (shop.getIsDeleted() != null && shop.getIsDeleted()) {
            throw new ItemNotFoundException("Shop not found: " + shopId);
        }

        // Verify ownership
        if (shop.getOwner() == null) {
            log.error("Shop has no owner: {}", shopId);
            throw new BadRequestException("Shop has no owner assigned");
        }

        if (!shop.getOwner().getAccountId().equals(user.getAccountId())) {
            log.warn("Access denied: User {} attempted to access shop {} owned by {}",
                    user.getUserName(), shopId, shop.getOwner().getUserName());
            throw new BadRequestException(
                    "Access denied. You are not the owner of this shop");
        }

        log.info("✓ Shop ownership validated: {} is owner of {}",
                user.getUserName(), shop.getShopName());

        return shop;
    }
}