package org.nextgate.nextgatebackend.financial_system.wallet.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletResponse {
    private UUID walletId;
    private UUID accountId;
    private String accountUserName;
    private BigDecimal currentBalance;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;
}