package org.nextgate.nextgatebackend.e_commerce.checkout_session.enums;

public enum CheckoutSessionType {
    REGULAR_DIRECTLY,      // Direct product purchase
    REGULAR_CART,          // Purchase from a cart
    GROUP_PURCHASE,        // Group buying
    INSTALLMENT            // Installment payment
}