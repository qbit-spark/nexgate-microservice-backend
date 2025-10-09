package org.nextgate.nextgatebackend.order_mng_service.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    /**
     * Creates order from a completed checkout session
     *
     * Automatically handles all session types:
     * - REGULAR_DIRECTLY: Direct purchase
     * - REGULAR_CART: Cart checkout
     * - INSTALLMENT: Installment purchase
     * - GROUP_PURCHASE: Group buying
     *
     * @param checkoutSessionId The completed checkout session
     * @return List containing the created order ID
     * @throws ItemNotFoundException if session not found
     * @throws BadRequestException if order cannot be created
     */
    List<UUID> createOrdersFromCheckoutSession(UUID checkoutSessionId)
            throws ItemNotFoundException, BadRequestException;
}