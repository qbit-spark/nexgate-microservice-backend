package org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarkOrderAsShippedRequest {

    @NotBlank(message = "Tracking number is required")
    @Size(max = 100, message = "Tracking number must not exceed 100 characters")
    private String trackingNumber;

    @NotBlank(message = "Carrier is required")
    @Size(max = 50, message = "Carrier name must not exceed 50 characters")
    private String carrier;
}