package org.nextgate.nextgatebackend.wallet_service.transactions.service;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.wallet_service.transactions.entity.TransactionEntity;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionStatus;
import org.nextgate.nextgatebackend.wallet_service.transactions.enums.TransactionType;
import org.nextgate.nextgatebackend.wallet_service.wallet.entity.WalletEntity;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionService {

    // Core transaction creation
    TransactionEntity createTransaction(WalletEntity wallet, TransactionType transactionType,
                                        BigDecimal amount, String description, UUID orderId);
    // Transaction management
    TransactionEntity updateTransactionStatus(UUID transactionId, TransactionStatus status)
            throws ItemNotFoundException;

    TransactionEntity getTransactionById(UUID transactionId) throws ItemNotFoundException;

    // Transaction history
    List<TransactionEntity> getUserTransactions(UUID accountId) throws ItemNotFoundException;

    Page<TransactionEntity> getUserTransactionsPaged(UUID accountId, int page, int size)
            throws ItemNotFoundException;

    List<TransactionEntity> getMyTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) throws ItemNotFoundException;

    Page<TransactionEntity> getMyTransactionsByDateRangePaged(LocalDateTime startDate, LocalDateTime endDate, int page, int size) throws ItemNotFoundException;

    TransactionEntity createPurchaseTransaction(AccountEntity buyer, AccountEntity seller, BigDecimal amount, String orderId, String description, boolean useWallet);
}