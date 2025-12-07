package org.nextgate.nextgatebackend.financial_system.payment_processing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;

@Converter
@Slf4j
public class PricingDetailsJsonConverter implements
        AttributeConverter<ProductCheckoutSessionEntity.PricingSummary, String> {

    private final ObjectMapper objectMapper;

    public PricingDetailsJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(ProductCheckoutSessionEntity.PricingSummary pricing) {
        if (pricing == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(pricing);
            log.debug("Successfully converted pricing to JSON: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            log.error("❌ ERROR converting pricing to JSON. Pricing: {}", pricing, e);
            throw new RuntimeException("Error converting pricing to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public ProductCheckoutSessionEntity.PricingSummary convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ProductCheckoutSessionEntity.PricingSummary.class);
        } catch (JsonProcessingException e) {
            log.error("❌ ERROR converting JSON to pricing: {}", dbData, e);
            return null;
        }
    }
}