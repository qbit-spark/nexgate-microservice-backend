package org.nextgate.nextgatebackend.order_mng_service.enums;

public enum DeliveryStatus {
    PENDING,                 // Not yet shipped
    SHIPPED,                 // Seller marked as shipped
    DELIVERED,               // Product handed to buyer (seller's claim)
    CONFIRMED                // Buyer confirmed receipt with code
}