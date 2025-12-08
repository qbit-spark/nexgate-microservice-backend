package org.nextgate.nextgatebackend.globe_crypto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nextgate.nextgatebackend.globe_crypto.enums.KeyStatus;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RSAKeys implements Serializable {

    private String privateKey;
    private String publicKey;
    private String algorithm;
    private Integer keySize;
    private KeyStatus status;  // ← Use enum instead of String
    private Instant generatedAt;
    public boolean isActive() {
        return status == KeyStatus.ACTIVE;
    }

    public boolean isRevoked() {
        return status == KeyStatus.REVOKED;
    }
}