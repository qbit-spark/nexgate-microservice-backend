package org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums;

public enum AgreementStatus {
    PENDING_FIRST_PAYMENT,  // Down payment made, waiting for grace period
    ACTIVE,                  // Currently paying installments
    COMPLETED,               // Fully paid off
    DEFAULTED,               // Missed payments, in collections
    CANCELLED                // User cancelled (if allowed by policy)
}