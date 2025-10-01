// WalletOperationResponse.java
package org.nextgate.nextgatebackend.financial_system.wallet.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletOperationResponse {
    private String message;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private String currency;
}