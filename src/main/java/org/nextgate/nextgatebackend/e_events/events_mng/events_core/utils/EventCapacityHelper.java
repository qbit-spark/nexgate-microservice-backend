package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload.CapacityMetrics;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class EventCapacityHelper {

    public static CapacityMetrics calculateCapacityMetrics(List<TicketEntity> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return CapacityMetrics.builder()
                    .hasUnlimitedTickets(false)
                    .limitedCapacity(0)
                    .limitedSold(0)
                    .limitedRemaining(0)
                    .unlimitedSold(0)
                    .totalSold(0)
                    .displayCapacity(0)
                    .sellOutPercentage(0.0)
                    .build();
        }

        boolean hasUnlimited = tickets.stream().anyMatch(TicketEntity::getIsUnlimited);

        List<TicketEntity> limitedTickets = tickets.stream()
                .filter(t -> !t.getIsUnlimited())
                .toList();

        List<TicketEntity> unlimitedTickets = tickets.stream()
                .filter(TicketEntity::getIsUnlimited)
                .toList();

        int limitedCapacity = limitedTickets.stream()
                .mapToInt(t -> t.getTotalQuantity() != null ? t.getTotalQuantity() : 0)
                .sum();

        int limitedSold = limitedTickets.stream()
                .mapToInt(TicketEntity::getQuantitySold)
                .sum();

        int unlimitedSold = unlimitedTickets.stream()
                .mapToInt(TicketEntity::getQuantitySold)
                .sum();

        int totalSold = limitedSold + unlimitedSold;

        int displayCapacity = hasUnlimited
                ? limitedCapacity + unlimitedSold
                : limitedCapacity;

        double sellOutPercentage = limitedCapacity > 0
                ? BigDecimal.valueOf(limitedSold)
                .divide(BigDecimal.valueOf(limitedCapacity), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        return CapacityMetrics.builder()
                .hasUnlimitedTickets(hasUnlimited)
                .limitedCapacity(limitedCapacity)
                .limitedSold(limitedSold)
                .limitedRemaining(limitedCapacity - limitedSold)
                .unlimitedSold(unlimitedSold)
                .totalSold(totalSold)
                .displayCapacity(displayCapacity)
                .sellOutPercentage(sellOutPercentage)
                .build();
    }

    public static Integer getTotalCapacity(List<TicketEntity> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return null;
        }

        boolean hasUnlimited = tickets.stream().anyMatch(TicketEntity::getIsUnlimited);
        if (hasUnlimited) {
            return null;
        }

        return tickets.stream()
                .mapToInt(t -> t.getTotalQuantity() != null ? t.getTotalQuantity() : 0)
                .sum();
    }

    public static Integer getTotalSold(List<TicketEntity> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return 0;
        }

        return tickets.stream()
                .mapToInt(TicketEntity::getQuantitySold)
                .sum();
    }
}