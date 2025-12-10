package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.enums.CheckoutSessionStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.CreateEventCheckoutRequest;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.payload.EventCheckoutResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.repo.EventCheckoutSessionRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.service.EventCheckoutService;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.validators.EventCheckoutValidations;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
import org.nextgate.nextgatebackend.financial_system.payment_processing.payloads.PaymentResponse;
import org.nextgate.nextgatebackend.financial_system.payment_processing.service.PaymentOrchestrator;
import org.nextgate.nextgatebackend.globe_enums.CheckoutSessionsDomains;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCheckoutServiceImpl implements EventCheckoutService {

    private final EventCheckoutSessionRepo checkoutSessionRepo;
    private final EventsRepo eventsRepo;
    private final TicketRepo ticketRepo;
    private final AccountRepo accountRepo;
    private final EventCheckoutValidations validations;
    private final PaymentOrchestrator paymentOrchestrator;

    @Override
    @Transactional
    public EventCheckoutResponse createCheckoutSession(CreateEventCheckoutRequest request)
            throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();
        log.info("Creating event checkout session for user: {}", customer.getUserName());

        validations.validateCheckoutRequest(request, customer);

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(request.getEventId())
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));

        TicketEntity ticket = ticketRepo.findById(request.getTicketTypeId())
                .orElseThrow(() -> new ItemNotFoundException("Ticket not found"));

        EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails = buildTicketDetails(request, ticket);
        EventCheckoutSessionEntity.PricingSummary pricing = calculatePricing(ticketDetails);
        EventCheckoutSessionEntity.PaymentIntent paymentIntent = createPaymentIntent(pricing, customer);

        LocalDateTime sessionExpiration = LocalDateTime.now().plusMinutes(15);
        LocalDateTime ticketHoldExpiration = LocalDateTime.now().plusMinutes(15);

        holdTickets(ticket, ticketDetails.getTotalQuantity(), ticketHoldExpiration);

        EventCheckoutSessionEntity session = EventCheckoutSessionEntity.builder()
                .customer(customer)
                .eventId(event.getId())
                .status(CheckoutSessionStatus.PENDING_PAYMENT)
                .ticketDetails(ticketDetails)
                .pricing(pricing)
                .paymentIntent(paymentIntent)
                .paymentAttempts(new ArrayList<>())
                .ticketsHeld(true)
                .ticketHoldExpiresAt(ticketHoldExpiration)
                .expiresAt(sessionExpiration)
                .build();

        log.info("=== ABOUT TO SAVE SESSION ===");
        log.info("Session object: {}", session);
        log.info("TicketDetails in session: {}", session.getTicketDetails());

        try {
            EventCheckoutSessionEntity savedSession = checkoutSessionRepo.save(session);
            log.info("=== SESSION SAVED SUCCESSFULLY ===");
            log.info("Saved session ID: {}", savedSession.getSessionId());
            return mapToResponse(savedSession, event, ticket, customer);
        } catch (Exception e) {
            log.error("=== ERROR DURING SAVE ===");
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventCheckoutResponse getCheckoutSessionById(UUID sessionId)
            throws ItemNotFoundException {

        AccountEntity customer = getAuthenticatedAccount();

        EventCheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, customer)
                .orElseThrow(() -> new ItemNotFoundException("Checkout session not found"));

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(session.getEventId())
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));

        TicketEntity ticket = ticketRepo.findById(session.getTicketDetails().getTicketTypeId())
                .orElseThrow(() -> new ItemNotFoundException("Ticket not found"));

        return mapToResponse(session, event, ticket, customer);
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID sessionId)
            throws ItemNotFoundException, BadRequestException, RandomExceptions {

        AccountEntity customer = getAuthenticatedAccount();

        EventCheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, customer)
                .orElseThrow(() -> new ItemNotFoundException("Checkout session not found"));

        if (session.getStatus() != CheckoutSessionStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Session is not pending payment: " + session.getStatus());
        }

        if (session.isExpired()) {
            throw new BadRequestException("Checkout session has expired");
        }

        return paymentOrchestrator.processPayment(sessionId, CheckoutSessionsDomains.EVENT);
    }

    @Override
    @Transactional
    public void cancelCheckoutSession(UUID sessionId)
            throws ItemNotFoundException, BadRequestException {

        AccountEntity customer = getAuthenticatedAccount();

        EventCheckoutSessionEntity session = checkoutSessionRepo.findBySessionIdAndCustomer(sessionId, customer)
                .orElseThrow(() -> new ItemNotFoundException("Checkout session not found"));

        if (session.getStatus() == CheckoutSessionStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel completed session");
        }

        if (session.getStatus() == CheckoutSessionStatus.PAYMENT_COMPLETED) {
            throw new BadRequestException("Cannot cancel - payment completed");
        }

        if (session.getTicketsHeld()) {
            releaseTickets(session);
        }

        session.setStatus(CheckoutSessionStatus.CANCELLED);
        session.setTicketsHeld(false);
        checkoutSessionRepo.save(session);

        log.info("Checkout session cancelled: {}", sessionId);
    }

    private EventCheckoutSessionEntity.TicketCheckoutDetails buildTicketDetails(
            CreateEventCheckoutRequest request, TicketEntity ticket) {

        int totalQuantity = request.getTicketsForMe();
        List<EventCheckoutSessionEntity.OtherAttendee> otherAttendees = new ArrayList<>();

        if (request.getOtherAttendees() != null) {
            for (CreateEventCheckoutRequest.OtherAttendeeRequest attendee : request.getOtherAttendees()) {
                totalQuantity += attendee.getQuantity();
                otherAttendees.add(EventCheckoutSessionEntity.OtherAttendee.builder()
                        .name(attendee.getName())
                        .email(attendee.getEmail())
                        .phone(attendee.getPhone())
                        .quantity(attendee.getQuantity())
                        .build());
            }
        }

        BigDecimal subtotal = ticket.getPrice().multiply(BigDecimal.valueOf(totalQuantity));

        EventCheckoutSessionEntity.TicketCheckoutDetails details = EventCheckoutSessionEntity.TicketCheckoutDetails.builder()
                .ticketTypeId(ticket.getId())
                .ticketTypeName(ticket.getName())
                .unitPrice(ticket.getPrice())
                .ticketsForBuyer(request.getTicketsForMe())
                .otherAttendees(otherAttendees)
                .sendTicketsToAttendees(request.getSendTicketsToAttendees())
                .totalQuantity(totalQuantity)
                .subtotal(subtotal)
                .build();

        log.info("Built ticket details: {}", details);
        return details;
    }

    private EventCheckoutSessionEntity.PricingSummary calculatePricing(
            EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails) {

        return EventCheckoutSessionEntity.PricingSummary.builder()
                .subtotal(ticketDetails.getSubtotal())
                .total(ticketDetails.getSubtotal())
                .build();
    }

    private EventCheckoutSessionEntity.PaymentIntent createPaymentIntent(
            EventCheckoutSessionEntity.PricingSummary pricing, AccountEntity customer) {

        return EventCheckoutSessionEntity.PaymentIntent.builder()
                .provider("WALLET")
                .clientSecret(null)
                .paymentMethods(List.of("WALLET"))
                .status("PENDING")
                .build();
    }

    private void holdTickets(TicketEntity ticket, Integer quantity, LocalDateTime holdExpiration) {
        ticket.setQuantitySold(ticket.getQuantitySold() + quantity);
        ticketRepo.save(ticket);
        log.debug("Held {} tickets until {}", quantity, holdExpiration);
    }

    private void releaseTickets(EventCheckoutSessionEntity session) {
        TicketEntity ticket = ticketRepo.findById(session.getTicketDetails().getTicketTypeId())
                .orElse(null);

        if (ticket != null) {
            Integer quantity = session.getTicketDetails().getTotalQuantity();
            ticket.setQuantitySold(Math.max(0, ticket.getQuantitySold() - quantity));
            ticketRepo.save(ticket);
            log.debug("Released {} tickets", quantity);
        }
    }
    private EventCheckoutResponse mapToResponse(
            EventCheckoutSessionEntity session,
            EventEntity event,
            TicketEntity ticket,
            AccountEntity customer) {

        EventCheckoutResponse.TicketDetailsResponse ticketDetailsResponse = EventCheckoutResponse.TicketDetailsResponse.builder()
                .ticketTypeId(session.getTicketDetails().getTicketTypeId())
                .ticketTypeName(session.getTicketDetails().getTicketTypeName())
                .unitPrice(session.getTicketDetails().getUnitPrice())
                .ticketsForBuyer(session.getTicketDetails().getTicketsForBuyer())
                .otherAttendees(mapOtherAttendees(session.getTicketDetails().getOtherAttendees()))
                .sendTicketsToAttendees(session.getTicketDetails().getSendTicketsToAttendees())
                .totalQuantity(session.getTicketDetails().getTotalQuantity())
                .subtotal(session.getTicketDetails().getSubtotal())
                .build();

        EventCheckoutResponse.PricingSummaryResponse pricingResponse = EventCheckoutResponse.PricingSummaryResponse.builder()
                .subtotal(session.getPricing().getSubtotal())
                .total(session.getPricing().getTotal())
                .build();

        EventCheckoutResponse.PaymentIntentResponse paymentIntentResponse = null;
        if (session.getPaymentIntent() != null) {
            paymentIntentResponse = EventCheckoutResponse.PaymentIntentResponse.builder()
                    .provider(session.getPaymentIntent().getProvider())
                    .clientSecret(session.getPaymentIntent().getClientSecret())
                    .paymentMethods(session.getPaymentIntent().getPaymentMethods())
                    .status(session.getPaymentIntent().getStatus())
                    .build();
        }

        return EventCheckoutResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus())
                .customerId(customer.getAccountId())
                .customerUserName(customer.getUserName())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .ticketDetails(ticketDetailsResponse)
                .pricing(pricingResponse)
                .paymentIntent(paymentIntentResponse)
                .ticketsHeld(session.getTicketsHeld())
                .ticketHoldExpiresAt(session.getTicketHoldExpiresAt())
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .completedAt(session.getCompletedAt())
                .createdBookingOrderId(session.getCreatedBookingOrderId())
                .isExpired(session.isExpired())
                .canRetryPayment(session.canRetryPayment())
                .build();
    }

    private List<EventCheckoutResponse.OtherAttendeeResponse> mapOtherAttendees(
            List<EventCheckoutSessionEntity.OtherAttendee> otherAttendees) {

        if (otherAttendees == null) {
            return null;
        }

        return otherAttendees.stream()
                .map(attendee -> EventCheckoutResponse.OtherAttendeeResponse.builder()
                        .name(attendee.getName())
                        .email(attendee.getEmail())
                        .phone(attendee.getPhone())
                        .quantity(attendee.getQuantity())
                        .build())
                .toList();
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}