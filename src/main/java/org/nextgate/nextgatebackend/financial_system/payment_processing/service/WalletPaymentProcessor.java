package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResult;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;


public interface WalletPaymentProcessor {

    PaymentResult processPayment(PayableCheckoutSession session)
            throws ItemNotFoundException, RandomExceptions;
}