package org.nextgate.nextgatebackend.globe_crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for generating and validating JWT tokens for event tickets.
 *
 * Each ticket gets a signed JWT that contains:
 * - Ticket information (ID, type, attendee)
 * - Event information (ID, name, date)
 * - Validity period
 * - Cryptographic signature (signed with event's RSA private key)
 *
 * The JWT can be validated offline by scanners using the event's public key.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketJWTService {

    private final RSAKeyService rsaKeyService;
    private final ObjectMapper objectMapper;

    /**
     * Generate a signed JWT token for a ticket
     *
     * @param ticketData All the data to include in the JWT
     * @param rsaKeys Event's RSA key pair (used to sign)
     * @return Signed JWT token string
     */
    public String generateTicketJWT(TicketJWTData ticketData, RSAKeys rsaKeys) {
        try {
            log.debug("Generating JWT for ticket: {}", ticketData.getTicketInstanceId());

            // 1. Create JWT Header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "JWT");

            // 2. Create JWT Payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("ticketInstanceId", ticketData.getTicketInstanceId().toString());
            payload.put("ticketTypeId", ticketData.getTicketTypeId().toString());
            payload.put("ticketTypeName", ticketData.getTicketTypeName());
            payload.put("ticketSeries", ticketData.getTicketSeries());
            payload.put("eventId", ticketData.getEventId().toString());
            payload.put("eventName", ticketData.getEventName());
            payload.put("eventStartDateTime", ticketData.getEventStartDateTime().toString());
            payload.put("attendeeName", ticketData.getAttendeeName());

            // Mask email for privacy (show only first char and domain)
            payload.put("attendeeEmail", maskEmail(ticketData.getAttendeeEmail()));

            payload.put("attendanceMode", ticketData.getAttendanceMode());
            payload.put("bookingReference", ticketData.getBookingReference());

            // Event schedules (for multi-day events)
            if (ticketData.getEventSchedules() != null && !ticketData.getEventSchedules().isEmpty()) {
                payload.put("eventSchedules", ticketData.getEventSchedules());
            }

            // Validity period from ticket
            payload.put("validFrom", ticketData.getValidFrom().toInstant().getEpochSecond());
            payload.put("validUntil", ticketData.getValidUntil().toInstant().getEpochSecond());

            // JWT standard claims
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", ticketData.getValidUntil().toInstant().getEpochSecond());

            // Optional fields
            if (ticketData.getAttendeePhone() != null) {
                // Mask phone number (show only last 4 digits)
                payload.put("attendeePhone", maskPhone(ticketData.getAttendeePhone()));
            }
            if (ticketData.getSeatNumber() != null) {
                payload.put("seatNumber", ticketData.getSeatNumber());
            }

            // 3. Encode header and payload as Base64
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));

            // 4. Create signature input
            String signatureInput = encodedHeader + "." + encodedPayload;

            // 5. Sign with event's private key
            PrivateKey privateKey = rsaKeyService.getPrivateKey(rsaKeys);
            String signature = rsaKeyService.sign(signatureInput, privateKey);
            String encodedSignature = base64UrlEncode(signature);

            // 6. Combine into final JWT
            String jwt = signatureInput + "." + encodedSignature;

            log.info("JWT generated successfully for ticket: {}", ticketData.getTicketInstanceId());

            return jwt;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate JWT for ticket", e);
        }
    }

    /**
     * Validate a JWT token using event's public key
     *
     * @param jwt JWT token to validate
     * @param rsaKeys Event's RSA keys (public key used for verification)
     * @return Validation result with ticket data if valid
     */
    public JWTValidationResult validateTicketJWT(String jwt, RSAKeys rsaKeys) {
        try {
            log.debug("Validating JWT token");

            // 1. Split JWT into parts
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                return JWTValidationResult.invalid("Invalid JWT format");
            }

            String encodedHeader = parts[0];
            String encodedPayload = parts[1];
            String encodedSignature = parts[2];

            // 2. Verify signature
            String signatureInput = encodedHeader + "." + encodedPayload;
            PublicKey publicKey = rsaKeyService.getPublicKey(rsaKeys);
            String signature = base64UrlDecode(encodedSignature);

            boolean signatureValid = rsaKeyService.verify(signatureInput, signature, publicKey);

            if (!signatureValid) {
                log.warn("JWT signature verification failed");
                return JWTValidationResult.invalid("Invalid signature");
            }

            // 3. Decode and parse payload
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(encodedPayload),
                    StandardCharsets.UTF_8
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // 4. Check expiration
            long exp = ((Number) payload.get("exp")).longValue();
            long now = Instant.now().getEpochSecond();

            if (now > exp) {
                log.warn("JWT token has expired");
                return JWTValidationResult.invalid("Token expired");
            }

            // 5. Extract ticket data
            UUID ticketInstanceId = UUID.fromString((String) payload.get("ticketInstanceId"));
            UUID eventId = UUID.fromString((String) payload.get("eventId"));
            String attendeeName = (String) payload.get("attendeeName");
            String ticketTypeName = (String) payload.get("ticketTypeName");
            String eventName = (String) payload.get("eventName");

            log.info("JWT validated successfully for ticket: {}", ticketInstanceId);

            return JWTValidationResult.valid(
                    ticketInstanceId,
                    eventId,
                    attendeeName,
                    ticketTypeName,
                    eventName,
                    payload
            );

        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return JWTValidationResult.invalid("Validation error: " + e.getMessage());
        }
    }

    /**
     * Decode JWT payload without validation (for display purposes only)
     * DO NOT use this for security decisions!
     *
     * @param jwt JWT token
     * @return Decoded payload
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> decodePayloadUnsafe(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format");
            }

            String encodedPayload = parts[1];
            String payloadJson = new String(
                    Base64.getUrlDecoder().decode(encodedPayload),
                    StandardCharsets.UTF_8
            );

            return objectMapper.readValue(payloadJson, Map.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT payload", e);
        }
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Base64 URL encode (without padding)
     */
    private String base64UrlEncode(String input) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 URL decode
     */
    private String base64UrlDecode(String encoded) {
        byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    /**
     * Mask email for privacy in JWT
     * Example: john.doe@example.com → j***@example.com
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 1) {
            return localPart.charAt(0) + "***" + domain;
        }

        return localPart.charAt(0) + "***" + domain;
    }

    /**
     * Mask phone number for privacy in JWT
     * Example: +255712345678 → ***5678
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "";
        }

        if (phone.length() <= 4) {
            return "***";
        }

        return "***" + phone.substring(phone.length() - 4);
    }

    // ========================================
    // DATA CLASSES
    // ========================================

    /**
     * Data required to generate a ticket JWT
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TicketJWTData {
        private UUID ticketInstanceId;
        private UUID ticketTypeId;
        private String ticketTypeName;
        private String ticketSeries;
        private UUID eventId;
        private String eventName;
        private ZonedDateTime eventStartDateTime;
        private String attendeeName;
        private String attendeeEmail;
        private String attendeePhone;
        private String attendanceMode;
        private String bookingReference;
        private String seatNumber;

        // Validity period (from ticket)
        private ZonedDateTime validFrom;
        private ZonedDateTime validUntil;

        // Event schedules (for multi-day events)
        // List of dates/times when event is happening
        private java.util.List<EventSchedule> eventSchedules;
    }

    /**
     * Represents a single day/session in a multi-day event
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EventSchedule {
        private String dayName;              // e.g., "Day 1", "Friday"
        private ZonedDateTime startDateTime;
        private ZonedDateTime endDateTime;
        private String description;          // Optional: "Workshop sessions"
    }

    /**
     * Result of JWT validation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JWTValidationResult {
        private boolean valid;
        private String errorMessage;
        private UUID ticketInstanceId;
        private UUID eventId;
        private String attendeeName;
        private String ticketTypeName;
        private String eventName;
        private Map<String, Object> payload;

        public static JWTValidationResult valid(
                UUID ticketInstanceId,
                UUID eventId,
                String attendeeName,
                String ticketTypeName,
                String eventName,
                Map<String, Object> payload
        ) {
            return JWTValidationResult.builder()
                    .valid(true)
                    .ticketInstanceId(ticketInstanceId)
                    .eventId(eventId)
                    .attendeeName(attendeeName)
                    .ticketTypeName(ticketTypeName)
                    .eventName(eventName)
                    .payload(payload)
                    .build();
        }

        public static JWTValidationResult invalid(String errorMessage) {
            return JWTValidationResult.builder()
                    .valid(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}