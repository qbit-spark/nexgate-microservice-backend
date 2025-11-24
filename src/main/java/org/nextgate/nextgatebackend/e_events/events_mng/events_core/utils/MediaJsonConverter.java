package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Media;

@Converter
public class MediaJsonConverter implements AttributeConverter<Media, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Media media) {
        if (media == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(media);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting Media to JSON", e);
        }
    }

    @Override
    public Media convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, Media.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to Media", e);
        }
    }
}