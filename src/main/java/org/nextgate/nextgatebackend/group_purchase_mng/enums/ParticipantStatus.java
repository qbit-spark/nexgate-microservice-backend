package org.nextgate.nextgatebackend.group_purchase_mng.enums;

public enum ParticipantStatus {
    ACTIVE,             // Currently in group, waiting for completion
    TRANSFERRED_OUT,    // Left this group (transferred to another)
    REFUNDED            // Group failed, money refunded
}