package org.nextgate.nextgatebackend.financial_system.wallet.service;

import org.nextgate.nextgatebackend.financial_system.ledger.entity.LedgerAccountEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    // Gets or creates wallet for authenticated user
    WalletEntity getMyWallet() throws ItemNotFoundException;

    // Gets wallet by wallet ID (admin only)
    WalletEntity getWalletById(UUID walletId) throws ItemNotFoundException;

    // Gets wallet by account ID
    WalletEntity getWalletByAccountId(UUID accountId) throws ItemNotFoundException;

    // Gets current wallet balance from ledger system
    BigDecimal getMyWalletBalance() throws ItemNotFoundException;

    // Gets balance for specific wallet
    BigDecimal getWalletBalance(WalletEntity wallet) throws ItemNotFoundException;

    // Gets the ledger account linked to this wallet
    LedgerAccountEntity getLedgerAccount(WalletEntity wallet) throws ItemNotFoundException;

    // Activates a wallet (admin only)
    void activateWallet(UUID walletId) throws ItemNotFoundException;

    // Deactivates a wallet with reason (admin only)
    void deactivateWallet(UUID walletId, String reason) throws ItemNotFoundException;

    // Adds money to wallet from external source (M-Pesa, card, etc.)
    void topupWallet(BigDecimal amount, String description) throws ItemNotFoundException, RandomExceptions;

    // Withdraws money from wallet to external destination (bank account)
    WalletEntity withdrawFromWallet(BigDecimal amount, String description) throws ItemNotFoundException, RandomExceptions;

    // Checks if wallet has sufficient balance
    boolean hasSufficientBalance(WalletEntity wallet, BigDecimal amount) throws ItemNotFoundException;

    // Updates last activity timestamp
    void recordActivity(WalletEntity wallet);

    WalletEntity getWalletByAccountIdInternalUse(UUID accountId) throws ItemNotFoundException;
}