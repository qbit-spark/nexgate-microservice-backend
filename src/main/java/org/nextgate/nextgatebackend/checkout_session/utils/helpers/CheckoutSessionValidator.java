// CheckoutSessionValidator.java
package org.nextgate.nextgatebackend.checkout_session.utils.helpers;

import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;
import org.nextgate.nextgatebackend.checkout_session.payload.CreateCheckoutSessionRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;
import org.nextgate.nextgatebackend.payment_methods.repo.PaymentMethodRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckoutSessionValidator {

    private final PaymentMethodRepository paymentMethodRepository;
    // Todo: Add other repositories as needed (ProductRepo, ShippingAddressRepo, etc.)

    // ========================================
    // SESSION TYPE VALIDATION
    // ========================================

    public void validateSessionType(CreateCheckoutSessionRequest request) throws BadRequestException {
        CheckoutSessionType sessionType = request.getSessionType();

        switch (sessionType) {
            case REGULAR_DIRECTLY -> validateRegularDirectlyRequest(request);
            case REGULAR_CART -> throw new BadRequestException("REGULAR_CART checkout not implemented yet");
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
}