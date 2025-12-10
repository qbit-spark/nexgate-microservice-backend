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
                    .totalCapacity(0)
                    .totalSold(0)
                    .totalRemaining(0)
                    .sellOutPercentage(0.0)
                    .build();
        }

        int totalCapacity = tickets.stream()
                .mapToInt(TicketEntity::getTotalQuantity)
                .sum();

        int totalSold = tickets.stream()
                .mapToInt(TicketEntity::getQuantitySold)
                .sum();

        double sellOutPercentage = totalCapacity > 0
                ? BigDecimal.valueOf(totalSold)
                .divide(BigDecimal.valueOf(totalCapacity), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue()
                : 0.0;

        return CapacityMetrics.builder()
                .totalCapacity(totalCapacity)
                .totalSold(totalSold)
                .totalRemaining(totalCapacity - totalSold)
                .sellOutPercentage(sellOutPercentage)
                .build();
    }

    public static Integer getTotalCapacity(List<TicketEntity> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return 0;
        }

        return tickets.stream()
                .mapToInt(TicketEntity::getTotalQuantity)
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