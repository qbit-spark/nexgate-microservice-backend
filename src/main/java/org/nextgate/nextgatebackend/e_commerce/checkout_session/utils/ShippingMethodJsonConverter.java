package org.nextgate.nextgatebackend.e_commerce.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;

@Converter
@Slf4j
public class ShippingMethodJsonConverter implements AttributeConverter<ProductCheckoutSessionEntity.ShippingMethod, String> {

    private final ObjectMapper objectMapper;

    public ShippingMethodJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(ProductCheckoutSessionEntity.ShippingMethod shippingMethod) {
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
    public ProductCheckoutSessionEntity.ShippingMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ProductCheckoutSessionEntity.ShippingMethod.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to shipping method", e);
            return null;
        }
    }
}