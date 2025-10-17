package org.nextgate.nextgatebackend.globesecurity.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

@Slf4j
public class HmacUtils {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // Generate HMAC signature: timestamp + body + url
    public static String generateSignature(String timestamp, String body, String url, String secretKey) {
        try {
            String message = timestamp + body + url;

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HMAC algorithm not available", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid secret key", e);
        }
    }

    // Validate HMAC signature
    public static boolean validateSignature(String receivedSignature, String timestamp, String body, String url, String secretKey) {
        try {
            String expectedSignature = generateSignature(timestamp, body, url, secretKey);
            return MessageDigest.isEqual(
                    receivedSignature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature validation error: {}", e.getMessage());
            return false;
        }
    }

    // Validate timestamp to prevent replay attacks
    public static void validateTimestamp(String timestampStr, int toleranceMinutes) {
        try {
            Instant requestTime = Instant.parse(timestampStr);
            Instant now = Instant.now();
            Duration age = Duration.between(requestTime, now);

            // Reject future requests (clock skew > 1 min)
            if (requestTime.isAfter(now.plusSeconds(60))) {
                throw new SecurityException("Request timestamp is in the future");
            }

            // Reject old requests
            if (age.toMinutes() > toleranceMinutes) {
                throw new SecurityException(
                        String.format("Request too old (%d min). Max: %d min", age.toMinutes(), toleranceMinutes)
                );
            }

        } catch (DateTimeParseException e) {
            throw new SecurityException("Invalid timestamp format. Expected ISO-8601");
        }
    }

    // Validate both timestamp and signature
    public static boolean validateRequest(String receivedSignature, String timestamp, String body, String url,
                                          String secretKey, int toleranceMinutes) {
        validateTimestamp(timestamp, toleranceMinutes);

        if (!validateSignature(receivedSignature, timestamp, body, url, secretKey)) {
            throw new SecurityException("HMAC signature validation failed");
        }

        return true;
    }
}