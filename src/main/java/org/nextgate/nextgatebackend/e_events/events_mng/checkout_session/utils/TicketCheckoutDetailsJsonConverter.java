package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;

/**
 * Converts TicketCheckoutDetails to/from JSON for database storage
 */
@Converter
@Slf4j
public class TicketCheckoutDetailsJsonConverter
        implements AttributeConverter<EventCheckoutSessionEntity.TicketCheckoutDetails, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails) {
        if (ticketDetails == null) {
            log.warn("Attempting to convert null TicketCheckoutDetails to JSON");
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(ticketDetails);
            log.debug("Successfully converted TicketCheckoutDetails to JSON");
            return json;
        } catch (JsonProcessingException e) {
            log.error("Error converting TicketCheckoutDetails to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert TicketCheckoutDetails to JSON", e);
        }
    }

    @Override
    public EventCheckoutSessionEntity.TicketCheckoutDetails convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            log.debug("Converting null/empty JSON to null TicketCheckoutDetails");
            return null;
        }

        try {
            EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails =
                    objectMapper.readValue(json, EventCheckoutSessionEntity.TicketCheckoutDetails.class);
            log.debug("Successfully converted JSON to TicketCheckoutDetails");
            return ticketDetails;
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to TicketCheckoutDetails: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert JSON to TicketCheckoutDetails", e);
        }
    }
}