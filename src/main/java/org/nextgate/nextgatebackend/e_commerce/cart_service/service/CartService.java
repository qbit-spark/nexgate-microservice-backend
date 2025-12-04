package org.nextgate.nextgatebackend.e_commerce.cart_service.service;

import org.nextgate.nextgatebackend.e_commerce.cart_service.entity.CartEntity;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.AddToCartRequest;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.UpdateCartItemRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;

import java.util.UUID;

public interface CartService {

    GlobeSuccessResponseBuilder addToCart(AddToCartRequest request)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder getCart()
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder updateCartItem(UUID itemId, UpdateCartItemRequest request)
            throws ItemNotFoundException, RandomExceptions;

    GlobeSuccessResponseBuilder removeCartItem(UUID itemId)
            throws ItemNotFoundException;

    GlobeSuccessResponseBuilder clearCart()
            throws ItemNotFoundException;

    CartEntity initializeCart()
            throws ItemNotFoundException;
}