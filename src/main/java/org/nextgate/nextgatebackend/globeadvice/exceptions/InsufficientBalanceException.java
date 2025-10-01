package org.nextgate.nextgatebackend.globeadvice.exceptions;

import java.math.BigDecimal;

public class InsufficientBalanceException extends LedgerException {

    private final BigDecimal required;
    private final BigDecimal available;

    public InsufficientBalanceException(String accountNumber, BigDecimal required, BigDecimal available) {
        super(String.format("Insufficient balance in account %s. Required: %s TZS, Available: %s TZS",
                accountNumber, required, available));
        this.required = required;
        this.available = available;
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}