package org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.entity.GroupParticipantEntity;

import java.util.ArrayList;
import java.util.List;

@Converter
@Slf4j
public class PurchaseHistoryJsonConverter implements AttributeConverter<List<GroupParticipantEntity.PurchaseRecord>, String> {

    private final ObjectMapper objectMapper;

    public PurchaseHistoryJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<GroupParticipantEntity.PurchaseRecord> purchaseHistory) {
        if (purchaseHistory == null || purchaseHistory.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(purchaseHistory);
        } catch (JsonProcessingException e) {
            log.error("Error converting purchase history to JSON", e);
            throw new RuntimeException("Error converting purchase history to JSON", e);
        }
    }

    @Override
    public List<GroupParticipantEntity.PurchaseRecord> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<GroupParticipantEntity.PurchaseRecord>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to purchase history", e);
            return new ArrayList<>();
        }
    }
}