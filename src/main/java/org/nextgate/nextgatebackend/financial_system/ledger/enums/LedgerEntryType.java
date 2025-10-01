package org.nextgate.nextgatebackend.financial_system.ledger.enums;

public enum LedgerEntryType {
    // Wallet operations
    WALLET_TOPUP,           // User adds money to wallet
    WALLET_WITHDRAWAL,      // User withdraws money from wallet

    // Purchase operations
    PURCHASE,               // Money moves from buyer wallet to escrow
    ESCROW_RELEASE,         // Money released from escrow to seller + platform
    REFUND,                 // Money returned from escrow to buyer

    // Fee operations
    FEE_COLLECTION,         // Platform fee collected

    // Group purchase operations
    GROUP_PURCHASE,         // Payment for group deal
    GROUP_REFUND,           // Refund for failed group deal

    // Installment operations
    INSTALLMENT_PAYMENT,    // One installment payment
    INSTALLMENT_REFUND,     // Refund for cancelled installment

    // External payment operations
    EXTERNAL_PAYMENT,       // Payment from M-Pesa, Tigo, etc.
    EXTERNAL_WITHDRAWAL,    // Withdrawal to bank account

    // Dispute operations
    DISPUTE_REFUND,         // Refund after dispute resolution

    // System operations
    INITIAL_BALANCE,        // Initial balance setup (migration)
    ADJUSTMENT              // Manual balance adjustment (admin only)
}