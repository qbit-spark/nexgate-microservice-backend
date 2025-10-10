package org.nextgate.nextgatebackend.order_mng_service.service;

import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.order_mng_service.entity.DeliveryConfirmationEntity;
import org.nextgate.nextgatebackend.order_mng_service.entity.OrderEntity;

import java.util.UUID;

public interface DeliveryConfirmationService {

    /**
     * Generate delivery confirmation code for an order.
     * Returns the PLAIN TEXT code (to send to customer).
     * Code is hashed before storing in database.
     */
    String generateConfirmationCode(OrderEntity order);

    /**
     * Verify confirmation code.
     *
     * @param orderId Order to verify
     * @param code Plain text code entered by customer
     * @param customer Customer attempting verification
     * @param ipAddress IP address of verification attempt
     * @param deviceInfo Device info (user agent)
     * @return true if verified successfully
     */
    boolean verifyConfirmationCode(
            UUID orderId,
            String code,
            AccountEntity customer,
            String ipAddress,
            String deviceInfo
    ) throws ItemNotFoundException, BadRequestException;

    /**
     * Get active confirmation for order.
     */
    DeliveryConfirmationEntity getActiveConfirmation(UUID orderId)
            throws ItemNotFoundException;

    /**
     * Revoke confirmation code (if customer lost it, etc.)
     */
    void revokeConfirmationCode(UUID confirmationId, UUID revokedBy, String reason)
            throws ItemNotFoundException;

    /**
     * Regenerate confirmation code (if lost/expired).
     */
    String regenerateConfirmationCode(UUID orderId, AccountEntity requester)
            throws ItemNotFoundException, BadRequestException;
}