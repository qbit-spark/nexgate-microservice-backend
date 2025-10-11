package org.nextgate.nextgatebackend.order_mng_service.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmDeliveryRequest {

    @NotBlank(message = "Confirmation code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Confirmation code must be exactly 6 digits")
    private String confirmationCode;
}