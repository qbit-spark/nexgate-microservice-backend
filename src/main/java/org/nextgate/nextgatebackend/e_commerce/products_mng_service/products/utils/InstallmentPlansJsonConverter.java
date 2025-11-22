package org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Converter
@Slf4j
public class InstallmentPlansJsonConverter implements AttributeConverter<List<Map<String, Object>>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Map<String, Object>> installmentPlans) {
        if (installmentPlans == null || installmentPlans.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(installmentPlans);
        } catch (JsonProcessingException e) {
            log.error("Error converting installment plans to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<Map<String, Object>> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to installment plans", e);
            return new ArrayList<>();
        }
    }
}