package org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.TicketResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.payload.TicketSummaryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to convert TicketTypeEntity to Response DTOs
 */
@Component
public class TicketEntityToResponseMapper {

    /**
     * Convert TicketTypeEntity to full TicketResponse
     */
    public TicketResponse toResponse(TicketEntity ticket) {
        if (ticket == null) {
            return null;
        }

        TicketResponse.TicketResponseBuilder builder = TicketResponse.builder()
                .id(ticket.getId())
                .eventId(ticket.getEvent() != null ? ticket.getEvent().getId() : null)
                .name(ticket.getName())
                .description(ticket.getDescription())
                .price(ticket.getPrice())
                .currency(ticket.getCurrency())
                .totalTickets(ticket.getTotalQuantity())
                .ticketsSold(ticket.getQuantitySold())
                .ticketsRemaining(ticket.getQuantityRemaining())
                .ticketsAvailable(ticket.getQuantityAvailable())
                .isUnlimited(ticket.getIsUnlimited())
                .isSoldOut(ticket.isSoldOut())
                .salesStartDateTime(ticket.getSalesStartDateTime())
                .salesEndDateTime(ticket.getSalesEndDateTime())
                .isOnSale(ticket.isOnSale())
                .minQuantityPerOrder(ticket.getMinQuantityPerOrder())
                .maxQuantityPerOrder(ticket.getMaxQuantityPerOrder())
                .maxQuantityPerUser(ticket.getMaxQuantityPerUser())
                .validUntilType(ticket.getValidUntilType())
                .customValidUntil(ticket.getCustomValidUntil())
                .attendanceMode(ticket.getAttendanceMode())
                .inclusiveItems(ticket.getInclusiveItems())
                .isHidden(ticket.getIsHidden())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt());

        // Add creator username
        if (ticket.getCreatedBy() != null) {
            builder.createdBy(ticket.getCreatedBy().getUserName());
        }

        // Add updater username
        if (ticket.getUpdatedBy() != null) {
            builder.updatedBy(ticket.getUpdatedBy().getUserName());
        }

        return builder.build();
    }

    /**
     * Convert TicketTypeEntity to lightweight TicketSummaryResponse
     */
    public TicketSummaryResponse toSummaryResponse(TicketEntity ticket) {
        if (ticket == null) {
            return null;
        }

        return TicketSummaryResponse.builder()
                .id(ticket.getId())
                .name(ticket.getName())
                .price(ticket.getPrice())
                .currency(ticket.getCurrency())
                .totalTickets(ticket.getTotalQuantity())
                .ticketsSold(ticket.getQuantitySold())
                .ticketsAvailable(ticket.getQuantityAvailable())
                .isUnlimited(ticket.getIsUnlimited())
                .isSoldOut(ticket.isSoldOut())
                .attendanceMode(ticket.getAttendanceMode())
                .status(ticket.getStatus())
                .isOnSale(ticket.isOnSale())
                .build();
    }

    /**
     * Convert list of TicketTypeEntity to list of TicketResponse
     */
    public List<TicketResponse> toResponseList(List<TicketEntity> tickets) {
        if (tickets == null) {
            return List.of();
        }

        return tickets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of TicketTypeEntity to list of TicketSummaryResponse
     */
    public List<TicketSummaryResponse> toSummaryList(List<TicketEntity> tickets) {
        if (tickets == null) {
            return List.of();
        }

        return tickets.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }
}