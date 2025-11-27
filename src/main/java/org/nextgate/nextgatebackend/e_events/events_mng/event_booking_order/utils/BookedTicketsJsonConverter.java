package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class BookedTicketsJsonConverter implements AttributeConverter<List<EventBookingOrderEntity.BookedTicket>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(List<EventBookingOrderEntity.BookedTicket> bookedTickets) {
        if (bookedTickets == null || bookedTickets.isEmpty()) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(bookedTickets);
        } catch (JsonProcessingException e) {
            log.error("Error converting BookedTickets to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to convert BookedTickets to JSON", e);
        }
    }

    @Override
    public List<EventBookingOrderEntity.BookedTicket> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<EventBookingOrderEntity.BookedTicket>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to BookedTickets: {}", e.getMessage());
            throw new RuntimeException("Failed to convert JSON to BookedTickets", e);
        }
    }
}