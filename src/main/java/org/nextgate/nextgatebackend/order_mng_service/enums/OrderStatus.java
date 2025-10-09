package org.nextgate.nextgatebackend.order_mng_service.enums;

public enum OrderStatus {
    PENDING_SHIPMENT,        // Order created, waiting for seller to ship
    SHIPPED,                 // Seller marked as shipped, buyer has confirmation code
    AWAITING_CONFIRMATION,   // Same as SHIPPED, waiting for buyer to confirm
    CONFIRMED,               // Buyer confirmed delivery with code
    COMPLETED,               // Money released to seller, order complete
    CANCELLED                // Order cancelled before shipment
}