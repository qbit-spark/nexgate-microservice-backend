package org.nextgate.nextgatebackend.wallet_service.escrow.service;

import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.nextgate.nextgatebackend.wallet_service.escrow.entity.EscrowEntity;
import org.nextgate.nextgatebackend.wallet_service.transactions.entity.TransactionEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EscrowService {

    // Core escrow operations
    EscrowEntity holdMoney(String orderId, UUID buyerId, UUID sellerId,
                           BigDecimal amount, TransactionEntity sourceTransaction);

    TransactionEntity releaseMoney(String orderId) throws ItemNotFoundException, RandomExceptions;

    TransactionEntity refundMoney(String orderId) throws ItemNotFoundException, RandomExceptions;

    // Escrow management
    EscrowEntity getEscrowByOrderId(String orderId) throws ItemNotFoundException;

    List<EscrowEntity> getEscrowsByBuyerId(UUID buyerId);

    List<EscrowEntity> getEscrowsBySellerId(UUID sellerId);

    List<EscrowEntity> getPendingEscrows();

    // Status checks
    boolean hasActiveEscrow(String orderId);

    BigDecimal getTotalHeldAmount(UUID userId);
}