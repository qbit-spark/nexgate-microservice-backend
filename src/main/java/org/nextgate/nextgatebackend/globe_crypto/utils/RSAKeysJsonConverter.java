package org.nextgate.nextgatebackend.globe_crypto.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.nextgate.nextgatebackend.globe_crypto.RSAKeys;

@Converter
public class RSAKeysJsonConverter implements AttributeConverter<RSAKeys, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(RSAKeys rsaKeys) {
        if (rsaKeys == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(rsaKeys);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting RSAKeys to JSON", e);
        }
    }

    @Override
    public RSAKeys convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, RSAKeys.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to RSAKeys: " + dbData, e);
        }
    }
}