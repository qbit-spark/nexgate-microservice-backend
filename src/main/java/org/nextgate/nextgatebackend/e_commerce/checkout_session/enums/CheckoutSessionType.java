package org.nextgate.nextgatebackend.e_commerce.checkout_session.enums;

public enum CheckoutSessionType {
    REGULAR_DIRECTLY,      // Direct product purchase
    REGULAR_CART,          // Purchase from cart
    GROUP_PURCHASE,        // Group buying (future)
    INSTALLMENT            // Installment payment (future)
}