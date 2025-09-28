package org.nextgate.nextgatebackend.payment_methods.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentMethodListResponse {

    private List<PaymentMethodSummaryResponse> paymentMethods;
    private Integer totalCount;
    private Integer activeCount;
    private PaymentMethodSummaryResponse defaultPaymentMethod;
}