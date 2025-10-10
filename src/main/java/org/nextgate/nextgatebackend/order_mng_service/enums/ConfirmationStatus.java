package org.nextgate.nextgatebackend.order_mng_service.enums;

public enum ConfirmationStatus {
    PENDING,      // Generated, waiting for verification
    VERIFIED,     // Successfully verified
    EXPIRED,      // Expired without verification
    REVOKED,      // Manually revoked
    MAX_ATTEMPTS  // Too many failed attempts
}
