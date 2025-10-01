package org.nextgate.nextgatebackend.financial_system.escrow.enums;

/**
 * Status of escrow account throughout its lifecycle
 */
public enum EscrowStatus {
    HELD,       // Money is held in escrow, waiting for delivery confirmation
    RELEASED,   // Money has been released to seller (delivery confirmed)
    REFUNDED,   // Money has been refunded to buyer (order cancelled/disputed)
    DISPUTED    // Under dispute resolution (money still held)
}