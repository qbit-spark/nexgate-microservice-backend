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
public class ConfirmationCodeRegeneratedResponse {

    private UUID orderId;
    private String orderNumber;
    private Boolean codeSent;
    private String destination; // "email" or "sms" or "both"
    private LocalDateTime codeExpiresAt;
    private Integer maxAttempts;
    private String message;
}