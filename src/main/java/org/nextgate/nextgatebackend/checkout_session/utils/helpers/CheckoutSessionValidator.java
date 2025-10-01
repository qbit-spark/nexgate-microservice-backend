// CheckoutSessionValidator.java
package org.nextgate.nextgatebackend.checkout_session.utils.helpers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.cart_service.entity.CartItemEntity;
import org.nextgate.nextgatebackend.cart_service.repo.CartRepo;
import org.nextgate.nextgatebackend.cart_service.service.CartService;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.payload.UpdateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;
import org.nextgate.nextgatebackend.payment_methods.repo.PaymentMethodRepository;
import org.springframework.stereotype.Component;

// Add these imports if not present
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.enums.GroupStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckoutSessionValidator {

    private final PaymentMethodRepository paymentMethodRepository;
    private final WalletService walletService;
    private final CartService cartService;
    private final CartRepo cartRepo;
    // Todo: Add other repositories as needed (ProductRepo, ShippingAddressRepo, etc.)

    // ========================================
    // SESSION TYPE VALIDATION
    // ========================================

    public void validateSessionType(CreateCheckoutSessionRequest request) throws BadRequestException, ItemNotFoundException {
        CheckoutSessionType sessionType = request.getSessionType();

        switch (sessionType) {
            case REGULAR_DIRECTLY -> validateRegularDirectlyRequest(request);
            case REGULAR_CART -> validateAndGetUserCart();
            case GROUP_PURCHASE -> throw new BadRequestException("GROUP_PURCHASE checkout not implemented yet");
            case INSTALLMENT -> throw new BadRequestException("INSTALLMENT checkout not implemented yet");
            default -> throw new BadRequestException("Invalid checkout session type");
        }
    }

    private void validateRegularDirectlyRequest(CreateCheckoutSessionRequest request) throws BadRequestException {
        // REGULAR_DIRECTLY should have exactly 1 item
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Items are required for checkout");
        }

        if (request.getItems().size() > 1) {
            throw new BadRequestException("REGULAR_DIRECTLY checkout supports only 1 item. Use REGULAR_CART for multiple items.");
        }

        // Validate the single item
        CreateCheckoutSessionRequest.CheckoutItemDto item = request.getItems().get(0);
        if (item.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }

        if (item.getQuantity() == null || item.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }
    }

    // ========================================
    // PAYMENT METHOD VALIDATION
    // ========================================

    public PaymentMethodsEntity validateAndGetPaymentMethod(UUID paymentMethodId, AccountEntity user)
            throws ItemNotFoundException, BadRequestException {

        // Check if payment method exists and belongs to user
        PaymentMethodsEntity paymentMethod = paymentMethodRepository
                .findByPaymentMethodIdAndOwner(paymentMethodId, user)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Payment method not found or does not belong to you"));

        // Check if payment method is active
        if (!paymentMethod.getIsActive()) {
            throw new BadRequestException("Payment method is not active");
        }

        // Optional: Check if payment method is verified (depends on business rules)
        // if (!paymentMethod.getIsVerified()) {
        //     throw new BadRequestException("Payment method is not verified");
        // }

        return paymentMethod;
    }

    // ========================================
    // SHIPPING ADDRESS VALIDATION (PLACEHOLDER)
    // ========================================

    public void validateShippingAddress(UUID shippingAddressId, AccountEntity user)
            throws ItemNotFoundException, BadRequestException {

        // Todo: Implement when ShippingAddressService/Repo is available
        // For now, just check it's not null
        if (shippingAddressId == null) {
            throw new BadRequestException("Shipping address is required");
        }

        // Todo: Verify address exists and belongs to user
        // ShippingAddressEntity address = shippingAddressRepo
        //     .findByIdAndUser(shippingAddressId, user)
        //     .orElseThrow(() -> new ItemNotFoundException("Shipping address not found"));
    }

    // ========================================
    // PRODUCT VALIDATION (PLACEHOLDER)
    // ========================================

    public void validateProduct(UUID productId) throws ItemNotFoundException, BadRequestException {

        // Todo: Implement when ProductService/Repo is available
        if (productId == null) {
            throw new BadRequestException("Product ID is required");
        }

        // Todo: Check product exists
        // ProductEntity product = productRepo.findById(productId)
        //     .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Todo: Check product is active/published
        // if (!product.isActive()) {
        //     throw new BadRequestException("Product is not available for purchase");
        // }

        // Todo: Check shop is active
        // if (!product.getShop().isActive()) {
        //     throw new BadRequestException("Shop is not active");
        // }
    }

    // ========================================
    // INVENTORY VALIDATION (PLACEHOLDER)
    // ========================================

    public void validateInventoryAvailability(UUID productId, Integer requestedQuantity)
            throws BadRequestException {

        // Todo: Implement inventory check
        if (requestedQuantity == null || requestedQuantity < 1) {
            throw new BadRequestException("Invalid quantity");
        }

        // Todo: Check available inventory
        // ProductEntity product = productRepo.findById(productId).orElseThrow(...);
        // if (product.getStock() < requestedQuantity) {
        //     throw new BadRequestException("Insufficient inventory. Available: " + product.getStock());
        // }
    }

    // ========================================
    // SHIPPING METHOD VALIDATION (PLACEHOLDER)
    // ========================================

    public void validateShippingMethod(String shippingMethodId) throws BadRequestException {

        // Todo: Implement when ShippingService is available
        if (shippingMethodId == null || shippingMethodId.trim().isEmpty()) {
            throw new BadRequestException("Shipping method is required");
        }

        // Todo: Verify shipping method exists and is available
        // ShippingMethodEntity method = shippingMethodRepo.findById(shippingMethodId)
        //     .orElseThrow(() -> new ItemNotFoundException("Shipping method not found"));
    }

    public PaymentMethodsEntity getOrCreateWalletPaymentMethod(
            UUID paymentMethodId,
            AccountEntity user) throws ItemNotFoundException, BadRequestException {

        // If payment method ID provided, validate and return it
        if (paymentMethodId != null) {
            return validateAndGetPaymentMethod(paymentMethodId, user);
        }

        // Otherwise, use wallet as default payment method
        // Check if user's wallet exists and is active
        WalletEntity wallet = walletService.getWalletByAccountId(user.getAccountId());

        if (!wallet.getIsActive()) {
            throw new BadRequestException("Your wallet is not active. Please activate it or provide a payment method.");
        }

        // Return a virtual PaymentMethodsEntity representing the wallet
        // This is not saved to DB, just used for processing
        return PaymentMethodsEntity.builder()
                .paymentMethodId(null) // Virtual, not in DB
                .owner(user)
                .paymentMethodType(PaymentMethodsType.WALLET)
                .isActive(true)
                .isDefault(false)
                .isVerified(true)
                .build();
    }

    public void validateSessionCanBeUpdated(CheckoutSessionEntity session)
            throws BadRequestException {

        // Cannot update completed sessions
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a completed checkout session");
        }

        // Cannot update if payment is completed
        if (session.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException("Cannot update - payment has been completed");
        }

        // Cannot update cancelled sessions
        if (session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled checkout session");
        }

        // Cannot update expired sessions
        if (session.isExpired()) {
            throw new BadRequestException("Cannot update an expired checkout session");
        }

        // Can only update sessions in PENDING_PAYMENT or PAYMENT_FAILED status
        if (session.getStatus() != CheckoutSessionStatus.PENDING_PAYMENT &&
                session.getStatus() != CheckoutSessionStatus.PAYMENT_FAILED) {
            throw new BadRequestException(
                    "Can only update sessions in PENDING_PAYMENT or PAYMENT_FAILED status"
            );
        }
    }

    public CreateCheckoutSessionRequest convertToCreateRequest(UpdateCheckoutSessionRequest updateRequest) {
        // Helper to convert update request to create request format for billing address determination
        return CreateCheckoutSessionRequest.builder()
                .shippingAddressId(updateRequest.getShippingAddressId())
                .shippingMethodId(updateRequest.getShippingMethodId())
                .paymentMethodId(updateRequest.getPaymentMethodId())
                .metadata(updateRequest.getMetadata())
                .build();
    }

    public PaymentMethodsEntity getPaymentMethodFromIntent(
            CheckoutSessionEntity.PaymentIntent paymentIntent,
            AccountEntity user) throws ItemNotFoundException, BadRequestException {

        // Reconstruct payment method info from intent
        String provider = paymentIntent.getProvider();

        if ("WALLET".equals(provider)) {
            return createVirtualWalletPaymentMethod(user);
        } else if ("CASH_ON_DELIVERY".equals(provider)) {
            return PaymentMethodsEntity.builder()
                    .paymentMethodId(null)
                    .owner(user)
                    .paymentMethodType(PaymentMethodsType.CASH_ON_DELIVERY)
                    .isActive(true)
                    .isDefault(false)
                    .isVerified(true)
                    .build();
        }

        // For other payment methods, we would need to store payment method ID
        throw new BadRequestException("Unable to determine payment method type");
    }

    public PaymentMethodsEntity createVirtualWalletPaymentMethod(AccountEntity user)
            throws ItemNotFoundException, BadRequestException {

        // Verify wallet exists and is active via WalletService
        // This will throw exception if wallet doesn't exist
        WalletEntity wallet = walletService.getWalletByAccountId(user.getAccountId());

        if (!wallet.getIsActive()) {
            throw new BadRequestException(
                    "Your wallet is not active. Please activate your wallet or provide a payment method."
            );
        }

        // Create virtual payment method entity (not persisted to DB)
        return PaymentMethodsEntity.builder()
                .paymentMethodId(null) // Virtual
                .owner(user)
                .paymentMethodType(PaymentMethodsType.WALLET)
                .isActive(true)
                .isDefault(false)
                .isVerified(true)
                .billingAddress(null) // Wallet doesn't need billing address
                .build();
    }


    public void validateSessionCanRetryPayment(CheckoutSessionEntity session)
            throws BadRequestException {

        if (session == null) {
            throw new BadRequestException("Checkout session is null");
        }

        CheckoutSessionStatus status = session.getStatus();

        if (status == null) {
            throw new BadRequestException("Checkout session status is null");
        }

        // Cannot retry completed sessions
        if (status == CheckoutSessionStatus.COMPLETED) {
            throw new BadRequestException(
                    "Cannot retry payment - checkout has already been completed successfully"
            );
        }

        // Cannot retry if payment is successful
        if (status == CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException(
                    "Cannot retry payment - payment has already been completed successfully"
            );
        }

        // Cannot retry cancelled sessions
        if (status == CheckoutSessionStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cannot retry payment - session has been cancelled. Please create a new checkout session."
            );
        }

        // Cannot retry if processing
        if (status == CheckoutSessionStatus.PAYMENT_PROCESSING) {
            throw new BadRequestException(
                    "Payment is currently being processed. Please wait for the current payment to complete."
            );
        }

        // Cannot retry expired sessions
        if (session.isExpired()) {
            throw new BadRequestException(
                    "Cannot retry payment - session has expired. Please create a new checkout session."
            );
        }

        // Can only retry if PAYMENT_FAILED
        if (status != CheckoutSessionStatus.PAYMENT_FAILED) {
            throw new BadRequestException(
                    String.format("Can only retry payment for failed payment attempts. Current status: %s. " +
                            "Expected status: PAYMENT_FAILED", status)
            );
        }

        // Additional safety check - ensure payment intent exists
        if (session.getPaymentIntent() == null) {
            throw new BadRequestException(
                    "Cannot retry payment - no payment intent found for this session. " +
                            "Please update payment method first."
            );
        }
    }



    // ========================================
    // CART VALIDATION
    // ========================================


    public CartEntity validateAndGetUserCart()
            throws ItemNotFoundException {
        CartEntity cart = cartService.initializeCart();

        return cart;
    }

    public void validateCartNotEmpty(CartEntity cart)
            throws BadRequestException {
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Cart is empty. Add items to cart before checkout.");
        }
    }

    public void validateCartItemsAvailability(CartEntity cart)
            throws BadRequestException, ItemNotFoundException {
        for (CartItemEntity cartItem : cart.getCartItems()) {
            validateProduct(cartItem.getProduct().getProductId());
            validateInventoryAvailability(
                    cartItem.getProduct().getProductId(),
                    cartItem.getQuantity()
            );
        }
    }

    // ========================================
