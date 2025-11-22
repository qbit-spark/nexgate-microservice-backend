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
public class TransferHistoryJsonConverter implements AttributeConverter<List<GroupParticipantEntity.TransferHistory>, String> {

    private final ObjectMapper objectMapper;

    public TransferHistoryJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(List<GroupParticipantEntity.TransferHistory> transferHistory) {
        if (transferHistory == null || transferHistory.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(transferHistory);
        } catch (JsonProcessingException e) {
            log.error("Error converting transfer history to JSON", e);
            throw new RuntimeException("Error converting transfer history to JSON", e);
        }
    }

    @Override
    public List<GroupParticipantEntity.TransferHistory> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty() || "[]".equals(dbData)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<GroupParticipantEntity.TransferHistory>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to transfer history", e);
            return new ArrayList<>();
        }
    }
}