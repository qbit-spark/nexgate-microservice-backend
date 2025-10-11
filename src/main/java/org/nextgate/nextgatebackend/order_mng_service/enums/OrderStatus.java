package org.nextgate.nextgatebackend.order_mng_service.enums;

public enum OrderStatus {
    PENDING_PAYMENT,    // Payment not yet completed
    PENDING_SHIPMENT,   // Paid, waiting for seller to ship
    SHIPPED,            // Seller marked as shipped
    DELIVERED,          // Seller marked as delivered
    COMPLETED,          // Customer confirmed delivery, escrow released
    CANCELLED,          // Order cancelled
    REFUNDED           // Order refunded
}