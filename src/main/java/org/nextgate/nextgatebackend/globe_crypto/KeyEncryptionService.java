package org.nextgate.nextgatebackend.globe_crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data (like RSA private keys)
 * using AES-256-GCM encryption.
 *
 * The encryption secret should be set via environment variable:
 * RSA_ENCRYPTION_KEY (minimum 32 characters for AES-256)
 */
@Service
public class KeyEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    @Value("${events.rsa.encryption.secret}")
    private String encryptionSecret;

    /**
     * Encrypt plaintext using AES-256-GCM
     *
     * @param plaintext Text to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext)
     */
    public String encrypt(String plaintext) {
        try {
            // Validate encryption secret length
            if (encryptionSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "Encryption secret must be at least 32 characters for AES-256"
                );
            }

            // Create AES key from secret (take first 32 bytes for AES-256)
            byte[] keyBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
            byte[] key = new byte[32]; // 256 bits
            System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            // Generate random IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the plaintext
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + encrypted data
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            // Return as Base64
            return Base64.getEncoder().encodeToString(combined);

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt encrypted data using AES-256-GCM
     *
     * @param encryptedBase64 Base64-encoded encrypted data (IV + ciphertext)
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedBase64) {
        try {
            // Decode from Base64
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            // Extract IV and encrypted data
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // Create AES key from secret
            byte[] keyBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
            byte[] key = new byte[32];
            System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));

            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt the data
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }

    /**
     * Validate that encryption secret is properly configured
     * Called on application startup
     */
    public void validateConfiguration() {
        if (encryptionSecret.equals("change-me-in-production-32-characters-minimum")) {
            throw new IllegalStateException(
                    "SECURITY WARNING: Default encryption secret detected! " +
                            "Set RSA_ENCRYPTION_KEY environment variable in production."
            );
        }

        if (encryptionSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Encryption secret must be at least 32 characters for AES-256"
            );
        }
    }
}