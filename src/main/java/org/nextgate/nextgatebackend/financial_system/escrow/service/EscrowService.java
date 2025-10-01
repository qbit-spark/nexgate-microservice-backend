package org.nextgate.nextgatebackend.financial_system.escrow.service;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EscrowService {

    // Creates escrow and moves money from buyer wallet to escrow ledger account
    EscrowAccountEntity holdMoney(
            CheckoutSessionEntity checkoutSession,
            AccountEntity buyer,
            AccountEntity seller,
            BigDecimal amount
    ) throws ItemNotFoundException, RandomExceptions;

    // Releases money from escrow to seller and platform (delivery confirmed)
    void releaseMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    // Refunds money from escrow back to buyer (cancelled/disputed)
    void refundMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    // Marks escrow as disputed (money stays held)
    void disputeEscrow(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    // Gets escrow by ID
    EscrowAccountEntity getEscrowById(UUID escrowId) throws ItemNotFoundException;

    // Gets escrow by escrow number
    EscrowAccountEntity getEscrowByNumber(String escrowNumber) throws ItemNotFoundException;

    // Gets escrow by checkout session
    EscrowAccountEntity getEscrowByCheckoutSession(CheckoutSessionEntity checkoutSession) throws ItemNotFoundException;

    // Gets all escrows for a buyer
    List<EscrowAccountEntity> getBuyerEscrows(AccountEntity buyer);

    // Gets all escrows for a seller
    List<EscrowAccountEntity> getSellerEscrows(AccountEntity seller);

    // Gets all escrows by status
    List<EscrowAccountEntity> getEscrowsByStatus(EscrowStatus status);

    // Checks if escrow exists for checkout session
    boolean escrowExistsForCheckoutSession(CheckoutSessionEntity checkoutSession);

    // Generates unique escrow number
    String generateEscrowNumber();
}