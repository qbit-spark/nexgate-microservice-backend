package org.nextgate.nextgatebackend.financial_system.wallet.events;

import lombok.Getter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class WalletTopUpEvent extends ApplicationEvent {

    private final AccountEntity account;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String transactionRef;

    public WalletTopUpEvent(
            Object source,
            AccountEntity account,
            BigDecimal amount,
            BigDecimal newBalance,
            String transactionRef) {

        super(source);
        this.account = account;
        this.amount = amount;
        this.newBalance = newBalance;
        this.transactionRef = transactionRef;
    }
}