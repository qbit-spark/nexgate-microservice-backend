package org.nextgate.nextgatebackend.wallet_service.wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.wallet_service.wallet.payload.*;
import org.nextgate.nextgatebackend.wallet_service.wallet.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/my-wallet")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyWallet() throws ItemNotFoundException {
        WalletEntity wallet = walletService.getMyWallet();
        BigDecimal balance = walletService.getMyWalletBalance();

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getId())
                .accountId(wallet.getAccount().getAccountId())
                .accountUserName(wallet.getAccount().getUserName())
                .currentBalance(balance)
                .isActive(wallet.getIsActive())
                .createdAt(wallet.getCreatedAt().toString())
                .updatedAt(wallet.getUpdatedAt().toString())
                .build();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet retrieved successfully",
                response
        ));
    }

    @GetMapping("/balance")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyBalance() throws ItemNotFoundException {
        BigDecimal balance = walletService.getMyWalletBalance();

        BalanceResponse response = BalanceResponse.builder()
                .balance(balance)
                .currency("TZS")
                .build();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Balance retrieved successfully",
                response
        ));
    }

    @GetMapping("/{walletId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_STAFF_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> getWalletById(@PathVariable UUID walletId)
            throws ItemNotFoundException {

        WalletEntity wallet = walletService.getWalletById(walletId);

        WalletResponse response = WalletResponse.builder()
                .walletId(wallet.getId())
                .accountId(wallet.getAccount().getAccountId())
                .accountUserName(wallet.getAccount().getUserName())
                .isActive(wallet.getIsActive())
                .createdAt(wallet.getCreatedAt().toString())
                .updatedAt(wallet.getUpdatedAt().toString())
                .build();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet retrieved successfully",
                response
        ));
    }

    @PostMapping("/activate/{walletId}")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> activateWallet(@PathVariable UUID walletId)
            throws ItemNotFoundException {

        walletService.activateWallet(walletId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet activated successfully",
                null
        ));
    }

    @PostMapping("/deactivate/{walletId}")
    @PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_STAFF_ADMIN')")
    public ResponseEntity<GlobeSuccessResponseBuilder> deactivateWallet(@PathVariable UUID walletId)
            throws ItemNotFoundException {

        walletService.deactivateWallet(walletId);

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Wallet deactivated successfully",
                null
        ));
    }

    @PostMapping("/topup")
    public ResponseEntity<GlobeSuccessResponseBuilder> topupWallet(@Valid @RequestBody TopupRequest request)
            throws ItemNotFoundException, RandomExceptions {

        walletService.topupWallet(request.getAmount(), request.getDescription());
        BigDecimal newBalance = walletService.getMyWalletBalance();

        WalletOperationResponse response = WalletOperationResponse.builder()
                .message("Wallet topped up successfully")
                .amount(request.getAmount())
                .newBalance(newBalance)
                .currency("TZS")
                .build();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Topup successful",
                response
        ));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<GlobeSuccessResponseBuilder> withdrawFromWallet(@Valid @RequestBody WithdrawRequest request)
            throws ItemNotFoundException, RandomExceptions {

        walletService.withdrawFromWallet(request.getAmount(), request.getDescription());
        BigDecimal newBalance = walletService.getMyWalletBalance();

        WalletOperationResponse response = WalletOperationResponse.builder()
                .message("Withdrawal successful")
                .amount(request.getAmount())
                .newBalance(newBalance)
                .currency("TZS")
                .build();

        return ResponseEntity.ok(GlobeSuccessResponseBuilder.success(
                "Withdrawal successful",
                response
        ));
    }
}