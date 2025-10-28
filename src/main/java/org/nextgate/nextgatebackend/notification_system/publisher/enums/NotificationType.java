package org.nextgate.nextgatebackend.notification_system.publisher.enums;

public enum NotificationType {
    // Order notifications
    ORDER_CONFIRMATION,
    ORDER_SHIPPED,
    ORDER_DELIVERED,

    // Payment notifications
    PAYMENT_RECEIVED,
    PAYMENT_FAILURE,

    // Cart notifications
    CART_ABANDONMENT,

    // Checkout notifications
    CHECKOUT_EXPIRY,

    // Wallet notifications
    WALLET_BALANCE_UPDATE,

    // Installment notifications
    INSTALLMENT_DUE,

    // Shop notifications
    SHOP_NEW_ORDER,
    SHOP_LOW_INVENTORY,

    // Group purchase notifications
    GROUP_PURCHASE_COMPLETE,

    //Todo: we need template and consumer for this
    GROUP_PURCHASE_FAILED,

    // User notifications
    WELCOME_EMAIL,

    // Promotional notifications
    PROMOTIONAL_OFFER,

    GROUP_PURCHASE_CREATED,
    GROUP_MEMBER_JOINED,
    GROUP_SEATS_TRANSFERRED

}