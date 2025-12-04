package org.nextgate.nextgatebackend.e_commerce.order_mng_service.enums;

public enum ProductOrderSource {
    DIRECT_PURCHASE,         // Normal buy now (full payment upfront)
    INSTALLMENT,             // Pay over time with installment plan
    GROUP_PURCHASE ,          // Group buying (future feature)
    CART_PURCHASE            // Purchase made through shopping cart
}