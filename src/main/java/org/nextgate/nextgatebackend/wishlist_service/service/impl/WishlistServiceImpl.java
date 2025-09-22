package org.nextgate.nextgatebackend.wishlist_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.cart_service.payload.AddToCartRequest;
import org.nextgate.nextgatebackend.cart_service.service.CartService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.wishlist_service.entity.WishlistItemEntity;
import org.nextgate.nextgatebackend.wishlist_service.payload.AddToWishlistRequest;
import org.nextgate.nextgatebackend.wishlist_service.payload.WishlistResponse;
import org.nextgate.nextgatebackend.wishlist_service.repo.WishlistItemRepo;
import org.nextgate.nextgatebackend.wishlist_service.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepo wishlistItemRepo;
    private final ProductRepo productRepo;
    private final AccountRepo accountRepo;
    private final CartService cartService;

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder addToWishlist(AddToWishlistRequest request)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Check if already in wishlist
        if (wishlistItemRepo.existsByUserAndProduct(user, product)) {
            throw new RandomExceptions(
                    String.format("'%s' is already in your wishlist", product.getProductName()));
        }

        // Add to wishlist
        WishlistItemEntity wishlistItem = new WishlistItemEntity();
        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);

        wishlistItemRepo.save(wishlistItem);

        return GlobeSuccessResponseBuilder.success("Product added to wishlist successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public GlobeSuccessResponseBuilder getWishlist() throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();
        List<WishlistItemEntity> wishlistItems = wishlistItemRepo.findByUserOrderByCreatedAtDesc(user);

        WishlistResponse wishlistResponse = buildWishlistResponse(user, wishlistItems);

        return GlobeSuccessResponseBuilder.success("Wishlist retrieved successfully", wishlistResponse);
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder removeFromWishlist(UUID itemId) throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();

        //Check if this item belong to this user
        wishlistItemRepo.findByWishlistIdAndUser(itemId, user)
                .orElseThrow(() -> new ItemNotFoundException("Wishlist item not found"));

        wishlistItemRepo.deleteById(itemId);

        return GlobeSuccessResponseBuilder.success("Product removed from wishlist successfully");
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder clearWishlist() throws ItemNotFoundException {

        AccountEntity user = getAuthenticatedAccount();
        wishlistItemRepo.deleteByUser(user);

        return GlobeSuccessResponseBuilder.success("Wishlist cleared successfully");
    }

    @Override
    @Transactional
    public GlobeSuccessResponseBuilder moveToCart(UUID itemId, Integer quantity)
            throws ItemNotFoundException, RandomExceptions {

        AccountEntity user = getAuthenticatedAccount();

        // Get wishlist item
        WishlistItemEntity wishlistItem = wishlistItemRepo.findByWishlistIdAndUser(itemId, user)
                .orElseThrow(() -> new ItemNotFoundException("Wishlist item not found"));

        UUID productId = wishlistItem.getProduct().getProductId();

        // Add to cart
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(productId);
        cartRequest.setQuantity(quantity != null ? quantity : 1);

        cartService.addToCart(cartRequest);

        // Remove from wishlist
       // removeFromWishlist(productId);

        return GlobeSuccessResponseBuilder.success("Product moved to cart successfully");
    }

    // HELPER METHODS
    private WishlistResponse buildWishlistResponse(AccountEntity user, List<WishlistItemEntity> wishlistItems) {

        // Build user summary
        WishlistResponse.UserSummary userSummary = WishlistResponse.UserSummary.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .name(user.getFirstName() + " " + user.getLastName())
                .build();

        // Build wishlist items
        List<WishlistResponse.WishlistItemResponse> itemResponses = wishlistItems.stream()
                .map(this::buildWishlistItemResponse)
                .toList();

        // Calculate summary
        WishlistResponse.WishlistSummary wishlistSummary = calculateWishlistSummary(itemResponses);

        // Get latest update time
        LocalDateTime updatedAt = wishlistItems.isEmpty() ? LocalDateTime.now() :
                wishlistItems.stream()
                        .map(WishlistItemEntity::getCreatedAt)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

        return WishlistResponse.builder()
                .user(userSummary)
                .wishlistSummary(wishlistSummary)
                .wishlistItems(itemResponses)
                .updatedAt(updatedAt)
                .build();
    }

    private WishlistResponse.WishlistItemResponse buildWishlistItemResponse(WishlistItemEntity wishlistItem) {
        ProductEntity product = wishlistItem.getProduct();

        // Get primary image
        String primaryImage = product.getProductImages() != null && !product.getProductImages().isEmpty()
                ? product.getProductImages().get(0) : null;

        // Real-time price calculations
        BigDecimal unitPrice = product.getPrice();
        BigDecimal discountAmount = BigDecimal.ZERO;
        Boolean isOnSale = false;

        if (product.isOnSale()) {
            discountAmount = product.getDiscountAmount();
            isOnSale = true;
        }

        return WishlistResponse.WishlistItemResponse.builder()
                .wishlistId(wishlistItem.getWishlistId())
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productImage(primaryImage)
                .unitPrice(unitPrice)
                .discountAmount(discountAmount)
                .isOnSale(isOnSale)
                .shop(WishlistResponse.ShopSummary.builder()
                        .shopId(product.getShop().getShopId())
                        .shopName(product.getShop().getShopName())
                        .shopSlug(product.getShop().getShopSlug())
                        .logoUrl(product.getShop().getLogoUrl())
                        .build())
                .availability(WishlistResponse.ProductAvailability.builder()
                        .inStock(product.isInStock())
                        .stockQuantity(product.getStockQuantity())
                        .build())
                .addedAt(wishlistItem.getCreatedAt())
                .build();
    }

    private WishlistResponse.WishlistSummary calculateWishlistSummary(List<WishlistResponse.WishlistItemResponse> items) {
        int totalItems = items.size();

        BigDecimal totalValue = items.stream()
                .map(WishlistResponse.WishlistItemResponse::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int inStockItems = (int) items.stream()
                .filter(item -> item.getAvailability().getInStock())
                .count();

        int outOfStockItems = totalItems - inStockItems;

        return WishlistResponse.WishlistSummary.builder()
                .totalItems(totalItems)
                .totalValue(totalValue)
                .inStockItems(inStockItems)
                .outOfStockItems(outOfStockItems)
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