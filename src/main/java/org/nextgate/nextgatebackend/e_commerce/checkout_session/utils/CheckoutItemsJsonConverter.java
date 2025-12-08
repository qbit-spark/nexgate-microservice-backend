package org.nextgate.nextgatebackend.e_commerce.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Converter for List<ProductCheckoutSessionEntity.CheckoutItem>
 * Converts between Java list and JSONB array database column
 */
@Converter
@Slf4j
public class CheckoutItemsJsonConverter implements AttributeConverter<List<ProductCheckoutSessionEntity.CheckoutItem>, String> {

    private final ObjectMapper objectMapper;

    public CheckoutItemsJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<ProductCheckoutSessionEntity.CheckoutItem> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        try {
            String json = objectMapper.writeValueAsString(items);
            log.debug("Successfully converted {} items to JSON", items.size());
            return json;
        } catch (JsonProcessingException e) {
            log.error("❌ ERROR converting checkout items to JSON. Items: {}", items, e);
            throw new RuntimeException("Error converting checkout items to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProductCheckoutSessionEntity.CheckoutItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<ProductCheckoutSessionEntity.CheckoutItem>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to checkout items: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}