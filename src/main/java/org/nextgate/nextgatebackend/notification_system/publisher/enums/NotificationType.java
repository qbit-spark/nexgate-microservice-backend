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
    INSTALLMENT_PAYMENT_SUCCESS,
    INSTALLMENT_PAYMENT_FAILED,
    INSTALLMENT_PAYMENT_REMINDER,
    INSTALLMENT_PAYMENT_OVERDUE,
    INSTALLMENT_AGREEMENT_CREATED,
    INSTALLMENT_AGREEMENT_COMPLETED,
    INSTALLMENT_AGREEMENT_DEFAULTED,
    INSTALLMENT_EARLY_PAYOFF_SUCCESS,
    INSTALLMENT_FLEXIBLE_PAYMENT_SUCCESS,
    INSTALLMENT_DEFAULT_ADMIN_ALERT,
    INSTALLMENT_COLLECTIONS_REQUIRED,

    // Shop notifications
    SHOP_NEW_ORDER,
    SHOP_LOW_INVENTORY,

    // Group purchase notifications
    GROUP_PURCHASE_COMPLETE,
    GROUP_PURCHASE_FAILED,
    GROUP_PURCHASE_CREATED,
    GROUP_MEMBER_JOINED,
    GROUP_SEATS_TRANSFERRED,

    // User notifications
    WELCOME_EMAIL,

    // Promotional notifications
    PROMOTIONAL_OFFER,

    // ✨ EVENT BOOKING NOTIFICATIONS (New!)
    EVENT_BOOKING_CONFIRMED,           // ← For ticket buyer (always sent)
    EVENT_TICKET_RECEIVED,             // ← For other attendees (conditional)
    EVENT_ORGANIZER_NEW_BOOKING,       // ← For event organizer
    EVENT_BOOKING_CANCELLED,           // ← When booking is cancelled
    EVENT_BOOKING_REMINDER            // ← Reminder before event (optional, future)
}