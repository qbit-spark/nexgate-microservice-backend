package org.nextgate.nextgatebackend.financial_system.payment_processing.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qbitspark.jikoexpress.financial_system.payment_processing.models.PricingDetails;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

@Converter
@Slf4j
public class PricingDetailsJsonConverter implements AttributeConverter<PricingDetails, String> {

    private final ObjectMapper objectMapper;

    public PricingDetailsJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(PricingDetails pricing) {
        if (pricing == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pricing);
        } catch (JsonProcessingException e) {
            log.error("Error converting pricing details to JSON", e);
            throw new RuntimeException("Error converting pricing details to JSON", e);
        }
    }

    @Override
    public PricingDetails convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PricingDetails.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to pricing details: {}", dbData, e);
            return null;
        }
    }
}