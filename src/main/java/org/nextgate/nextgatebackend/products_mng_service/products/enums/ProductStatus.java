package org.nextgate.nextgatebackend.products_mng_service.products.enums;

public enum ProductStatus {
    DRAFT,        // Product is being created/edited (not public)
    ACTIVE,       // Product is live and available for purchase
    INACTIVE,     // Product is temporarily unavailable
    OUT_OF_STOCK, // Product is out of stock
    ARCHIVED      // Product is no longer sold but keep for history
}
