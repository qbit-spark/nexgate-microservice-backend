package org.nextgate.nextgatebackend.financial_system.payment_processing.enums;

public enum PaymentStatus {
    SUCCESS,     // Payment completed successfully
    FAILED,      // Payment failed (insufficient balance, error, etc.)
    PENDING,     // Payment initiated but waiting confirmation (for external payments)
    CANCELLED    // Payment cancelled by user or system
}