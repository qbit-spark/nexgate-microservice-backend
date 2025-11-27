package org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.utils.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;

import java.util.ArrayList;
import java.util.List;

@Converter(autoApply = false)
@Slf4j
public class EventPaymentAttemptsJsonConverter
        implements AttributeConverter<List<EventCheckoutSessionEntity.PaymentAttempt>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<EventCheckoutSessionEntity.PaymentAttempt>> TYPE_REF =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<EventCheckoutSessionEntity.PaymentAttempt> attempts) {
        if (attempts == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attempts);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert PaymentAttempts to JSON", e);
            throw new IllegalArgumentException("Cannot convert PaymentAttempts to JSON", e);
        }
    }

    @Override
    public List<EventCheckoutSessionEntity.PaymentAttempt> convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to PaymentAttempts", e);
            throw new IllegalArgumentException("Cannot convert JSON to PaymentAttempts", e);
        }
    }
}