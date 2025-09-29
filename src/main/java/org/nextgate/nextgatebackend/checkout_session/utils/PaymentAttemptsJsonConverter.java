package org.nextgate.nextgatebackend.checkout_session.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class PaymentAttemptsJsonConverter implements AttributeConverter<List<CheckoutSessionEntity.PaymentAttempt>, String> {

    private final ObjectMapper objectMapper;

    public PaymentAttemptsJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<CheckoutSessionEntity.PaymentAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attempts);
        } catch (JsonProcessingException e) {
            log.error("Error converting payment attempts to JSON", e);
            throw new RuntimeException("Error converting payment attempts to JSON", e);
        }
    }

    @Override
    public List<CheckoutSessionEntity.PaymentAttempt> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<CheckoutSessionEntity.PaymentAttempt>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to payment attempts", e);
            return new ArrayList<>();
        }
    }
}
