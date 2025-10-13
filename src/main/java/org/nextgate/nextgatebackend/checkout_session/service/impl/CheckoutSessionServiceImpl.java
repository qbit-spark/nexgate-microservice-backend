package org.nextgate.nextgatebackend.checkout_session.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.payload.UpdateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.checkout_session.repo.CheckoutSessionRepo;
import org.nextgate.nextgatebackend.checkout_session.service.CheckoutSessionService;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionHelper;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionMapper;
import org.nextgate.nextgatebackend.checkout_session.utils.helpers.CheckoutSessionValidator;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.PaymentOrchestrator;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.service.WalletService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.group_purchase_mng.service.GroupPurchaseService;
import org.nextgate.nextgatebackend.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.installment_purchase.utils.InstallmentCalculator;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.enums.PaymentMethodsType;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
    private final PaymentOrchestrator paymentOrchestrator;
    private final ProductRepo productRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final InstallmentPlanRepo installmentPlanRepo;
    private final InstallmentCalculator installmentCalculator;

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
            case REGULAR_CART -> handleRegularCartCheckout(request, authenticatedUser);
            case GROUP_PURCHASE -> handleGroupPurchaseCheckout(request, authenticatedUser);
            case INSTALLMENT -> handleInstallmentCheckout(request, authenticatedUser);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSessionResponse getCheckoutSessionById(UUID sessionId)
            throws ItemNotFoundException {

        log.info("Fetching checkout session: {}", sessionId);

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // ========================================
        // 2. FETCH CHECKOUT SESSION
        // ========================================
        CheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, authenticatedUser)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found or you don't have permission to access it"));

        // ========================================
        // 3. CHECK IF SESSION IS EXPIRED
        // ========================================
        if (session.isExpired()) {
            log.warn("Requested checkout session {} is expired", sessionId);
            // Still return it, but the client can check the status/expiresAt
        }

        // ========================================
        // 4. MAP TO RESPONSE & RETURN
        // ========================================
        CheckoutSessionResponse response = mapper.toResponse(session);
        log.info("Successfully retrieved checkout session: {}", sessionId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckoutSessionSummaryResponse> getMyCheckoutSessions()
            throws ItemNotFoundException {

        log.info("Fetching all checkout sessions for authenticated user");

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // ========================================
        // 2. FETCH ALL SESSIONS FOR USER
        // ========================================
        List<CheckoutSessionEntity> sessions = checkoutSessionRepo
                .findByCustomerOrderByCreatedAtDesc(authenticatedUser);

        log.info("Found {} checkout sessions for user: {}",
                sessions.size(), authenticatedUser.getUserName());

        // ========================================
        // 3. MAP TO SUMMARY RESPONSES
        // ========================================
        List<CheckoutSessionSummaryResponse> responses = mapper.toSummaryResponseList(sessions);

        return responses;
    }

     // ========================================
     // CANCEL CHECKOUT SESSION
    // ========================================
    @Override
    @Transactional
    public void cancelCheckoutSession(UUID sessionId)
            throws ItemNotFoundException, BadRequestException {

        log.info("Cancelling checkout session: {}", sessionId);

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // ========================================
        // 2. FETCH CHECKOUT SESSION
        // ========================================
        CheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, authenticatedUser)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found or you don't have permission to access it"));

        // ========================================
        // 3. VALIDATE SESSION CAN BE CANCELLED
        // ========================================
        // Cannot cancel if already completed
        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed checkout session");
        }

        // Cannot cancel if payment is already successful
        if (session.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException("Cannot cancel - payment has been completed. Please contact support.");
        }

        // Cannot cancel if already cancelled
        if (session.getStatus() == CheckoutSessionStatus.CANCELLED) {
            throw new BadRequestException("Checkout session is already cancelled");
        }

        // ========================================
        // 4. RELEASE HELD INVENTORY
        // ========================================
        if (session.getInventoryHeld() != null && session.getInventoryHeld()) {
            for (CheckoutSessionEntity.CheckoutItem item : session.getItems()) {
                helper.releaseInventory(item.getProductId(), item.getQuantity());
                log.info("Released inventory for product: {} quantity: {}",
                        item.getProductId(), item.getQuantity());
            }
        }

        // ========================================
        // 5. UPDATE SESSION STATUS
        // ========================================
        session.setStatus(CheckoutSessionStatus.CANCELLED);
        session.setInventoryHeld(false);
        session.setUpdatedAt(LocalDateTime.now());

        // ========================================
        // 6. SAVE UPDATED SESSION
        // ========================================
        checkoutSessionRepo.save(session);

        log.info("Checkout session {} cancelled successfully by user: {}",
                sessionId, authenticatedUser.getUserName());
    }


    // ========================================
   // UPDATE CHECKOUT SESSION
   // ========================================
    @Override
    @Transactional
    public CheckoutSessionResponse updateCheckoutSession(
            UUID sessionId,
            UpdateCheckoutSessionRequest request)
            throws ItemNotFoundException, BadRequestException {

        log.info("Updating checkout session: {}", sessionId);

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // ========================================
        // 2. FETCH CHECKOUT SESSION
        // ========================================
        CheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, authenticatedUser)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found or you don't have permission to access it"));

        // ========================================
        // 3. VALIDATE SESSION CAN BE UPDATED
        // ========================================
        validator.validateSessionCanBeUpdated(session);

        // ========================================
        // 4. TRACK IF PRICING NEEDS RECALCULATION
        // ========================================
        boolean needsPricingRecalculation = false;

        // ========================================
        // 5. UPDATE SHIPPING ADDRESS (if provided)
        // ========================================
        if (request.getShippingAddressId() != null) {
            log.info("Updating shipping address to: {}", request.getShippingAddressId());

            validator.validateShippingAddress(request.getShippingAddressId(), authenticatedUser);
            CheckoutSessionEntity.ShippingAddress newShippingAddress =
                    helper.fetchShippingAddress(request.getShippingAddressId());

            session.setShippingAddress(newShippingAddress);
            log.info("Shipping address updated successfully");
        }

        // ========================================
        // 6. UPDATE SHIPPING METHOD (if provided)
        // ========================================
        if (request.getShippingMethodId() != null) {
            log.info("Updating shipping method to: {}", request.getShippingMethodId());

            validator.validateShippingMethod(request.getShippingMethodId());
            CheckoutSessionEntity.ShippingMethod newShippingMethod =
                    helper.createPlaceholderShippingMethod(request.getShippingMethodId());

            session.setShippingMethod(newShippingMethod);
            needsPricingRecalculation = true; // Shipping cost affects total
            log.info("Shipping method updated successfully: {}", newShippingMethod.getName());
        }

        // ========================================
        // 7. UPDATE PAYMENT METHOD (if provided)
        // ========================================
        if (request.getPaymentMethodId() != null) {
            log.info("Updating payment method to: {}", request.getPaymentMethodId());

            PaymentMethodsEntity newPaymentMethod = validator.validateAndGetPaymentMethod(
                    request.getPaymentMethodId(),
                    authenticatedUser
            );

            // Update billing address based on new payment method
            CheckoutSessionEntity.BillingAddress newBillingAddress =
                    helper.determineBillingAddress(
                            validator.convertToCreateRequest(request),
                            newPaymentMethod
                    );
            session.setBillingAddress(newBillingAddress);

            // Create new payment intent for the new payment method
            CheckoutSessionEntity.PaymentIntent newPaymentIntent =
                    helper.createPaymentIntent(
                            newPaymentMethod,
                            session.getPricing(),
                            authenticatedUser.getAccountId()
                    );
            session.setPaymentIntent(newPaymentIntent);

            log.info("Payment method updated successfully: {}", newPaymentMethod.getPaymentMethodType());
        }

        // ========================================
        // 8. UPDATE METADATA (if provided)
        // ========================================
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            log.info("Updating metadata");

            // Merge with existing metadata (don't replace completely)
            Map<String, Object> existingMetadata = session.getMetadata();
            if (existingMetadata == null) {
                existingMetadata = new HashMap<>();
            }
            existingMetadata.putAll(request.getMetadata());
            session.setMetadata(existingMetadata);

            log.info("Metadata updated successfully");
        }

        // ========================================
        // 9. RECALCULATE PRICING (if needed)
        // ========================================
        if (needsPricingRecalculation) {
            log.info("Recalculating pricing due to shipping method change");

            CheckoutSessionEntity.PricingSummary newPricing =
                    helper.calculatePricing(session.getItems(), session.getShippingMethod());
            session.setPricing(newPricing);

            // Update payment intent with new total if payment method exists
            if (session.getPaymentIntent() != null) {
                CheckoutSessionEntity.PaymentIntent updatedPaymentIntent =
                        helper.createPaymentIntent(
                                validator.getPaymentMethodFromIntent(session.getPaymentIntent(), authenticatedUser),
                                newPricing,
                                authenticatedUser.getAccountId()
                        );
                session.setPaymentIntent(updatedPaymentIntent);
            }

            log.info("Pricing recalculated - New total: {} {}",
                    newPricing.getTotal(), newPricing.getCurrency());
        }

        // ========================================
        // 10. UPDATE TIMESTAMP
        // ========================================
        session.setUpdatedAt(LocalDateTime.now());

        // ========================================
        // 11. SAVE UPDATED SESSION
        // ========================================
        CheckoutSessionEntity updatedSession = checkoutSessionRepo.save(session);
        log.info("Checkout session {} updated successfully", sessionId);

        // ========================================
        // 12. BUILD & RETURN RESPONSE
        // ========================================
        return mapper.toResponse(updatedSession);
    }



    @Override
    @Transactional
    public PaymentResponse retryPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions {

        log.info("Retrying payment for checkout session: {}", sessionId);

        // Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Fetch checkout session
        CheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, authenticatedUser)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found or you don't have permission to access it"));

        // Validate session can retry payment
        if (session.getStatus() != CheckoutSessionStatus.PAYMENT_FAILED) {
            throw new BadRequestException(
                    String.format("Cannot retry payment - session status: %s. Expected: PAYMENT_FAILED",
                            session.getStatus())
            );
        }

        // Check if expired
        if (session.isExpired()) {
            session.setStatus(CheckoutSessionStatus.EXPIRED);
            checkoutSessionRepo.save(session);
            throw new BadRequestException("Checkout session has expired. Please create a new checkout session.");
        }

        // Check payment attempt limit (max 5 attempts)
        int attemptCount = session.getPaymentAttemptCount();
        final int MAX_PAYMENT_ATTEMPTS = 5;

        if (attemptCount >= MAX_PAYMENT_ATTEMPTS) {
            // Exceed max attempts → mark as EXPIRED
            session.setStatus(CheckoutSessionStatus.EXPIRED);
            session.setUpdatedAt(LocalDateTime.now());
            checkoutSessionRepo.save(session);

            throw new BadRequestException(
                    String.format("Maximum payment attempts (%d) exceeded. Please create a new checkout session.",
                            MAX_PAYMENT_ATTEMPTS)
            );
        }

        // Verify inventory still available
        for (CheckoutSessionEntity.CheckoutItem item : session.getItems()) {
            try {
                validator.validateInventoryAvailability(item.getProductId(), item.getQuantity());
            } catch (BadRequestException e) {
                throw new BadRequestException(
                        String.format("Product '%s' is no longer available in requested quantity. " +
                                "Please create a new checkout session.", item.getProductName())
                );
            }
        }

        // Verify payment method still valid
        if (session.getPaymentIntent() == null) {
            throw new BadRequestException("No payment intent found for this session");
        }

        PaymentMethodsEntity paymentMethod = helper.getPaymentMethodFromIntent(
                session.getPaymentIntent(),
                authenticatedUser
        );

        // For wallet payments, check balance
        if (paymentMethod.getPaymentMethodType() == PaymentMethodsType.WALLET) {
            WalletEntity wallet = walletService.getWalletByAccountId(authenticatedUser.getAccountId());

            if (!wallet.getIsActive()) {
                throw new BadRequestException(
                        "Your wallet is not active. Please update your payment method."
                );
            }

            BigDecimal walletBalance = walletService.getMyWalletBalance();
            if (walletBalance.compareTo(session.getPricing().getTotal()) < 0) {
                throw new BadRequestException(
                        String.format("Insufficient wallet balance. Required: %s TZS, Available: %s TZS. " +
                                        "Please top up your wallet or update your payment method.",
                                session.getPricing().getTotal(), walletBalance)
                );
            }
        }

        // Extend session expiration for retry
        LocalDateTime newExpiration = helper.calculateSessionExpiration();
        session.setExpiresAt(newExpiration);

        // Re-hold inventory (was released after previous failure)
        LocalDateTime newInventoryHoldExpiration = helper.calculateInventoryHoldExpiration();
        session.setInventoryHoldExpiresAt(newInventoryHoldExpiration);

        for (CheckoutSessionEntity.CheckoutItem item : session.getItems()) {
            helper.holdInventory(item.getProductId(), item.getQuantity(), newInventoryHoldExpiration);
        }
        session.setInventoryHeld(true);

        log.info("Inventory re-held until: {}", newInventoryHoldExpiration);

        // Record retry attempt
        CheckoutSessionEntity.PaymentAttempt attempt = CheckoutSessionEntity.PaymentAttempt.builder()
                .attemptNumber(attemptCount + 1)
                .paymentMethod(paymentMethod.getPaymentMethodType().toString())
                .status("RETRY_INITIATED")
                .errorMessage(null)
                .attemptedAt(LocalDateTime.now())
                .transactionId(null)
                .build();

        session.addPaymentAttempt(attempt);

        // Update session status back to PENDING_PAYMENT
        session.setStatus(CheckoutSessionStatus.PENDING_PAYMENT);
        session.setUpdatedAt(LocalDateTime.now());

        checkoutSessionRepo.save(session);

        log.info("Payment retry validated for session: {}. Attempt #{}", sessionId, attemptCount + 1);

        // Now process the payment
        return paymentOrchestrator.processPayment(sessionId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<CheckoutSessionSummaryResponse> getMyActiveCheckoutSessions()
            throws ItemNotFoundException {

        log.info("Fetching active checkout sessions for authenticated user");

        // ========================================
        // 1. GET AUTHENTICATED USER
        // ========================================
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // ========================================
        // 2. FETCH PENDING_PAYMENT SESSIONS
        // ========================================
        List<CheckoutSessionEntity> pendingSessions = checkoutSessionRepo
                .findByCustomerAndStatus(authenticatedUser, CheckoutSessionStatus.PENDING_PAYMENT);

        // ========================================
        // 3. FETCH PAYMENT_FAILED SESSIONS
        // ========================================
        List<CheckoutSessionEntity> failedSessions = checkoutSessionRepo
                .findByCustomerAndStatus(authenticatedUser, CheckoutSessionStatus.PAYMENT_FAILED);

        log.info("Found {} PENDING_PAYMENT and {} PAYMENT_FAILED sessions for user: {}",
                pendingSessions.size(), failedSessions.size(), authenticatedUser.getUserName());

        // ========================================
        // 4. COMBINE AND FILTER OUT EXPIRED SESSIONS
        // ========================================
        List<CheckoutSessionEntity> allActiveSessions = new ArrayList<>();
        allActiveSessions.addAll(pendingSessions);
        allActiveSessions.addAll(failedSessions);

        List<CheckoutSessionEntity> activeSessions = allActiveSessions.stream()
                .filter(session -> !session.isExpired())
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt())) // Most recent first
                .toList();

        log.info("After filtering expired sessions, {} sessions are active",
                activeSessions.size());

        // ========================================
        // 5. MAP TO SUMMARY RESPONSES
        // ========================================
        List<CheckoutSessionSummaryResponse> responses = mapper.toSummaryResponseList(activeSessions);

        return responses;
    }


    @Override
    @Transactional
    public PaymentResponse processPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions {

        log.info("Processing payment for checkout session: {}", sessionId);

        // Get authenticated user
        AccountEntity authenticatedUser = getAuthenticatedAccount();

        // Fetch checkout session and verify ownership
        CheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, authenticatedUser)
                .orElseThrow(() -> new ItemNotFoundException(
                        "Checkout session not found or you don't have permission to access it"));

        // Validate session can be paid
        if (session.getStatus() != CheckoutSessionStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Cannot process payment - session is not pending: " + session.getStatus());
        }

        if (session.isExpired()) {
            throw new BadRequestException("Checkout session has expired");
        }

        // Delegate to payment orchestrator
        return paymentOrchestrator.processPayment(sessionId);
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

        return account.getRoles().stream()
                .anyMatch(role -> customRoles.contains(role.getRoleName()));
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
            paymentMethod = validator.createVirtualWalletPaymentMethod(authenticatedUser);
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
                .createdOrderIds(null)
                .cartId(null) // For REGULAR_DIRECTLY, no cart reference
                .build();

        // Save to database
        CheckoutSessionEntity savedSession = checkoutSessionRepo.save(checkoutSession);
        log.info("Checkout session created successfully: {}", savedSession.getSessionId());

        // ========================================
        // 13. BUILD & RETURN RESPONSE
        // ========================================

        return mapper.toResponse(savedSession);
    }


    // ========================================
    // REGULAR_CART CHECKOUT HANDLER
    // ========================================
    private CheckoutSessionResponse handleRegularCartCheckout(
            CreateCheckoutSessionRequest request,
            AccountEntity authenticatedUser) throws ItemNotFoundException, BadRequestException {

        log.info("Processing REGULAR_CART checkout for user: {}", authenticatedUser.getUserName());

        // ========================================
        // 3. GET & VALIDATE USER'S CART
        // ========================================
        CartEntity cart = validator.validateAndGetUserCart();
        log.info("Retrieved cart: {} with {} items", cart.getCartId(), cart.getCartItems().size());

        // Validate cart is not empty
        validator.validateCartNotEmpty(cart);
        log.info("Cart is not empty");

        // Validate all cart items are still available
        validator.validateCartItemsAvailability(cart);
        log.info("All cart items are available");

        // ========================================
        // 4. CONVERT CART ITEMS TO CHECKOUT ITEMS
        // ========================================
        List<CheckoutSessionEntity.CheckoutItem> items = helper.convertCartItemsToCheckoutItems(cart);
        log.info("Converted {} cart items to checkout items", items.size());

        // ========================================
        // 5. VALIDATE & FETCH PAYMENT METHOD
        // ========================================
        PaymentMethodsEntity paymentMethod;

        if (request.getPaymentMethodId() != null) {
            paymentMethod = validator.validateAndGetPaymentMethod(
                    request.getPaymentMethodId(),
                    authenticatedUser
            );
            log.info("Payment method validated: {}", paymentMethod.getPaymentMethodType());
        } else {
            paymentMethod = helper.createVirtualWalletPaymentMethod(authenticatedUser);
            log.info("Using default wallet payment method");
        }

        // ========================================
        // 6. VALIDATE SHIPPING ADDRESS
        // ========================================
        validator.validateShippingAddress(request.getShippingAddressId(), authenticatedUser);
        CheckoutSessionEntity.ShippingAddress shippingAddress =
                helper.fetchShippingAddress(request.getShippingAddressId());
        log.info("Shipping address validated");

        // ========================================
        // 7. VALIDATE SHIPPING METHOD
        // ========================================
        validator.validateShippingMethod(request.getShippingMethodId());
        CheckoutSessionEntity.ShippingMethod shippingMethod =
                helper.createPlaceholderShippingMethod(request.getShippingMethodId());
        log.info("Shipping method validated: {}", shippingMethod.getName());

        // ========================================
        // 8. CALCULATE PRICING
        // ========================================
        CheckoutSessionEntity.PricingSummary pricing = helper.calculatePricing(items, shippingMethod);
        log.info("Pricing calculated - Total: {} {}", pricing.getTotal(), pricing.getCurrency());

        // ========================================
        // 9. DETERMINE BILLING ADDRESS
        // ========================================
        CheckoutSessionEntity.BillingAddress billingAddress =
                helper.determineBillingAddress(request, paymentMethod);

        // ========================================
        // 10. CREATE PAYMENT INTENT
        // ========================================
        CheckoutSessionEntity.PaymentIntent paymentIntent = helper.createPaymentIntent(
                paymentMethod,
                pricing,
                authenticatedUser.getAccountId()
        );
        log.info("Payment intent created: {} - {}", paymentIntent.getProvider(), paymentIntent.getStatus());

        // ========================================
        // 11. CALCULATE EXPIRATION TIMES
        // ========================================
        LocalDateTime sessionExpiration = helper.calculateSessionExpiration();
        LocalDateTime inventoryHoldExpiration = helper.calculateInventoryHoldExpiration();

        // ========================================
        // 12. HOLD INVENTORY
        // ========================================
        for (CheckoutSessionEntity.CheckoutItem item : items) {
            helper.holdInventory(item.getProductId(), item.getQuantity(), inventoryHoldExpiration);
        }
        log.info("Inventory held for {} items until {}", items.size(), inventoryHoldExpiration);

        // ========================================
        // 13. BUILD & SAVE CHECKOUT SESSION ENTITY
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
                .createdOrderIds(null)
                .cartId(cart.getCartId()) // Link to cart
                .build();

        // Save to database
        CheckoutSessionEntity savedSession = checkoutSessionRepo.save(checkoutSession);
        log.info("Checkout session created successfully: {}", savedSession.getSessionId());

        // ========================================
        // 14. BUILD & RETURN RESPONSE
        // ========================================

        return mapper.toResponse(savedSession);
    }


    // ========================================
    // GROUP_PURCHASE CHECKOUT HANDLER
    // ========================================

    public CheckoutSessionResponse handleGroupPurchaseCheckout(
            CreateCheckoutSessionRequest request,
            AccountEntity authenticatedUser) throws ItemNotFoundException, BadRequestException {

        log.info("Processing GROUP_PURCHASE checkout for user: {}",
                authenticatedUser.getUserName());

        // 1. Validate request
        validator.validateGroupPurchaseRequest(request);

        // 2. Get groupInstanceId (nullable)
        UUID groupInstanceId = request.getGroupInstanceId();

        // 3. Extract product and quantity
        UUID productId = request.getItems().getFirst().getProductId();
        Integer quantity = request.getItems().getFirst().getQuantity();

        // 4. Fetch and validate product
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        validator.validateProductForGroupBuying(product);
        validator.validateQuantityForGroupBuying(quantity, product);

        // 5. If joining existing group, validate it
        if (groupInstanceId != null) {
            GroupPurchaseInstanceEntity group =
                    groupPurchaseInstanceRepo.findById(groupInstanceId)
                            .orElseThrow(() -> new ItemNotFoundException("Group not found"));

            validator.validateGroupIsJoinable(group);
            validator.validateSeatsAvailable(group, quantity);

            // Validate product matches
            if (!group.getProduct().getProductId().equals(productId)) {
                throw new BadRequestException("Product mismatch with group");
            }
        }

        // 6. Force payment method to WALLET
        PaymentMethodsEntity paymentMethod =
                validator.createVirtualWalletPaymentMethod(authenticatedUser);

        // 7. Validate wallet balance
        BigDecimal groupPrice = product.getGroupPrice();
        BigDecimal totalAmount = groupPrice.multiply(BigDecimal.valueOf(quantity));

        BigDecimal walletBalance = walletService.getMyWalletBalance();
        if (walletBalance.compareTo(totalAmount) < 0) {
            throw new BadRequestException(
                    String.format("Insufficient wallet balance. Required: %s TZS, Available: %s TZS",
                            totalAmount, walletBalance)
            );
        }

        // 8. Validate shipping address
        validator.validateShippingAddress(request.getShippingAddressId(), authenticatedUser);
        CheckoutSessionEntity.ShippingAddress shippingAddress =
                helper.fetchShippingAddress(request.getShippingAddressId());

        // 9. Validate shipping method
        validator.validateShippingMethod(request.getShippingMethodId());
        CheckoutSessionEntity.ShippingMethod shippingMethod =
                helper.createPlaceholderShippingMethod(request.getShippingMethodId());

        // 10. Build checkout item (using groupPrice)
        CheckoutSessionEntity.CheckoutItem item =
                helper.buildGroupPurchaseCheckoutItem(product, quantity);

        // 11. Calculate pricing (using groupPrice)
        CheckoutSessionEntity.PricingSummary pricing =
                helper.calculateGroupPurchasePricing(List.of(item), shippingMethod);

        // 12. Determine billing address
        CheckoutSessionEntity.BillingAddress billingAddress =
                helper.determineBillingAddress(request, paymentMethod);

        // 13. Create payment intent (WALLET only)
        CheckoutSessionEntity.PaymentIntent paymentIntent =
                helper.createPaymentIntent(paymentMethod, pricing, authenticatedUser.getAccountId());

        // 14. Calculate expiration times
        LocalDateTime sessionExpiration = helper.calculateSessionExpiration();
        LocalDateTime inventoryHoldExpiration = helper.calculateInventoryHoldExpiration();

        // 15. Hold inventory
        helper.holdInventory(productId, quantity, inventoryHoldExpiration);

        // 16. Build and save checkout session
        CheckoutSessionEntity checkoutSession = CheckoutSessionEntity.builder()
                .sessionType(CheckoutSessionType.GROUP_PURCHASE)
                .customer(authenticatedUser)
                .status(CheckoutSessionStatus.PENDING_PAYMENT)
                .items(List.of(item))
                .pricing(pricing)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .groupIdToBeJoined(groupInstanceId)
                .shippingMethod(shippingMethod)
                .paymentIntent(paymentIntent)
                .paymentAttempts(new ArrayList<>())
                .inventoryHeld(true)
                .inventoryHoldExpiresAt(inventoryHoldExpiration)
                .metadata(request.getMetadata()) // Contains groupInstanceId if joining
                .expiresAt(sessionExpiration)
                .build();

        CheckoutSessionEntity savedSession = checkoutSessionRepo.save(checkoutSession);

        log.info("GROUP_PURCHASE checkout session created: {}", savedSession.getSessionId());

        // 17. Return response
        return mapper.toResponse(savedSession);
    }




       // ========================================
      // INSTALLMENT CHECKOUT HANDLER
     // ========================================

    private CheckoutSessionResponse handleInstallmentCheckout(
            CreateCheckoutSessionRequest request,
            AccountEntity authenticatedUser) throws ItemNotFoundException, BadRequestException {

        log.info("Processing INSTALLMENT checkout for user: {}", authenticatedUser.getUserName());

        // ========================================
        // 1. VALIDATE REQUEST
        // ========================================
        validator.validateInstallmentRequest(request);

        // ========================================
        // 2. EXTRACT DATA FROM REQUEST
        // ========================================
        UUID productId = request.getItems().get(0).getProductId();
        Integer quantity = request.getItems().get(0).getQuantity();
        UUID planId = request.getInstallmentPlanId();
        Integer downPaymentPercent = request.getDownPaymentPercent();

        log.debug("Extracted - Product: {}, Qty: {}, Plan: {}, Down: {}%",
                productId, quantity, planId, downPaymentPercent);

        // ========================================
        // 3. FETCH AND VALIDATE PRODUCT
        // ========================================
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        validator.validateProductForInstallment(product);
        validator.validateQuantityForInstallment(quantity, product);

        log.debug("Product validated: {}", product.getProductName());

        // ========================================
        // 4. FETCH AND VALIDATE INSTALLMENT PLAN
        // ========================================
        InstallmentPlanEntity plan = installmentPlanRepo.findById(planId)
                .orElseThrow(() -> new ItemNotFoundException("Installment plan not found"));

        validator.validateInstallmentPlan(plan, product);
        validator.validateDownPaymentPercent(downPaymentPercent, plan);

        log.debug("Plan validated: {}", plan.getPlanName());

        // ========================================
        // 5. CALCULATE INSTALLMENT CONFIGURATION
        // ========================================
        CheckoutSessionEntity.InstallmentConfiguration installmentConfig =
                installmentCalculator.calculateInstallmentConfig(
                        plan,
                        product.getPrice(),
                        quantity,
                        downPaymentPercent
                );

        log.info("Installment config calculated - Down: {}, Monthly: {}, Total: {}",
                installmentConfig.getDownPaymentAmount(),
                installmentConfig.getMonthlyPaymentAmount(),
                installmentConfig.getTotalAmount());

        // ========================================
        // 6. FORCE PAYMENT METHOD TO WALLET
        // ========================================
        // Similar to group purchase, installments require wallet
        PaymentMethodsEntity paymentMethod =
                validator.createVirtualWalletPaymentMethod(authenticatedUser);

        log.info("Using wallet payment method for installment");

        // ========================================
        // 7. VALIDATE WALLET BALANCE FOR DOWN PAYMENT
        // ========================================
        BigDecimal downPaymentAmount = installmentConfig.getDownPaymentAmount();
        BigDecimal walletBalance = walletService.getMyWalletBalance();

        if (walletBalance.compareTo(downPaymentAmount) < 0) {
            throw new BadRequestException(
                    String.format("Insufficient wallet balance. Required: %s TZS, Available: %s TZS",
                            downPaymentAmount, walletBalance)
            );
        }

        log.debug("Wallet balance sufficient: {} TZS", walletBalance);

        // ========================================
        // 8. VALIDATE SHIPPING ADDRESS
        // ========================================
        validator.validateShippingAddress(request.getShippingAddressId(), authenticatedUser);
        CheckoutSessionEntity.ShippingAddress shippingAddress =
                helper.fetchShippingAddress(request.getShippingAddressId());

        log.debug("Shipping address validated");

        // ========================================
        // 9. VALIDATE SHIPPING METHOD
        // ========================================
        validator.validateShippingMethod(request.getShippingMethodId());
        CheckoutSessionEntity.ShippingMethod shippingMethod =
                helper.createPlaceholderShippingMethod(request.getShippingMethodId());

        log.debug("Shipping method validated: {}", shippingMethod.getName());

        // ========================================
        // 10. BUILD CHECKOUT ITEM (Using Regular Price)
        // ========================================
        CheckoutSessionEntity.CheckoutItem item = helper.fetchAndBuildCheckoutItem(
                productId,
                quantity
        );

        log.debug("Checkout item built: {}", item.getProductName());

        // ========================================
        // 11. CALCULATE PRICING (DOWN PAYMENT ONLY!)
        // ========================================
        // CRITICAL: pricing.total = DOWN PAYMENT, not full product price
        CheckoutSessionEntity.PricingSummary pricing =
                CheckoutSessionEntity.PricingSummary.builder()
                        .subtotal(downPaymentAmount)
                        .shippingCost(BigDecimal.ZERO) // Can add shipping cost if needed
                        .tax(BigDecimal.ZERO) // Can add tax if needed
                        .total(downPaymentAmount) // ← ONLY DOWN PAYMENT!
                        .currency("TZS")
                        .build();

        log.info("Pricing calculated - Total (down payment): {} TZS", pricing.getTotal());

        // ========================================
        // 12. DETERMINE BILLING ADDRESS
        // ========================================
        CheckoutSessionEntity.BillingAddress billingAddress =
                helper.determineBillingAddress(request, paymentMethod);

        // ========================================
        // 13. CREATE PAYMENT INTENT (WALLET ONLY)
        // ========================================
        CheckoutSessionEntity.PaymentIntent paymentIntent =
                helper.createPaymentIntent(paymentMethod, pricing, authenticatedUser.getAccountId());

        log.debug("Payment intent created: WALLET - {}", paymentIntent.getStatus());

        // ========================================
        // 14. CALCULATE EXPIRATION TIMES
        // ========================================
        LocalDateTime sessionExpiration = helper.calculateSessionExpiration();
        LocalDateTime inventoryHoldExpiration = helper.calculateInventoryHoldExpiration();

        // ========================================
        // 15. HOLD INVENTORY
        // ========================================
        helper.holdInventory(productId, quantity, inventoryHoldExpiration);

        log.info("Inventory held for {} items until {}", quantity, inventoryHoldExpiration);

        // ========================================
        // 16. BUILD AND SAVE CHECKOUT SESSION ENTITY
        // ========================================
        CheckoutSessionEntity checkoutSession = CheckoutSessionEntity.builder()
                .sessionType(CheckoutSessionType.INSTALLMENT)
                .customer(authenticatedUser)
                .status(CheckoutSessionStatus.PENDING_PAYMENT)
                .items(List.of(item))
                .pricing(pricing) // ← DOWN PAYMENT ONLY
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .shippingMethod(shippingMethod)
                .paymentIntent(paymentIntent)
                .paymentAttempts(new ArrayList<>())
                .inventoryHeld(true)
                .inventoryHoldExpiresAt(inventoryHoldExpiration)
                .metadata(request.getMetadata())
                .expiresAt(sessionExpiration)
                .createdOrderIds(null) // Will be set after agreement creation
                .cartId(null)
                .groupIdToBeJoined(null)
                // NEW: Installment-specific fields
                .selectedInstallmentPlanId(planId)
                .installmentConfig(installmentConfig) // ← FULL CONFIG STORED HERE
                .build();

        // ========================================
        // 17. SAVE TO DATABASE
        // ========================================
        CheckoutSessionEntity savedSession = checkoutSessionRepo.save(checkoutSession);

        log.info("INSTALLMENT checkout session created: {} - Down payment: {} TZS",
                savedSession.getSessionId(), downPaymentAmount);

        // ========================================
        // 18. BUILD AND RETURN RESPONSE
        // ========================================
        return mapper.toResponse(savedSession);
    }

}