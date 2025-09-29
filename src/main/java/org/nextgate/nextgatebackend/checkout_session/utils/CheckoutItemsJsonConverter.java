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
public class CheckoutItemsJsonConverter implements AttributeConverter<List<CheckoutSessionEntity.CheckoutItem>, String> {

    private final ObjectMapper objectMapper;

    public CheckoutItemsJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<CheckoutSessionEntity.CheckoutItem> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.error("Error converting checkout items to JSON", e);
            throw new RuntimeException("Error converting checkout items to JSON", e);
        }
    }

    @Override
    public List<CheckoutSessionEntity.CheckoutItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<CheckoutSessionEntity.CheckoutItem>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to checkout items", e);
            return new ArrayList<>();
        }
    }
}

