package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.embedded.VirtualDetails;

@Converter
@Slf4j
public class VirtualDetailsJsonConverter
        implements AttributeConverter<VirtualDetails, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(VirtualDetails virtualDetails) {
        if (virtualDetails == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(virtualDetails);
        } catch (JsonProcessingException e) {
            log.error("Error converting VirtualDetails to JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to convert VirtualDetails to JSON", e);
        }
    }

    @Override
    public VirtualDetails convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, VirtualDetails.class);
        } catch (JsonProcessingException e) {
            log.error("Error converting JSON to VirtualDetails: {}", e.getMessage());
            throw new RuntimeException("Failed to convert JSON to VirtualDetails", e);
        }
    }
}