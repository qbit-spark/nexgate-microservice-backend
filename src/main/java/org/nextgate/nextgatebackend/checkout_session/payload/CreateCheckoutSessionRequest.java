package org.nextgate.nextgatebackend.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateCheckoutSessionRequest {

    @NotNull(message = "Session type is required")
    private CheckoutSessionType sessionType;

    @Valid
    private List<CheckoutItemDto> items;

    @NotNull(message = "Shipping address is required")
    private UUID shippingAddressId;

    @NotBlank(message = "Shipping method is required")
    private String shippingMethodId;

    //This is optional coz Wallet can be used without Payment Method
    private UUID paymentMethodId;

    // Optional metadata for coupons, referrals, etc.
    private Map<String, Object> metadata;

    // For INSTALLMENT type (future use)
    private UUID installmentPlanId;

    // Nested DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CheckoutItemDto {

        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}