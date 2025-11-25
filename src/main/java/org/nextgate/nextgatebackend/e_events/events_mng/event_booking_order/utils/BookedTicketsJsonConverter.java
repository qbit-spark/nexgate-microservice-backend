package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts List<BookedTicket> to/from JSON for database storage
 */
@Converter
@Slf4j
public class BookedTicketsJsonConverter
        implements AttributeConverter<List<EventBookingOrderEntity.BookedTicket>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<EventBookingOrderEntity.BookedTicket> bookedTickets) {
        if (bookedTickets == null || bookedTickets.isEmpty()) {
            log.debug("Converting empty booked tickets list to JSON");
            return "[]";
        }

        try {
            String json = objectMapper.writeValueAsString(bookedTickets);
            log.debug("Successfully converted {} booked tickets to JSON", bookedTickets.size());
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting BookedTickets to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert BookedTickets to JSON", e);
        }
    }

    @Override
    public List<EventBookingOrderEntity.BookedTicket> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            log.debug("Converting null/empty JSON to empty booked tickets list");
            return new ArrayList<>();
        }

        try {
            List<EventBookingOrderEntity.BookedTicket> bookedTickets =
                    objectMapper.readValue(json, new TypeReference<List<EventBookingOrderEntity.BookedTicket>>() {});
            log.debug("Successfully converted JSON to {} booked tickets", bookedTickets.size());
            return bookedTickets;
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to BookedTickets: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert JSON to BookedTickets", e);
        }
    }
}