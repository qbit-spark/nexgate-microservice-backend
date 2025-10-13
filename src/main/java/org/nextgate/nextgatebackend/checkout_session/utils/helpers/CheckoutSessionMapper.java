// CheckoutSessionMapper.java
package org.nextgate.nextgatebackend.checkout_session.utils.helpers;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionResponse;
import org.nextgate.nextgatebackend.checkout_session.payload.CheckoutSessionSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CheckoutSessionMapper {

    // ========================================
    // ENTITY TO RESPONSE CONVERSIONS
    // ========================================

    public CheckoutSessionResponse toResponse(CheckoutSessionEntity entity) {
        if (entity == null) return null;

        return CheckoutSessionResponse.builder()
                .sessionId(entity.getSessionId())
                .sessionType(entity.getSessionType())
                .status(entity.getStatus())
                .customerId(entity.getCustomer().getAccountId())
                .customerUserName(entity.getCustomer().getUserName())
                .items(mapItemsToResponse(entity.getItems()))
                .pricing(mapPricingToResponse(entity.getPricing()))
                .shippingAddress(mapShippingAddressToResponse(entity.getShippingAddress()))
                .billingAddress(mapBillingAddressToResponse(entity.getBillingAddress()))
                .shippingMethod(mapShippingMethodToResponse(entity.getShippingMethod()))
                .paymentIntent(mapPaymentIntentToResponse(entity.getPaymentIntent()))
                .paymentAttempts(mapPaymentAttemptsToResponse(entity.getPaymentAttempts()))
                .inventoryHeld(entity.getInventoryHeld())
                .inventoryHoldExpiresAt(entity.getInventoryHoldExpiresAt())
                .metadata(entity.getMetadata())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .completedAt(entity.getCompletedAt())
                .createdOrderIds(entity.getCreatedOrderIds())
                .cartId(entity.getCartId())
                .selectedInstallmentPlanId(entity.getSelectedInstallmentPlanId())
                .installmentConfig(mapInstallmentConfigToResponse(entity.getInstallmentConfig()))
                .build();
    }

    public CheckoutSessionSummaryResponse toSummaryResponse(CheckoutSessionEntity entity) {
        if (entity == null) return null;

        return CheckoutSessionSummaryResponse.builder()
                .sessionId(entity.getSessionId())
                .sessionType(entity.getSessionType())
                .status(entity.getStatus())
                .itemCount(entity.getItems() != null ? entity.getItems().size() : 0)
                .totalAmount(entity.getPricing() != null ? entity.getPricing().getTotal() : null)
                .currency(entity.getPricing() != null ? entity.getPricing().getCurrency() : null)
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .isExpired(entity.isExpired())
                .canRetryPayment(entity.canRetryPayment())
                .itemPreviews(mapItemsToPreview(entity.getItems())) // ADD THIS LINE
                .build();
    }

    public List<CheckoutSessionSummaryResponse> toSummaryResponseList(List<CheckoutSessionEntity> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // PRIVATE MAPPING HELPERS
    // ========================================

    private List<CheckoutSessionResponse.CheckoutItemResponse> mapItemsToResponse(
            List<CheckoutSessionEntity.CheckoutItem> items) {
        if (items == null) return List.of();

        return items.stream()
                .map(item -> CheckoutSessionResponse.CheckoutItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSlug(item.getProductSlug())
                        .productImage(item.getProductImage())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        //.discountAmount(item.getDiscountAmount())
                        .subtotal(item.getSubtotal())
                        .tax(item.getTax())
                        .total(item.getTotal())
                        .shopId(item.getShopId())
                        .shopName(item.getShopName())
                        .shopLogo(item.getShopLogo())
                        .availableForCheckout(item.getAvailableForCheckout())
                        .availableQuantity(item.getAvailableQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private CheckoutSessionResponse.PricingSummaryResponse mapPricingToResponse(
            CheckoutSessionEntity.PricingSummary pricing) {
        if (pricing == null) return null;

        return CheckoutSessionResponse.PricingSummaryResponse.builder()
                .subtotal(pricing.getSubtotal())
                .discount(pricing.getDiscount())
                .shippingCost(pricing.getShippingCost())
                .tax(pricing.getTax())
                .total(pricing.getTotal())
                .currency(pricing.getCurrency())
                .build();
    }

    private CheckoutSessionResponse.ShippingAddressResponse mapShippingAddressToResponse(
            CheckoutSessionEntity.ShippingAddress address) {
        if (address == null) return null;

        return CheckoutSessionResponse.ShippingAddressResponse.builder()
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .phone(address.getPhone())
                .build();
    }

    private CheckoutSessionResponse.BillingAddressResponse mapBillingAddressToResponse(
            CheckoutSessionEntity.BillingAddress address) {
        if (address == null) return null;

        return CheckoutSessionResponse.BillingAddressResponse.builder()
                .sameAsShipping(address.getSameAsShipping())
                .fullName(address.getFullName())
                .addressLine1(address.getAddressLine1())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private CheckoutSessionResponse.ShippingMethodResponse mapShippingMethodToResponse(
            CheckoutSessionEntity.ShippingMethod method) {
        if (method == null) return null;

        return CheckoutSessionResponse.ShippingMethodResponse.builder()
                .id(method.getId())
                .name(method.getName())
                .carrier(method.getCarrier())
                .cost(method.getCost())
                .estimatedDays(method.getEstimatedDays())
                .estimatedDelivery(method.getEstimatedDelivery())
                .build();
    }

    private CheckoutSessionResponse.PaymentIntentResponse mapPaymentIntentToResponse(
            CheckoutSessionEntity.PaymentIntent intent) {
        if (intent == null) return null;

        return CheckoutSessionResponse.PaymentIntentResponse.builder()
                .provider(intent.getProvider())
                .clientSecret(intent.getClientSecret())
                .paymentMethods(intent.getPaymentMethods())
                .status(intent.getStatus())
                .build();
    }

    private List<CheckoutSessionResponse.PaymentAttemptResponse> mapPaymentAttemptsToResponse(
            List<CheckoutSessionEntity.PaymentAttempt> attempts) {
        if (attempts == null) return List.of();

        return attempts.stream()
                .map(attempt -> CheckoutSessionResponse.PaymentAttemptResponse.builder()
                        .attemptNumber(attempt.getAttemptNumber())
                        .paymentMethod(attempt.getPaymentMethod())
                        .status(attempt.getStatus())
                        .errorMessage(attempt.getErrorMessage())
                        .attemptedAt(attempt.getAttemptedAt())
                        .transactionId(attempt.getTransactionId())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CheckoutSessionSummaryResponse.ItemPreview> mapItemsToPreview(
            List<CheckoutSessionEntity.CheckoutItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        return items.stream()
                .map(item -> CheckoutSessionSummaryResponse.ItemPreview.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .total(item.getTotal())
                        .shopName(item.getShopName())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }


    // NEW: Helper method
    private CheckoutSessionResponse.InstallmentConfigResponse mapInstallmentConfigToResponse(
            CheckoutSessionEntity.InstallmentConfiguration config) {
        if (config == null) return null;

        return CheckoutSessionResponse.InstallmentConfigResponse.builder()
                .planId(config.getPlanId())
                .planName(config.getPlanName())
                .termMonths(config.getTermMonths())
                .apr(config.getApr())
                .downPaymentPercent(config.getDownPaymentPercent())
                .downPaymentAmount(config.getDownPaymentAmount())
                .financedAmount(config.getFinancedAmount())
                .monthlyPaymentAmount(config.getMonthlyPaymentAmount())
                .totalInterest(config.getTotalInterest())
                .totalAmount(config.getTotalAmount())
                .firstPaymentDate(config.getFirstPaymentDate())
                .gracePeriodDays(config.getGracePeriodDays())
                .fulfillmentTiming(config.getFulfillmentTiming())
                .schedule(mapScheduleToResponse(config.getSchedule()))
                .build();
    }

    // NEW: Helper method
    private List<CheckoutSessionResponse.PaymentScheduleItemResponse> mapScheduleToResponse(
            List<CheckoutSessionEntity.PaymentScheduleItem> schedule) {
        if (schedule == null) return null;

        return schedule.stream()
                .map(item -> CheckoutSessionResponse.PaymentScheduleItemResponse.builder()
                        .paymentNumber(item.getPaymentNumber())
                        .dueDate(item.getDueDate())
                        .amount(item.getAmount())
                        .principalPortion(item.getPrincipalPortion())
                        .interestPortion(item.getInterestPortion())
                        .remainingBalance(item.getRemainingBalance())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}