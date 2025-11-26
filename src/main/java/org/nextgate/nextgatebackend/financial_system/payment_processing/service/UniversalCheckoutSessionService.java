package org.nextgate.nextgatebackend.financial_system.payment_processing.service;

import com.qbitspark.jikoexpress.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.repo.ProductCheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalCheckoutSessionService {

    private final ProductCheckoutSessionRepo productCheckoutSessionRepo;
    private final EventCheckoutSessionRepo eventCheckoutSessionRepo;

    public PayableCheckoutSession findCheckoutSession(
            UUID sessionId,
            CheckoutSessionsDomains sessionDomain) // ← Enum
            throws ItemNotFoundException {

        return switch (sessionDomain) {
            case PRODUCT -> productCheckoutSessionRepo.findById(sessionId)
                    .orElseThrow(() -> new ItemNotFoundException("Product checkout session not found"));

            case EVENT -> eventCheckoutSessionRepo.findById(sessionId)
                    .orElseThrow(() -> new ItemNotFoundException("Event checkout session not found"));
            default -> throw new ItemNotFoundException("Unknown session domain: ");
        };
    }

    public void saveCheckoutSession(PayableCheckoutSession session) throws RandomExceptions {
        if (session instanceof ProductCheckoutSessionEntity productSession) {
            productCheckoutSessionRepo.save(productSession);
        } else if (session instanceof EventCheckoutSessionEntity eventSession) {
            eventCheckoutSessionRepo.save(eventSession);
        } else {
            throw new RandomExceptions("Unknown session type: " + session.getClass().getName());
        }
    }
}