// GROUP PURCHASE VALIDATIONS
// ========================================

    public void validateGroupPurchaseRequest(CreateCheckoutSessionRequest request)
            throws BadRequestException {

        // 1. Validate session type is GROUP_PURCHASE
        if (request.getSessionType() != CheckoutSessionType.GROUP_PURCHASE) {
            throw new BadRequestException(
                    "Invalid session type. Expected: GROUP_PURCHASE"
            );
        }

        // 2. Validate exactly 1 item
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("GROUP_PURCHASE must have items");
        }

        if (request.getItems().size() != 1) {
            throw new BadRequestException(
                    "GROUP_PURCHASE must have exactly 1 item"
            );
        }

        // 3. Validate item has productId and quantity
        CreateCheckoutSessionRequest.CheckoutItemDto item = request.getItems().get(0);

        if (item.getProductId() == null) {
            throw new BadRequestException("Product ID is required");
        }

        if (item.getQuantity() == null || item.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        // 4. Validate shipping address is provided
        if (request.getShippingAddressId() == null) {
            throw new BadRequestException("Shipping address is required");
        }

        // 5. Validate shipping method is provided
        if (request.getShippingMethodId() == null ||
                request.getShippingMethodId().trim().isEmpty()) {
            throw new BadRequestException("Shipping method is required");
        }

        log.debug("GROUP_PURCHASE request validation passed");
    }

    public void validateProductForGroupBuying(ProductEntity product)
            throws BadRequestException {

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
        if (product.getGroupTimeLimitHours() == null ||
                product.getGroupTimeLimitHours() <= 0) {
            throw new BadRequestException(
                    "Product group time limit is not set or invalid"
            );
        }

        log.debug("Product validation passed for group buying: {}", product.getProductId());
    }

    public void validateQuantityForGroupBuying(Integer quantity, ProductEntity product)
            throws BadRequestException {

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

    public void validateGroupProductMatch(
            GroupPurchaseInstanceEntity group,
            UUID productId
    ) throws BadRequestException {

        if (!group.getProduct().getProductId().equals(productId)) {
            throw new BadRequestException(
                    String.format("Product mismatch. Group has product: %s, but checkout has: %s",
                            group.getProduct().getProductId(), productId)
            );
        }

        log.debug("Product match validated for group");
    }

}