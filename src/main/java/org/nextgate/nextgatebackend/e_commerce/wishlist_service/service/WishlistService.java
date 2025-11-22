package org.nextgate.nextgatebackend.e_commerce.wishlist_service.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.e_commerce.wishlist_service.payload.AddToWishlistRequest;

import java.util.UUID;

public interface WishlistService {

    GlobeSuccessResponseBuilder addToWishlist(AddToWishlistRequest request)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getWishlist()
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder removeFromWishlist(UUID productId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder clearWishlist()
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder moveToCart(UUID itemId, Integer quantity)
            throws ItemNotFoundException, RandomExceptions;
}