package org.nextgate.nextgatebackend.financial_system.payment_processing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.financial_system.payment_processing.model.PaymentIntent;

@Converter
@Slf4j
public class PaymentIntentJsonConverter implements AttributeConverter<PaymentIntent, String> {

    private final ObjectMapper objectMapper;

    public PaymentIntentJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(PaymentIntent paymentIntent) {
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
    public PaymentIntent convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PaymentIntent.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to payment intent: {}", dbData, e);
            return null;
        }
    }
}