package org.nextgate.nextgatebackend.payment_methods.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.payment_methods.entity.PaymentMethodsEntity;

import java.util.List;

@Converter
@Slf4j
public class BillingAddressJsonConverter implements AttributeConverter<PaymentMethodsEntity.BillingAddress, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(PaymentMethodsEntity.BillingAddress attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting BillingAddress to JSON", e);
            throw new RuntimeException("Error converting BillingAddress to JSON", e);
        }
    }

    @Override
    public PaymentMethodsEntity.BillingAddress convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PaymentMethodsEntity.BillingAddress.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to BillingAddress", e);
            throw new RuntimeException("Error converting JSON to BillingAddress", e);
        }
    }
}