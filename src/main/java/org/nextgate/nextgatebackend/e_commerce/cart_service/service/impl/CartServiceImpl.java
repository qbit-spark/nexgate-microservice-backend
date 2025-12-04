package org.nextgate.nextgatebackend.e_commerce.cart_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartItemEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.AddToCartRequest;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.CartResponse;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.UpdateCartItemRequest;
import org.nextgate.nextgatebackend.e_commerce.cart_service.repo.CartItemRepo;
import org.nextgate.nextgatebackend.e_commerce.cart_service.repo.CartRepo;
import org.nextgate.nextgatebackend.e_commerce.cart_service.service.CartService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepo cartRepo;
    private final CartItemRepo cartItemRepo;
    private final ProductRepo productRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder addToCart(AddToCartRequest request)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();
        CartEntity cart = ensureCartExists(user);

        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalseAndStatus(request.getProductId(), ProductStatus.ACTIVE)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Check stock availability
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RandomExceptions(
                    String.format("Insufficient stock for '%s'. Only %d units available",
                            product.getProductName(), product.getStockQuantity()));
        }

        // Check if item already in cart
        Optional<CartItemEntity> existingItem = cartItemRepo.findByCart_UserAndProduct(user, product);

        if (existingItem.isPresent()) {
            // Update existing item
            CartItemEntity cartItem = existingItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();

            if (product.getStockQuantity() < newQuantity) {
                throw new RandomExceptions(
                        String.format("Cannot add more items. Total quantity (%d) would exceed available stock (%d) for '%s'",
                                newQuantity, product.getStockQuantity(), product.getProductName()));
            }

            cartItem.setQuantity(newQuantity);
            cartItemRepo.save(cartItem);

            return GlobeSuccessResponseBuilder.success("Product quantity updated in cart successfully");
        } else {
            // Create new item
            CartItemEntity cartItem = new CartItemEntity();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());

            cartItemRepo.save(cartItem);

            return GlobeSuccessResponseBuilder.success("Product added to cart successfully");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getCart() throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();
        CartEntity cart = ensureCartExists(user);
        List<CartItemEntity> cartItems = cartItemRepo.findByCart_UserOrderByCreatedAtDesc(user);

        CartResponse cartResponse = buildCartResponse(user, cartItems);

        return GlobeSuccessResponseBuilder.success("Shopping cart retrieved successfully", cartResponse);
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder updateCartItem(UUID itemId, UpdateCartItemRequest request)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        CartItemEntity cartItem = cartItemRepo.findByItemIdAndCart_User(itemId, user)
                .orElseThrow(() -> new ItemNotFoundException("Cart item not found"));

        // Validate stock availability
        if (cartItem.getProduct().getStockQuantity() < request.getQuantity()) {
            throw new RandomExceptions(
                    String.format("Insufficient stock for '%s'. Only %d units available",
                            cartItem.getProduct().getProductName(), cartItem.getProduct().getStockQuantity()));
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepo.save(cartItem);

        return GlobeSuccessResponseBuilder.success("Product quantity updated successfully");
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder removeCartItem(UUID itemId) throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();

        CartItemEntity cartItem = cartItemRepo.findByItemIdAndCart_User(itemId, user)
                .orElseThrow(() -> new ItemNotFoundException("Cart item not found"));

        cartItemRepo.delete(cartItem);

        return GlobeSuccessResponseBuilder.success("Product removed from cart successfully");
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder clearCart() throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();
        cartItemRepo.deleteByCart_User(user);

        return GlobeSuccessResponseBuilder.success("Shopping cart cleared successfully");
    }

    @Override
    @Transactional
    public CartEntity initializeCart() throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();
        CartEntity cart = ensureCartExists(user);

        return cart;
    }

    // HELPER METHODS
    private CartEntity ensureCartExists(AccountEntity user) {
        return cartRepo.findByUser(user).orElseGet(() -> {
            CartEntity newCart = new CartEntity();
            newCart.setUser(user);
            CartEntity savedCart = cartRepo.save(newCart);
            log.info("New shopping cart created for user: {}", user.getUserName());
            return savedCart;
        });
    }

    private CartResponse buildCartResponse(AccountEntity user, List<CartItemEntity> cartItems) {

        // Build user summary
        CartResponse.UserSummary userSummary = CartResponse.UserSummary.builder()
                .userId(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .userName(user.getUserName())
                .build();

        // Build cart items
        List<CartResponse.CartItemResponse> itemResponses = cartItems.stream()
                .map(this::buildCartItemResponse)
                .toList();

        // Calculate totals
        CartResponse.CartSummary cartSummary = calculateCartSummary(itemResponses);

        // Get latest update time
        LocalDateTime updatedAt = cartItems.isEmpty() ? LocalDateTime.now() :
                cartItems.stream()
                        .map(CartItemEntity::getUpdatedAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

        return CartResponse.builder()
                .user(userSummary)
                .cartSummary(cartSummary)
                .cartItems(itemResponses)
                .updatedAt(updatedAt)
                .build();
    }

    private CartResponse.CartItemResponse buildCartItemResponse(CartItemEntity cartItem) {
        ProductEntity product = cartItem.getProduct();

        // Get primary image
        String primaryImage = product.getProductImages() != null && !product.getProductImages().isEmpty()
                ? product.getProductImages().get(0) : null;

        // Real-time price calculations
        BigDecimal unitPrice = product.getPrice();
        BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));


        return CartResponse.CartItemResponse.builder()
                .itemId(cartItem.getItemId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productImage(primaryImage)
                .unitPrice(unitPrice)
                .quantity(cartItem.getQuantity())
                .itemSubtotal(itemSubtotal)
                .totalPrice(itemSubtotal)
                .shop(CartResponse.ShopSummary.builder()
                        .shopId(product.getShop().getShopId())
                        .shopName(product.getShop().getShopName())
                        .shopSlug(product.getShop().getShopSlug())
                        .logoUrl(product.getShop().getLogoUrl())
                        .build())
                .availability(CartResponse.ProductAvailability.builder()
                        .inStock(product.isInStock())
                        .stockQuantity(product.getStockQuantity())
                        .build())
                .addedAt(cartItem.getCreatedAt())
                .build();
    }

    private CartResponse.CartSummary calculateCartSummary(List<CartResponse.CartItemResponse> items) {
        int totalItems = items.size();
        int totalQuantity = items.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum();

        BigDecimal totalAmount = items.stream()
                .map(CartResponse.CartItemResponse::getItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.CartSummary.builder()
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .subtotal(totalAmount)
                .totalDiscount(BigDecimal.ZERO)  // ← Set to ZERO instead of null
                .totalAmount(totalAmount)
                .build();
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

}