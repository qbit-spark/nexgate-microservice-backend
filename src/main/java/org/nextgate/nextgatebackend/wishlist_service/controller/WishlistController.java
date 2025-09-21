package org.nextgate.nextgatebackend.wishlist_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.wishlist_service.payload.AddToWishlistRequest;
import org.nextgate.nextgatebackend.wishlist_service.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    public ResponseEntity<GlobeSuccessResponseBuilder> addToWishlist(
            @Valid @RequestBody AddToWishlistRequest request)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = wishlistService.addToWishlist(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getWishlist()
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = wishlistService.getWishlist();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeFromWishlist(
            @PathVariable UUID productId)
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<GlobeSuccessResponseBuilder> clearWishlist()
            throws ItemNotFoundException {

        GlobeSuccessResponseBuilder response = wishlistService.clearWishlist();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/move-to-cart/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> moveToCart(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "1") Integer quantity)
            throws ItemNotFoundException, RandomExceptions {

        GlobeSuccessResponseBuilder response = wishlistService.moveToCart(productId, quantity);
        return ResponseEntity.ok(response);
    }
}