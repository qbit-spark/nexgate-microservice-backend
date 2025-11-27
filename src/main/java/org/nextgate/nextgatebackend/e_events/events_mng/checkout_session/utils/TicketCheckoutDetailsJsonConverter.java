package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;

@Converter(autoApply = false)
@Slf4j
public class TicketCheckoutDetailsJsonConverter
        implements AttributeConverter<EventCheckoutSessionEntity.TicketCheckoutDetails, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails) {
        log.info("=== CONVERTER: convertToDatabaseColumn CALLED ===");
        log.info("Input ticketDetails: {}", ticketDetails);

        if (ticketDetails == null) {
            log.info("ticketDetails is null, returning null");
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(ticketDetails);
            log.info("Successfully converted to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("=== CONVERTER ERROR ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            throw new RuntimeException("Failed to convert TicketCheckoutDetails to JSON", e);
        }
    }

    @Override
    public EventCheckoutSessionEntity.TicketCheckoutDetails convertToEntityAttribute(String json) {
        log.info("=== CONVERTER: convertToEntityAttribute CALLED ===");
        log.info("Input JSON: {}", json);

        if (json == null || json.isBlank()) {
            log.info("JSON is null/blank, returning null");
            return null;
        }

        try {
            EventCheckoutSessionEntity.TicketCheckoutDetails ticketDetails =
                    objectMapper.readValue(json, EventCheckoutSessionEntity.TicketCheckoutDetails.class);
            log.info("Successfully converted from JSON to object");
            return ticketDetails;
        } catch (JsonProcessingException e) {
            log.error("=== CONVERTER ERROR ===");
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full stack trace:", e);
            throw new RuntimeException("Failed to convert JSON to TicketCheckoutDetails", e);
        }
    }
}