package org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums;

public enum PaymentStatus {
    SCHEDULED,    // Not due yet
    PENDING,      // Due today, awaiting payment
    PROCESSING,   // Payment in progress
    COMPLETED,    // Successfully paid
    FAILED,       // Payment attempt failed
    LATE,         // Past due date
    SKIPPED,      // Missed completely
    WAIVED ,    // Forgiven (special cases)

    PARTIALLY_PAID
}