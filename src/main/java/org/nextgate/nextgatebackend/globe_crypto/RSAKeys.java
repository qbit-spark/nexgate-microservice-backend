package org.nextgate.nextgatebackend.globe_crypto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Value object representing an RSA key pair.
 *
 * Stored as JSONB in the events table.
 * Contains both encrypted private key and public key.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RSAKeys implements Serializable {

    /**
     * Encrypted private key (Base64-encoded, encrypted with AES-256-GCM)
     * NEVER expose this via API!
     */
    private String privateKey;

    /**
     * Public key (Base64-encoded)
     * Safe to distribute to scanners
     */
    private String publicKey;

    /**
     * Algorithm used (e.g., "RS256")
     */
    private String algorithm;

    /**
     * Key size in bits (e.g., 2048)
     */
    private Integer keySize;

    /**
     * Status of the keys
     * Values: ACTIVE, ROTATED, REVOKED
     */
    private String status;

    /**
     * When the keys were generated
     */
    private Instant generatedAt;

    /**
     * Check if keys are active
     */
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    /**
     * Check if keys are revoked
     */
    public boolean isRevoked() {
        return "REVOKED".equals(status);
    }
}