package org.nextgate.nextgatebackend.wallet_service.wallet.controller;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.wallet_service.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deactivate/{walletId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_STAFF_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> deactivateWallet(
            @PathVariable UUID walletId) throws ItemNotFoundException {

        walletService.deactivateWallet(walletId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet deactivated successfully",
                null
        ));
    }

    @PostMapping("/activate/{walletId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_STAFF_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateWallet(
            @PathVariable UUID walletId) throws ItemNotFoundException {
         walletService.activateWallet(walletId);
        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet activated successfully",
                null
        ));
    }
}