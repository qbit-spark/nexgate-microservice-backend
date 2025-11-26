package org.nextgate.nextgatebackend.financial_system.escrow.service;


import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.entity.EscrowAccountEntity;
import org.nextgate.nextgatebackend.financial_system.escrow.enums.EscrowStatus;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;


import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EscrowService {

    // NEW: Universal method accepting PayableCheckoutSession
    EscrowAccountEntity holdMoney(
            PayableCheckoutSession session,
            AccountEntity payer,
            AccountEntity payee,
            BigDecimal amount
    ) throws ItemNotFoundException, RandomExceptions;

    void releaseMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    void refundMoney(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    void disputeEscrow(UUID escrowId) throws ItemNotFoundException, RandomExceptions;

    EscrowAccountEntity getEscrowById(UUID escrowId) throws ItemNotFoundException;

    EscrowAccountEntity getEscrowByNumber(String escrowNumber) throws ItemNotFoundException;

    EscrowAccountEntity getEscrowBySessionId(UUID sessionId, CheckoutSessionsDomains sessionDomain)
            throws ItemNotFoundException;
    List<EscrowAccountEntity> getBuyerEscrows(AccountEntity buyer);

    List<EscrowAccountEntity> getSellerEscrows(AccountEntity seller);

    List<EscrowAccountEntity> getEscrowsByStatus(EscrowStatus status);

    boolean escrowExistsForSession(UUID sessionId, CheckoutSessionsDomains sessionDomain);
    String generateEscrowNumber();
}