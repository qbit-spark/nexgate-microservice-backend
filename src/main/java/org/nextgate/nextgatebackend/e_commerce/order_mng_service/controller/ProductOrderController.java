package org.nextgate.nextgatebackend.e_commerce.order_mng_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.authentication_service.service.AccountService;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.ConfirmDeliveryRequest;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.ConfirmationCodeRegeneratedResponse;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.DeliveryConfirmedResponse;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads.ProductOrderShippedResponse;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.DeliveryConfirmationEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.entity.ProductOrderEntity;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums.ProductOrderStatus;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.service.DeliveryConfirmationService;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.service.ProductOrderService;
import org.nextgate.nextgatebackend.e_commerce.order_mng_service.utils.ProductOrderMapper;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.entity.ShopEntity;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
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
public class ProductOrderController {

    private final ProductOrderService productOrderService;
    private final DeliveryConfirmationService deliveryConfirmationService;
    private final AccountService accountService;
    private final ProductOrderMapper productOrderMapper;
    private final AccountRepo accountRepo;
    private final ShopRepo shopRepo;


    @GetMapping("/{orderId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrderById(
            @PathVariable UUID orderId,
            Authentication authentication
    ) throws ItemNotFoundException, BadRequestException {


        AccountEntity requester = getAuthenticatedAccount();

        ProductOrderEntity order = productOrderService.getOrderById(orderId, requester);

        return ResponseEntity.ok(productOrderMapper.toOrderResponse(order));
    }



    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOrderByNumber(
            @PathVariable String orderNumber,
            Authentication authentication
    ) throws ItemNotFoundException, BadRequestException {


        AccountEntity requester = getAuthenticatedAccount();

        ProductOrderEntity order = productOrderService.getOrderByNumber(orderNumber, requester);

        return ResponseEntity.ok(productOrderMapper.toOrderResponse(order));
    }



    @GetMapping("/my-orders")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrders()
            throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<ProductOrderEntity> orders = productOrderService.getMyOrders(customer);

        return ResponseEntity.ok(productOrderMapper.toOrderResponseList(orders));
    }


    /**
     * Get my orders by status
     */
    @GetMapping("/my-orders/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersByStatus(
            @PathVariable ProductOrderStatus status
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        List<ProductOrderEntity> orders = productOrderService.getMyOrdersByStatus(customer, status);

        return ResponseEntity.ok(productOrderMapper.toOrderResponseList(orders));
    }


    // ========================================
    // SELLER ACTIONS
    // ========================================

    @PostMapping("/{orderId}/ship")
    public ResponseEntity<GlobeSuccessResponseBuilder> markOrderAsShipped(
            @PathVariable UUID orderId
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity seller = getAuthenticatedAccount();

        productOrderService.markOrderAsShipped(orderId, seller);

        // Get active confirmation to include in response
        DeliveryConfirmationEntity confirmation = deliveryConfirmationService.getActiveConfirmation(orderId);

        ProductOrderShippedResponse response = ProductOrderShippedResponse.builder()
                .orderId(orderId)
                .orderNumber(confirmation.getOrder().getOrderNumber())
                .shippedAt(confirmation.getOrder().getShippedAt())
                .message("Order marked as shipped. Confirmation code sent to customer.")
                .confirmationCodeSent(true)
                .codeExpiresAt(confirmation.getExpiresAt())
                .maxVerificationAttempts(confirmation.getMaxAttempts())
                .build();

        return ResponseEntity.ok(
                GlobeSuccessResponseBuilder.builder()
                        .message("Order marked as shipped")
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
    ) throws ItemNotFoundException, BadRequestException, RandomExceptions {


        AccountEntity customer = getAuthenticatedAccount();

        // Get IP address
        String ipAddress = getClientIpAddress(httpRequest);

        // Get device info (User-Agent)
        String deviceInfo = httpRequest.getHeader("User-Agent");

        productOrderService.confirmDelivery(
                orderId,
                request.getConfirmationCode(),
                customer,
                ipAddress,
                deviceInfo
        );

        // Get order to build response
        ProductOrderEntity order = productOrderService.getOrderById(orderId, customer);

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

        // Service handles everything
        productOrderService.regenerateDeliveryConfirmationCode(orderId, customer);

        // Get confirmation details for response
        DeliveryConfirmationEntity confirmation =
                deliveryConfirmationService.getActiveConfirmation(orderId);

        // Build response
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
     * Get my orders with pagination
     */
    @GetMapping("/my-orders/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersPaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        Page<ProductOrderEntity> orderPage = productOrderService.getMyOrdersPaged(customer, page, size);

        return ResponseEntity.ok(productOrderMapper.toOrderPageResponse(orderPage));
    }


    /**
     * Get my orders by status with pagination
     */
    @GetMapping("/my-orders/status/{status}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyOrdersByStatusPaged(
            @PathVariable ProductOrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        Page<ProductOrderEntity> orderPage = productOrderService.getMyOrdersByStatusPaged(
                customer, status, page, size);

        return ResponseEntity.ok(productOrderMapper.toOrderPageResponse(orderPage));
    }


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

        Page<ProductOrderEntity> orderPage = productOrderService.getShopOrdersPaged(shop, page, size);

        return ResponseEntity.ok(productOrderMapper.toOrderPageResponse(orderPage));
    }


    /**
     * Get shop orders by status with pagination (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders/status/{status}/paged")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrdersByStatusPaged(
            @PathVariable UUID shopId,
            @PathVariable ProductOrderStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        Page<ProductOrderEntity> orderPage = productOrderService.getShopOrdersByStatusPaged(
                shop, status, page, size);

        return ResponseEntity.ok(productOrderMapper.toOrderPageResponse(orderPage));
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

        List<ProductOrderEntity> orders = productOrderService.getShopOrders(shop);

        return ResponseEntity.ok(productOrderMapper.toOrderResponseList(orders));
    }


    /**
     * Get shop orders by status non-paginated (Shop Owner only)
     */
    @GetMapping("/shop/{shopId}/orders/status/{status}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getShopOrdersByStatus(
            @PathVariable UUID shopId,
            @PathVariable ProductOrderStatus status
    ) throws ItemNotFoundException, BadRequestException {

        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Get shop and verify ownership
        ShopEntity shop = validateShopOwnership(shopId, authenticatedUser);

        List<ProductOrderEntity> orders = productOrderService.getShopOrdersByStatus(shop, status);

        return ResponseEntity.ok(productOrderMapper.toOrderResponseList(orders));
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