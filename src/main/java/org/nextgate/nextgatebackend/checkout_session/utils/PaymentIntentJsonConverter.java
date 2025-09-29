package org.nextgate.nextgatebackend.checkout_session.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;

@Converter
@Slf4j
public class PaymentIntentJsonConverter implements AttributeConverter<CheckoutSessionEntity.PaymentIntent, String> {

    private final ObjectMapper objectMapper;

    public PaymentIntentJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(CheckoutSessionEntity.PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(paymentIntent);
        } catch (JsonProcessingException e) {
            log.error("Error converting payment intent to JSON", e);
            throw new RuntimeException("Error converting payment intent to JSON", e);
        }
    }

    @Override
    public CheckoutSessionEntity.PaymentIntent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, CheckoutSessionEntity.PaymentIntent.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to payment intent", e);
            return null;
        }
    }
}
