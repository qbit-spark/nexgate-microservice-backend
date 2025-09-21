package org.nextgate.nextgatebackend.wishlist_service.payload;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AddToWishlistRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;
}