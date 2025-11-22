// CheckoutSessionSummaryResponse.java
package org.nextgate.nextgatebackend.e_commerce.checkout_session.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    // Item preview - showing limited info about items
    private List<ItemPreview> itemPreviews;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ItemPreview {
        private UUID productId;
        private String productName;
        private String productImage;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
        private String shopName;
    }
}