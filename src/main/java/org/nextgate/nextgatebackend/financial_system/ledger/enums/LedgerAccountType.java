package org.nextgate.nextgatebackend.financial_system.ledger.enums;

public enum LedgerAccountType {
    USER_WALLET,           // Individual user wallet
    ESCROW,                // Temporary holding for transactions
    PLATFORM_REVENUE,      // Platform's fee collection account
    PLATFORM_RESERVE,      // Emergency/reserve fund
    EXTERNAL_MONEY_IN,     // Virtual account for external payments (M-Pesa, etc.)
    EXTERNAL_MONEY_OUT     // Virtual account for withdrawals to banks
}