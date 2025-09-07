package org.nextgate.nextgatebackend.authentication_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String tempToken;
    private String message;
    private LocalDateTime expireAt;
}
