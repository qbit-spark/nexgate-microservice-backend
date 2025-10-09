package org.nextgate.nextgatebackend.order_mng_service.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.order_mng_service.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Override
    public UUID createOrderFromCheckoutSession(
            CheckoutSessionEntity checkoutSession,
            EscrowAccountEntity escrow) {

        // TODO: Implement order creation logic in future phases
        // For now, just log and return null

        log.info("ORDER PLACEHOLDER: Would create order for checkout session: {}, escrow: {}",
                checkoutSession.getSessionId(), escrow.getEscrowNumber());

        log.info("ORDER PLACEHOLDER: Buyer: {}, Seller: {}, Amount: {} TZS",
                escrow.getBuyer().getUserName(),
                escrow.getSeller().getUserName(),
                escrow.getTotalAmount());

        // Return null for now - will return actual order ID later
        return null;
    }
}