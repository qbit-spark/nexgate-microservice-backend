package org.nextgate.nextgatebackend.group_purchase_mng.enums;

public enum GroupStatus {
    OPEN,           // Group is active and accepting participants
    COMPLETED,      // All seats filled, orders being created/created
    FAILED,         // Group expired without filling, refunds issued
    DELETED         // Soft deleted (all participants transferred out or admin deleted)
}