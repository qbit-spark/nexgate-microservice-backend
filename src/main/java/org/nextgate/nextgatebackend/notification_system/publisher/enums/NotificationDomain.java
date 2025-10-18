package org.nextgate.nextgatebackend.notification_system.publisher.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * Maps notification types to their corresponding domains and routing keys
 * This enum ensures type-safety and prevents routing errors
 */
@Getter
public enum NotificationDomain {

    // Order domain
    ORDER("notification.order",
            NotificationType.ORDER_CONFIRMATION,
            NotificationType.ORDER_SHIPPED,
            NotificationType.ORDER_DELIVERED),

    // Payment domain
    PAYMENT("notification.payment",
            NotificationType.PAYMENT_RECEIVED,
            NotificationType.PAYMENT_FAILURE),

    // Cart domain
    CART("notification.cart",
            NotificationType.CART_ABANDONMENT),

    // Checkout domain
    CHECKOUT("notification.checkout",
            NotificationType.CHECKOUT_EXPIRY),

    // Wallet domain
    WALLET("notification.wallet",
            NotificationType.WALLET_BALANCE_UPDATE),

    // Installment domain
    INSTALLMENT("notification.installment",
            NotificationType.INSTALLMENT_DUE),

    // Shop domain
    SHOP("notification.shop",
            NotificationType.SHOP_NEW_ORDER,
            NotificationType.SHOP_LOW_INVENTORY),

    // Group Purchase domain
    GROUP_PURCHASE("notification.group_purchase",
            NotificationType.GROUP_PURCHASE_COMPLETE,
            NotificationType.GROUP_PURCHASE_CREATED,
            NotificationType.GROUP_MEMBER_JOINED,
            NotificationType.GROUP_SEATS_TRANSFERRED),

    // User domain
    USER("notification.user",
            NotificationType.WELCOME_EMAIL),


    // Promotional domain
    PROMOTIONAL("notification.promotional",
            NotificationType.PROMOTIONAL_OFFER);

    private final String routingKey;
    private final NotificationType[] notificationTypes;

    // Constructor for varargs
    NotificationDomain(String routingKey, NotificationType... notificationTypes) {
        this.routingKey = routingKey;
        this.notificationTypes = notificationTypes;
    }

    /**
     * Get the domain for a given notification type
     * Throws exception if notification type is not mapped
     */
    public static NotificationDomain fromNotificationType(NotificationType type) {
        return Arrays.stream(values())
                .filter(domain -> Arrays.asList(domain.getNotificationTypes()).contains(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No domain mapping found for notification type: " + type));
    }

    /**
     * Check if this domain handles the given notification type
     */
    public boolean handles(NotificationType type) {
        return Arrays.asList(notificationTypes).contains(type);
    }
}