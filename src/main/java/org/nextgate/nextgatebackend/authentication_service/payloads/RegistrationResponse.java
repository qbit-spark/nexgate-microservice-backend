package org.nextgate.nextgatebackend.authentication_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RegistrationResponse {
    private String tempToken;
    private String message;
    private LocalDateTime expireAt;
}
