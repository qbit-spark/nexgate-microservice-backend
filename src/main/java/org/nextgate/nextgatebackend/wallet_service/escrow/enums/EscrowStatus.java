package org.nextgate.nextgatebackend.wallet_service.escrow.enums;

public enum EscrowStatus {
    HELD,       // Money held in escrow waiting for delivery
    RELEASED,   // Money released to seller (order delivered)
    REFUNDED    // Money refunded to buyer (order canceled)
}