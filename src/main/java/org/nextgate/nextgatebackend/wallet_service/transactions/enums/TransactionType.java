package org.nextgate.nextgatebackend.wallet_service.transactions.enums;

public enum TransactionType {
    // Wallet Operations
    WALLET_TOPUP,           // "Added 10,000 TZS to wallet"
    WALLET_WITHDRAWAL,      // "Withdrew 5,000 TZS to bank"

    // Purchase Operations
    PURCHASE_PAYMENT,       // "Paid 2,500 TZS for iPhone case"
    PURCHASE_REFUND,        // "Refunded 2,500 TZS - order canceled"

    // Sale Operations
    SALE_EARNINGS,          // "Earned 2,500 TZS from John for iPhone case"
    SALE_REFUND             // "Refunded 2,500 TZS to customer"
}