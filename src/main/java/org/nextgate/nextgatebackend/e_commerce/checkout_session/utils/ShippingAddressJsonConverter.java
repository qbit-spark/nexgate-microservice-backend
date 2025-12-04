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
public class ShippingAddressJsonConverter implements AttributeConverter<ProductCheckoutSessionEntity.ShippingAddress, String> {

    private final ObjectMapper objectMapper;

    public ShippingAddressJsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(ProductCheckoutSessionEntity.ShippingAddress address) {
        if (address == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            log.error("Error converting shipping address to JSON", e);
            throw new RuntimeException("Error converting shipping address to JSON", e);
        }
    }

    @Override
    public ProductCheckoutSessionEntity.ShippingAddress convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ProductCheckoutSessionEntity.ShippingAddress.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to shipping address", e);
            return null;
        }
    }
}