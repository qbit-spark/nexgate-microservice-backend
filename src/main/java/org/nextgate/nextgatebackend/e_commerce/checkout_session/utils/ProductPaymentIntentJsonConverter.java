package org.nextgate.nextgatebackend.e_commerce.checkout_session.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_commerce.checkout_session.entity.ProductCheckoutSessionEntity;

@Converter(autoApply = false)
@Slf4j
public class ProductPaymentIntentJsonConverter
        implements AttributeConverter<ProductCheckoutSessionEntity.PaymentIntent, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ProductCheckoutSessionEntity.PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(paymentIntent);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert PaymentIntent to JSON", e);
            throw new IllegalArgumentException("Cannot convert PaymentIntent to JSON", e);
        }
    }

    @Override
    public ProductCheckoutSessionEntity.PaymentIntent convertToEntityAttribute(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, ProductCheckoutSessionEntity.PaymentIntent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert JSON to PaymentIntent", e);
            throw new IllegalArgumentException("Cannot convert JSON to PaymentIntent", e);
        }
    }
}