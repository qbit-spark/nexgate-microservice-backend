package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;

public interface SessionMetadataExtractor {

    AccountEntity extractPayee(PayableCheckoutSession session) throws RandomExceptions;

    CheckoutSessionsDomains getSupportedDomain();

    default boolean canHandle(PayableCheckoutSession session) {
        return getSupportedDomain().equals(session.getSessionDomain());
    }
}