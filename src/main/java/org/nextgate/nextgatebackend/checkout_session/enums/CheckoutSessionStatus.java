package org.nextgate.nextgatebackend.checkout_session.enums;

public enum CheckoutSessionStatus {
    PENDING_PAYMENT,       // Session created, awaiting payment
    PAYMENT_PROCESSING,    // Payment in progress
    PAYMENT_FAILED,        // Payment failed, can retry
    PAYMENT_COMPLETED,     // Payment successful
    EXPIRED,               // Session expired
    CANCELLED,             // User cancelled
    COMPLETED              // Order created successfully
}