package org.nextgate.nextgatebackend.order_mng_service.service;

import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;

import java.util.UUID;

// Placeholder interface - will be implemented properly later
public interface OrderService {

    // Creates order from checkout session after successful payment
    // Returns order ID (for now returns null - placeholder)
    UUID createOrderFromCheckoutSession(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow
    );
}