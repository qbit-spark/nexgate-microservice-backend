package org.nextgate.nextgatebackend.checkout_session.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.checkout_session.service.CheckoutSessionService;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionHelper;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionMapper;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionValidator;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.wallet_service.wallet.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutSessionServiceImpl implements CheckoutSessionService {

    private final AccountRepo accountRepo;
    private final CheckoutSessionRepo checkoutSessionRepo;
    private final CheckoutSessionValidator validator;
    private final CheckoutSessionHelper helper;
    private final CheckoutSessionMapper mapper;
    private final WalletService walletService;

    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException {

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        log.info("Creating checkout session for user: {}", authenticatedUser.getUserName());

        // ========================================
        // 2. VALIDATE SESSION TYPE & DELEGATE
        // ========================================
        validator.validateSessionType(request);

        // Delegate to appropriate handler based on session type
        return switch (request.getSessionType()) {
            case REGULAR_DIRECTLY -> handleRegularDirectlyCheckout(request, authenticatedUser);
            case REGULAR_CART -> throw new BadRequestException("REGULAR_CART checkout not implemented yet");
            case GROUP_PURCHASE -> throw new BadRequestException("GROUP_PURCHASE checkout not implemented yet");
            case INSTALLMENT -> throw new BadRequestException("INSTALLMENT checkout not implemented yet");
        };
    }

    // ========================================
    // REGULAR_DIRECTLY CHECKOUT HANDLER
    // ========================================
    private CheckoutSessionResponse handleRegularDirectlyCheckout(
            CreateCheckoutSessionRequest request,
            AccountEntity authenticatedUser) throws ItemNotFoundException, BadRequestException {

        log.info("Processing REGULAR_DIRECTLY checkout for user: {}", authenticatedUser.getUserName());

        // ========================================
        // 3. VALIDATE & FETCH PAYMENT METHOD
        // ========================================

        PaymentMethodsEntity paymentMethod;

        if (request.getPaymentMethodId() != null) {
            // User provided a payment method - validate it
            paymentMethod = validator.validateAndGetPaymentMethod(
                    request.getPaymentMethodId(),
                    authenticatedUser
            );
            log.info("Payment method validated: {}", paymentMethod.getPaymentMethodType());
        } else {
            // No payment method provided - default to wallet
            // Create a virtual wallet payment method
            paymentMethod = createVirtualWalletPaymentMethod(authenticatedUser);
            log.info("Using default wallet payment method");
        }


        // ========================================
        // 4. VALIDATE SHIPPING ADDRESS
        // ========================================
        validator.validateShippingAddress(request.getShippingAddressId(), authenticatedUser);
        CheckoutSessionEntity.ShippingAddress shippingAddress =
                helper.fetchShippingAddress(request.getShippingAddressId());
        log.info("Shipping address validated");

        // ========================================
        // 5. VALIDATE SHIPPING METHOD
        // ========================================
        validator.validateShippingMethod(request.getShippingMethodId());
        CheckoutSessionEntity.ShippingMethod shippingMethod =
                helper.createPlaceholderShippingMethod(request.getShippingMethodId());
        log.info("Shipping method validated: {}", shippingMethod.getName());

        // ========================================
        // 6. FETCH & VALIDATE PRODUCTS
        // ========================================
        List<CheckoutSessionEntity.CheckoutItem> items = new ArrayList<>();

        for (CreateCheckoutSessionRequest.CheckoutItemDto itemDto : request.getItems()) {
            // Validate product exists and is available
            validator.validateProduct(itemDto.getProductId());
            validator.validateInventoryAvailability(itemDto.getProductId(), itemDto.getQuantity());

            // Fetch product and build checkout item
            CheckoutSessionEntity.CheckoutItem item = helper.fetchAndBuildCheckoutItem(
                    itemDto.getProductId(),
                    itemDto.getQuantity()
            );
            items.add(item);

            log.info("Product added to checkout: {} x{}", item.getProductName(), item.getQuantity());
        }

        // ========================================
        // 7. CALCULATE PRICING
        // ========================================
        CheckoutSessionEntity.PricingSummary pricing = helper.calculatePricing(items, shippingMethod);
        log.info("Pricing calculated - Total: {} {}", pricing.getTotal(), pricing.getCurrency());

        // ========================================
        // 8. DETERMINE BILLING ADDRESS
        // ========================================
        CheckoutSessionEntity.BillingAddress billingAddress =
                helper.determineBillingAddress(request, paymentMethod);

        // ========================================
        // 9. CREATE PAYMENT INTENT
        // ========================================
        CheckoutSessionEntity.PaymentIntent paymentIntent = helper.createPaymentIntent(
                paymentMethod,
                pricing,
                authenticatedUser.getAccountId()
        );
        log.info("Payment intent created: {} - {}", paymentIntent.getProvider(), paymentIntent.getStatus());

        // ========================================
        // 10. CALCULATE EXPIRATION TIMES
        // ========================================
        LocalDateTime sessionExpiration = helper.calculateSessionExpiration();
        LocalDateTime inventoryHoldExpiration = helper.calculateInventoryHoldExpiration();

        // ========================================
        // 11. HOLD INVENTORY
        // ========================================
        for (CheckoutSessionEntity.CheckoutItem item : items) {
            helper.holdInventory(item.getProductId(), item.getQuantity(), inventoryHoldExpiration);
        }
        log.info("Inventory held for {} items until {}", items.size(), inventoryHoldExpiration);

        // ========================================
        // 12. BUILD & SAVE CHECKOUT SESSION ENTITY
        // ========================================
        CheckoutSessionEntity checkoutSession = CheckoutSessionEntity.builder()
                .sessionType(request.getSessionType())
                .customer(authenticatedUser)
                .status(CheckoutSessionStatus.PENDING_PAYMENT)
                .items(items)
                .pricing(pricing)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .shippingMethod(shippingMethod)
                .paymentIntent(paymentIntent)
                .paymentAttempts(new ArrayList<>())
                .inventoryHeld(true)
                .inventoryHoldExpiresAt(inventoryHoldExpiration)
                .metadata(request.getMetadata())
                .expiresAt(sessionExpiration)
                .createdOrderId(null)
                .cartId(null) // For REGULAR_DIRECTLY, no cart reference
                .build();

        // Save to database
        CheckoutSessionEntity savedSession = checkoutSessionRepo.save(checkoutSession);
        log.info("Checkout session created successfully: {}", savedSession.getSessionId());

        // ========================================
        // 13. BUILD & RETURN RESPONSE
        // ========================================
        CheckoutSessionResponse response = mapper.toResponse(savedSession);

        return response;
    }

    // ========================================
    // HELPER METHODS
    // ========================================

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

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account, CheckoutSessionEntity checkoutSession) {
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        boolean isOwner = checkoutSession.getCustomer().getAccountId().equals(account.getAccountId());

        return hasCustomRole || isOwner;
    }

    private boolean validateSystemRolesOrOwner(List<String> customRoles, AccountEntity account) {
        boolean hasCustomRole = account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));

        return hasCustomRole;
    }

    private PaymentMethodsEntity createVirtualWalletPaymentMethod(AccountEntity user)
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
}