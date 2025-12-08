package org.nextgate.nextgatebackend.globe_crypto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.globe_crypto.enums.KeyStatus;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Service for generating and managing RSA key pairs for JWT signing.
 *
 * Each event gets its own RSA key pair:
 * - Private key: Used to sign tickets (encrypted before storage)
 * - Public key: Distributed to scanners for signature verification
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RSAKeyService {

    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private final KeyEncryptionService encryptionService;

    /**
     * Generate a new RSA key pair for an event
     * Returns RSAKeys object with encrypted private key
     *
     * @return RSAKeys value object containing encrypted private key and public key
     */
    public RSAKeys generateKeys() {
        try {
            log.info("Generating RSA-{} key pair...", KEY_SIZE);

            // Generate RSA key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            // Encode private key to Base64
            String privateKeyBase64 = Base64.getEncoder()
                    .encodeToString(keyPair.getPrivate().getEncoded());

            // Encode public key to Base64
            String publicKeyBase64 = Base64.getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            // Encrypt private key before storage
            String encryptedPrivateKey = encryptionService.encrypt(privateKeyBase64);

            // Create RSAKeys value object
            RSAKeys rsaKeys = new RSAKeys();
            rsaKeys.setPrivateKey(encryptedPrivateKey);
            rsaKeys.setPublicKey(publicKeyBase64);
            rsaKeys.setAlgorithm("RS256");
            rsaKeys.setKeySize(KEY_SIZE);
            rsaKeys.setStatus(KeyStatus.ACTIVE);
            rsaKeys.setGeneratedAt(Instant.now());

            log.info("RSA key pair generated successfully");

            return rsaKeys;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Load private key from encrypted Base64 string
     * Used for signing tickets
     *
     * @param encryptedPrivateKeyBase64 Encrypted Base64-encoded private key
     * @return PrivateKey object for signing
     */
    public PrivateKey loadPrivateKey(String encryptedPrivateKeyBase64) {
        try {
            // Decrypt private key
            String decryptedPrivateKey = encryptionService.decrypt(encryptedPrivateKeyBase64);

            // Decode from Base64
            byte[] keyBytes = Base64.getDecoder().decode(decryptedPrivateKey);

            // Create PrivateKey object
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
        }
    }

    /**
     * Load public key from Base64 string
     * Used for signature verification
     *
     * @param publicKeyBase64 Base64-encoded public key
     * @return PublicKey object for verification
     */
    public PublicKey loadPublicKey(String publicKeyBase64) {
        try {
            // Decode from Base64
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);

            // Create PublicKey object
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);

            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    /**
     * Sign data with private key
     * Used for signing JWT tokens
     *
     * @param data Data to sign
     * @param privateKey Private key for signing
     * @return Base64-encoded signature
     */
    public String sign(String data, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data.getBytes());

            byte[] signatureBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }

    /**
     * Verify signature with public key
     * Used for validating JWT tokens
     *
     * @param data Original data
     * @param signatureBase64 Base64-encoded signature
     * @param publicKey Public key for verification
     * @return true if signature is valid
     */
    public boolean verify(String data, String signatureBase64, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data.getBytes());

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Get private key from RSAKeys object
     * Convenience method for loading private key from event
     *
     * @param rsaKeys RSAKeys object from event
     * @return PrivateKey for signing
     */
    public PrivateKey getPrivateKey(RSAKeys rsaKeys) {
        if (rsaKeys == null || rsaKeys.getPrivateKey() == null) {
            throw new IllegalArgumentException("RSA keys not found");
        }

        if (!"ACTIVE".equals(rsaKeys.getStatus())) {
            throw new IllegalStateException("RSA keys are not active");
        }

        return loadPrivateKey(rsaKeys.getPrivateKey());
    }

    /**
     * Get public key from RSAKeys object
     * Convenience method for loading public key from event
     *
     * @param rsaKeys RSAKeys object from event
     * @return PublicKey for verification
     */
    public PublicKey getPublicKey(RSAKeys rsaKeys) {
        if (rsaKeys == null || rsaKeys.getPublicKey() == null) {
            throw new IllegalArgumentException("RSA keys not found");
        }

        return loadPublicKey(rsaKeys.getPublicKey());
    }

    /**
     * Get public key as Base64 string
     * Used for sending to scanners
     *
     * @param rsaKeys RSAKeys object from event
     * @return Base64-encoded public key
     */
    public String getPublicKeyBase64(RSAKeys rsaKeys) {
        if (rsaKeys == null || rsaKeys.getPublicKey() == null) {
            throw new IllegalArgumentException("RSA keys not found");
        }

        return rsaKeys.getPublicKey();
    }
}