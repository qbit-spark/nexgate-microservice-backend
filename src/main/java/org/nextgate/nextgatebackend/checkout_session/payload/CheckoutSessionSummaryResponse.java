// CheckoutSessionSummaryResponse.java
package org.nextgate.nextgatebackend.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.checkout_session.enums.CheckoutSessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutSessionSummaryResponse {

    private UUID sessionId;
    private CheckoutSessionType sessionType;
    private CheckoutSessionStatus status;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private Boolean isExpired;
    private Boolean canRetryPayment;
}