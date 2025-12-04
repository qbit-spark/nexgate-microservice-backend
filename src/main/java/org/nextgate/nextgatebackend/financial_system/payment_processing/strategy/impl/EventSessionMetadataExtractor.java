package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.impl;


import org.nextgate.nextgatebackend.financial_system.payment_processing.contract.PayableCheckoutSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.financial_system.payment_processing.strategy.SessionMetadataExtractor;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventSessionMetadataExtractor implements SessionMetadataExtractor {

    private final EventsRepo eventRepo;

    @Override
    public AccountEntity extractPayee(PayableCheckoutSession session) throws RandomExceptions {

        if (!(session instanceof EventCheckoutSessionEntity eventSession)) {
            throw new RandomExceptions("Invalid session type for EventSessionMetadataExtractor");
        }

        var event = eventRepo.findById(eventSession.getEventId())
                .orElseThrow(() -> new RandomExceptions("Event not found: " + eventSession.getEventId()));

        AccountEntity payee = event.getOrganizer();

        log.debug("Payee extracted: {} (Event: {})", payee.getUserName(), event.getTitle());

        return payee;
    }

    @Override
    public CheckoutSessionsDomains getSupportedDomain() {
        return CheckoutSessionsDomains.EVENT;
    }
}