package org.nextgate.nextgatebackend.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.checkout_session.entity.CheckoutSessionEntity;

@Converter
@Slf4j
public class InstallmentConfigJsonConverter implements
        AttributeConverter<CheckoutSessionEntity.InstallmentConfiguration, String> {

    private final ObjectMapper objectMapper;

    public InstallmentConfigJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(
            CheckoutSessionEntity.InstallmentConfiguration config) {
        if (config == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Error converting installment config to JSON", e);
            throw new RuntimeException("Error converting installment config to JSON", e);
        }
    }

    @Override
    public CheckoutSessionEntity.InstallmentConfiguration convertToEntityAttribute(
            String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData,
                    CheckoutSessionEntity.InstallmentConfiguration.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to installment config", e);
            return null;
        }
    }
}