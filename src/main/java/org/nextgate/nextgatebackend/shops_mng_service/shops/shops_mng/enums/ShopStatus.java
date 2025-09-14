package org.nextgate.nextgatebackend.shops_mng_service.shops.shops_mng.enums;

public enum ShopStatus {
    PENDING,     // Awaiting admin approval
    ACTIVE,      // Active and operational
    SUSPENDED,   // Temporarily suspended by admin
    CLOSED,      // Permanently closed/inactive
    UNDER_REVIEW // Under admin review for policy violations
}
