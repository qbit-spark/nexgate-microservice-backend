package org.nextgate.nextgatebackend.financial_system.payment_processing.enums;

public enum PaymentMethod {
    WALLET,              // Internal wallet payment
    MPESA,               // M-Pesa (Vodacom Tanzania)
    TIGO_PESA,           // Tigo Pesa
    AIRTEL_MONEY,        // Airtel Money
    HALOPESA,            // Halopesa
    CREDIT_CARD,         // Credit card
    DEBIT_CARD,          // Debit card
    BANK_TRANSFER,       // Direct bank transfer
    CASH_ON_DELIVERY     // COD (no upfront payment)
}