package org.nextgate.nextgatebackend.wallet_service.transactions.enums;

public enum TransactionStatus {
    COMPLETED,  // Transaction successful
    PENDING,    // Transaction in progress
    FAILED,     // Transaction failed
    CANCELED,   // Transaction canceled
    HELD        // Money held in escrow
}