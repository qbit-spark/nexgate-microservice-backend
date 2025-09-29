package org.nextgate.nextgatebackend.checkout_session.utils.helpers;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.products_mng_service.products.enums.ProductStatus;
import org.nextgate.nextgatebackend.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.wallet_service.wallet.service.WalletService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckoutSessionHelper {

    private final WalletService walletService;
    private final ProductRepo productRepo;

    // ========================================
    // BILLING ADDRESS DETERMINATION
    // ========================================

    public CheckoutSessionEntity.BillingAddress determineBillingAddress(
            CreateCheckoutSessionRequest request,
            PaymentMethodsEntity paymentMethod) {

        // If payment method has billing address, use it
        if (paymentMethod.getBillingAddress() != null) {
            return convertPaymentMethodBillingToCheckoutBilling(paymentMethod.getBillingAddress());
        }

        // Otherwise, return null (don't fallback to shipping)
        return null;
    }

    private CheckoutSessionEntity.BillingAddress convertPaymentMethodBillingToCheckoutBilling(
            PaymentMethodsEntity.BillingAddress pmBilling) {

        return CheckoutSessionEntity.BillingAddress.builder()
                .sameAsShipping(false)
                .fullName(null)
                .addressLine1(pmBilling.getStreet())
                .city(pmBilling.getCity())
                .state(pmBilling.getState())
                .postalCode(pmBilling.getPostalCode())
                .country(pmBilling.getCountry())
                .build();
    }

    // ========================================
    // SHIPPING ADDRESS FETCHING (PLACEHOLDER)
    // ========================================

    public CheckoutSessionEntity.ShippingAddress fetchShippingAddress(UUID shippingAddressId) {

        // Todo: Fetch from ShippingAddressService/Repo when available
        return CheckoutSessionEntity.ShippingAddress.builder()
                .fullName("John Doe")
                .addressLine1("123 Main Street")
                .addressLine2(null)
                .city("Dar es Salaam")
                .state("Dar es Salaam Region")
                .postalCode("12345")
                .country("Tanzania")
                .phone("+255123456789")
                .build();
    }

    // ========================================
    // SHIPPING METHOD CREATION (PLACEHOLDER)
    // ========================================

    public CheckoutSessionEntity.ShippingMethod createPlaceholderShippingMethod(String shippingMethodId) {

        // Todo: Fetch from ShippingService when available
        return CheckoutSessionEntity.ShippingMethod.builder()
                .id(shippingMethodId)
                .name("Standard Shipping")
                .carrier("DHL")
                .cost(BigDecimal.valueOf(5000)) // 5000 TZS
                .estimatedDays("3-5 business days")
                .estimatedDelivery(LocalDateTime.now().plusDays(5).toString())
                .build();
    }

    // ========================================
    // PRODUCT FETCHING & VALIDATION - REAL IMPLEMENTATION
    // ========================================

    public CheckoutSessionEntity.CheckoutItem fetchAndBuildCheckoutItem(
            UUID productId,
            Integer quantity) throws ItemNotFoundException, BadRequestException {

        // Fetch real product from database
        ProductEntity product = productRepo.findByProductIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new ItemNotFoundException("Product not found"));

        // Validate product is available for purchase
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not available for purchase");
        }

        // Validate inventory
        if (!product.isInStock()) {
            throw new BadRequestException("Product is out of stock");
        }

        // Validate quantity against product constraints
        if (!product.canOrderQuantity(quantity)) {
            throw new BadRequestException(
                    String.format("Invalid quantity. Min: %d, Max: %d",
                            product.getMinOrderQuantity(),
                            product.getMaxOrderQuantity())
            );
        }

        // Validate stock availability
        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            product.getStockQuantity(), quantity)
            );
        }

        // Build checkout item from real product
        return buildCheckoutItemFromProduct(product, quantity);
    }

    private CheckoutSessionEntity.CheckoutItem buildCheckoutItemFromProduct(
            ProductEntity product,
            Integer quantity) {

        BigDecimal unitPrice = product.getPrice();
        BigDecimal discountPerItem = product.getDiscountAmount(); // If product is on sale
        BigDecimal discountAmount = discountPerItem.multiply(BigDecimal.valueOf(quantity));
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        //Todo: here we handle Tax
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.0)); // 18% VAT
        BigDecimal total = subtotal.add(tax).subtract(discountAmount);

        return CheckoutSessionEntity.CheckoutItem.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .productSlug(product.getProductSlug())
                .productImage(product.getProductImages() != null && !product.getProductImages().isEmpty()
                        ? product.getProductImages().get(0) : null)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .discountAmount(discountAmount)
                .subtotal(subtotal)
                .tax(tax)
                .total(total)
                .shopId(product.getShop().getShopId())
                .shopName(product.getShop().getShopName())
                .shopLogo(product.getShop().getLogoUrl())
                .availableForCheckout(product.isInStock())
                .availableQuantity(product.getStockQuantity())
                .build();
    }

    // ========================================
    // PRICING CALCULATION
    // ========================================

    public CheckoutSessionEntity.PricingSummary calculatePricing(
            List<CheckoutSessionEntity.CheckoutItem> items,
            CheckoutSessionEntity.ShippingMethod shippingMethod) {

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        for (CheckoutSessionEntity.CheckoutItem item : items) {
            subtotal = subtotal.add(item.getSubtotal());
            totalTax = totalTax.add(item.getTax());
            totalDiscount = totalDiscount.add(item.getDiscountAmount());
        }

        BigDecimal shippingCost = shippingMethod != null ? shippingMethod.getCost() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingCost).add(totalTax).subtract(totalDiscount);

        return CheckoutSessionEntity.PricingSummary.builder()
                .subtotal(subtotal.setScale(2, RoundingMode.HALF_UP))
                .discount(totalDiscount.setScale(2, RoundingMode.HALF_UP))
                .shippingCost(shippingCost.setScale(2, RoundingMode.HALF_UP))
                .tax(totalTax.setScale(2, RoundingMode.HALF_UP))
                .total(total.setScale(2, RoundingMode.HALF_UP))
                .currency("TZS")
                .build();
    }

    // ========================================
    // PAYMENT INTENT CREATION
    // ========================================

    public CheckoutSessionEntity.PaymentIntent createPaymentIntent(
            PaymentMethodsEntity paymentMethod,
            CheckoutSessionEntity.PricingSummary pricing,
            UUID accountId) throws BadRequestException, ItemNotFoundException {

        return switch (paymentMethod.getPaymentMethodType()) {
            case WALLET -> createWalletPaymentIntent(paymentMethod, pricing, accountId);
            case CASH_ON_DELIVERY -> createCODPaymentIntent();
            case CREDIT_CARD, DEBIT_CARD -> throw new BadRequestException("Card payment not implemented yet");
            case MNO_PAYMENT -> throw new BadRequestException("M-Pesa payment not implemented yet");
            case PAYPAL -> throw new BadRequestException("PayPal payment not implemented yet");
            case BANK_TRANSFER -> throw new BadRequestException("Bank transfer not implemented yet");
            case CRYPTOCURRENCY -> throw new BadRequestException("Cryptocurrency payment not implemented yet");
            case MOBILE_PAYMENT -> throw new BadRequestException("Mobile payment not implemented yet");
            case GIFT_CARD -> throw new BadRequestException("Gift card payment not implemented yet");
            default -> throw new BadRequestException("Unsupported payment method type");
        };
    }

    private CheckoutSessionEntity.PaymentIntent createWalletPaymentIntent(
            PaymentMethodsEntity paymentMethod,
            CheckoutSessionEntity.PricingSummary pricing,
            UUID accountId) throws BadRequestException, ItemNotFoundException {

        WalletEntity wallet = walletService.getWalletByAccountId(accountId);

        if (!wallet.getIsActive()) {
            throw new BadRequestException("Wallet is not active");
        }

        BigDecimal walletBalance = walletService.getMyWalletBalance();

        if (walletBalance.compareTo(pricing.getTotal()) < 0) {
            throw new BadRequestException(
                    String.format("Insufficient wallet balance. Required: %s TZS, Available: %s TZS",
                            pricing.getTotal(), walletBalance)
            );
        }

        return CheckoutSessionEntity.PaymentIntent.builder()
                .provider("WALLET")
                .clientSecret(null)
                .paymentMethods(List.of("WALLET"))
                .status("READY")
                .build();
    }

    private CheckoutSessionEntity.PaymentIntent createCODPaymentIntent() {
        return CheckoutSessionEntity.PaymentIntent.builder()
                .provider("CASH_ON_DELIVERY")
                .clientSecret(null)
                .paymentMethods(List.of("CASH"))
                .status("PENDING")
                .build();
    }

    // ========================================
    // INVENTORY HOLD (PLACEHOLDER)
    // ========================================

    public void holdInventory(UUID productId, Integer quantity, LocalDateTime expiresAt) {
        // Todo: Implement inventory hold logic
        // This should decrement available stock temporarily
    }

    public void releaseInventory(UUID productId, Integer quantity) {
        // Todo: Release held inventory
    }

    // ========================================
    // SESSION EXPIRATION
    // ========================================

    public LocalDateTime calculateSessionExpiration() {
        return LocalDateTime.now().plusMinutes(15);
    }

    public LocalDateTime calculateInventoryHoldExpiration() {
        return calculateSessionExpiration();
    }
}