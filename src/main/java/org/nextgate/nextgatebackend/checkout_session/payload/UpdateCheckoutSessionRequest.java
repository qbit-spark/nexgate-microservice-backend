package org.nextgate.nextgatebackend.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateCheckoutSessionRequest {

    // All fields are optional - only provided fields will be updated
    // Null means "don't change this field"

    private UUID shippingAddressId;

    private String shippingMethodId;

    private UUID paymentMethodId;

    // Optional metadata update
    private Map<String, Object> metadata;
}