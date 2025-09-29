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
public class ShippingMethodJsonConverter implements AttributeConverter<CheckoutSessionEntity.ShippingMethod, String> {

    private final ObjectMapper objectMapper;

    public ShippingMethodJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(CheckoutSessionEntity.ShippingMethod shippingMethod) {
        if (shippingMethod == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(shippingMethod);
        } catch (JsonProcessingException e) {
            log.error("Error converting shipping method to JSON", e);
            throw new RuntimeException("Error converting shipping method to JSON", e);
        }
    }

    @Override
    public CheckoutSessionEntity.ShippingMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, CheckoutSessionEntity.ShippingMethod.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to shipping method", e);
            return null;
        }
    }
}