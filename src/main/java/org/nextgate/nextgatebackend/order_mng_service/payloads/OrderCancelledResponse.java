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
public class OrderCancelledResponse {

    private UUID orderId;
    private String orderNumber;
    private LocalDateTime cancelledAt;
    private String reason;
    private Boolean refundProcessed;
    private BigDecimal refundAmount;
    private String currency;
    private String message;
}