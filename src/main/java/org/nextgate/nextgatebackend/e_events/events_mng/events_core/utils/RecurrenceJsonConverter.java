package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.Recurrence;

@Converter
public class RecurrenceJsonConverter implements AttributeConverter<Recurrence, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()); // For LocalDate support

    @Override
    public String convertToDatabaseColumn(Recurrence recurrence) {
        if (recurrence == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(recurrence);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting Recurrence to JSON", e);
        }
    }

    @Override
    public Recurrence convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, Recurrence.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to Recurrence", e);
        }
    }
}