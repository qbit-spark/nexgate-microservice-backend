package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;

@Converter(autoApply = false)
@Slf4j
public class PricingSummaryJsonConverter
        implements AttributeConverter<EventCheckoutSessionEntity.PricingSummary, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EventCheckoutSessionEntity.PricingSummary pricing) {
        if (pricing == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(pricing);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert PricingSummary to JSON", e);
            throw new IllegalArgumentException("Cannot convert PricingSummary to JSON", e);
        }
    }

    @Override
    public EventCheckoutSessionEntity.PricingSummary convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, EventCheckoutSessionEntity.PricingSummary.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to PricingSummary", e);
            throw new IllegalArgumentException("Cannot convert JSON to PricingSummary", e);
        }
    }
}