package org.nextgate.nextgatebackend.payment_methods.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;

/**
 * Converter for PaymentMethodDetails to JSON
 */
@Converter
@Slf4j
public class PaymentMethodDetailsJsonConverter implements AttributeConverter<PaymentMethodsEntity.PaymentMethodDetails, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PaymentMethodsEntity.PaymentMethodDetails attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting PaymentMethodDetails to JSON", e);
            throw new RuntimeException("Error converting PaymentMethodDetails to JSON", e);
        }
    }

    @Override
    public PaymentMethodsEntity.PaymentMethodDetails convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PaymentMethodsEntity.PaymentMethodDetails.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to PaymentMethodDetails", e);
            throw new RuntimeException("Error converting JSON to PaymentMethodDetails", e);
        }
    }
}