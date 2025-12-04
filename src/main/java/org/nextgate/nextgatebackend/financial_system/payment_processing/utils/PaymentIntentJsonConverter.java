package org.nextgate.nextgatebackend.financial_system.payment_processing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.checkout_session.entity.EventCheckoutSessionEntity;

@Converter(autoApply = false)
@Slf4j
public class PaymentIntentJsonConverter
        implements AttributeConverter<EventCheckoutSessionEntity.PaymentIntent, String> {  // ← Use the nested class!

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(EventCheckoutSessionEntity.PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(paymentIntent);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert PaymentIntent to JSON", e);
            throw new IllegalArgumentException("Cannot convert PaymentIntent to JSON", e);
        }
    }

    @Override
    public EventCheckoutSessionEntity.PaymentIntent convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, EventCheckoutSessionEntity.PaymentIntent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to PaymentIntent", e);
            throw new IllegalArgumentException("Cannot convert JSON to PaymentIntent", e);
        }
    }
}