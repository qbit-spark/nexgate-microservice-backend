package org.nextgate.nextgatebackend.e_commerce.order_mng_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderShippedResponse {

    private UUID orderId;
    private String orderNumber;
    private String trackingNumber;
    //private String carrier;
    private LocalDateTime shippedAt;
    private String message;

    // Confirmation code info (DO NOT include actual code!)
    private Boolean confirmationCodeSent;
    private LocalDateTime codeExpiresAt;
    private Integer maxVerificationAttempts;
}