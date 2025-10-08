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

    private UUID paymentMethodId;

    private Map<String, Object> metadata;


    // NEW: For GROUP_PURCHASE
    private UUID groupInstanceId;  // Optional: to join existing group

    // NEW: For INSTALLMENT
    private UUID installmentPlanId;              // Which plan customer selected
    private Integer downPaymentPercent;          // Customer's chosen down payment %

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