package org.nextgate.nextgatebackend.order_mng_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryConfirmedResponse {

    private UUID orderId;
    private String orderNumber;
    private LocalDateTime deliveredAt;
    private LocalDateTime confirmedAt;
    private Boolean escrowReleased;
    private BigDecimal sellerAmount;
    private String currency;
    private String message;
}