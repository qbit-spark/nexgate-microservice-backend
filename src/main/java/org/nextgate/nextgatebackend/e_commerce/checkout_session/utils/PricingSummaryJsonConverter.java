package org.nextgate.nextgatebackend.e_commerce.checkout_session.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.CheckoutSessionEntity;

@Converter
@Slf4j
public class PricingSummaryJsonConverter implements AttributeConverter<CheckoutSessionEntity.PricingSummary, String> {

    private final ObjectMapper objectMapper;

    public PricingSummaryJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(CheckoutSessionEntity.PricingSummary pricing) {
        if (pricing == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pricing);
        } catch (JsonProcessingException e) {
            log.error("Error converting pricing summary to JSON", e);
            throw new RuntimeException("Error converting pricing summary to JSON", e);
        }
    }

    @Override
    public CheckoutSessionEntity.PricingSummary convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, CheckoutSessionEntity.PricingSummary.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to pricing summary", e);
            return null;
        }
    }
}
