
package org.nextgate.nextgatebackend.e_commerce.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Converter
@Slf4j
public class OrderIdsJsonConverter implements AttributeConverter<List<UUID>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(orderIds);
        } catch (JsonProcessingException e) {
            log.error("Error converting order IDs to JSON", e);
            throw new RuntimeException("Error converting order IDs to JSON", e);
        }
    }

    @Override
    public List<UUID> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            // Explicitly return ArrayList to ensure mutability
            return new ArrayList<>(objectMapper.readValue(dbData, new TypeReference<List<UUID>>() {}));
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to order IDs", e);
            return new ArrayList<>();
        }
    }
}