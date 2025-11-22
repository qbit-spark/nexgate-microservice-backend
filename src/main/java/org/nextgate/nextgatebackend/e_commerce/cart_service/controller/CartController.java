package org.nextgate.nextgatebackend.e_commerce.cart_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.AddToCartRequest;
import org.nextgate.nextgatebackend.e_commerce.cart_service.payload.UpdateCartItemRequest;
import org.nextgate.nextgatebackend.e_commerce.cart_service.service.CartService;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

//    @PostMapping("/initialize")
//    public ResponseEntity<GlobeSuccessResponseBuilder> initializeCart()
//            throws ItemNotFoundException {
//
//        GlobeSuccessResponseBuilder response = cartService.initializeCart();
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/add")
    public ResponseEntity<GlobeSuccessResponseBuilder> addToCart(
            @Valid @RequestBody AddToCartRequest request)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = cartService.addToCart(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getCart()
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = cartService.getCart();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateCartItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest request)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = cartService.updateCartItem(itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeCartItem(
            @PathVariable UUID itemId)
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = cartService.removeCartItem(itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<GlobeSuccessResponseBuilder> clearCart()
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = cartService.clearCart();
        return ResponseEntity.ok(response);
    }
}