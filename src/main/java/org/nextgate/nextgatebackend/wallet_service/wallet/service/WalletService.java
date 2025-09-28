package org.nextgate.nextgatebackend.wallet_service.wallet.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    // Core wallet operations
    WalletEntity getWalletByAccountId(UUID accountId) throws ItemNotFoundException;
    WalletEntity getMyWallet() throws ItemNotFoundException;

    WalletEntity getWalletById(UUID walletId) throws ItemNotFoundException;

    BigDecimal getMyWalletBalance() throws ItemNotFoundException;

    // Wallet management
    void activateWallet(UUID walletId) throws ItemNotFoundException;

    void deactivateWallet(UUID walletId) throws ItemNotFoundException;

    // Wallet operations
    WalletEntity topupWallet(BigDecimal amount, String description)
            throws ItemNotFoundException, RandomExceptions;

    WalletEntity withdrawFromWallet(BigDecimal amount, String description)
            throws ItemNotFoundException, RandomExceptions;
}