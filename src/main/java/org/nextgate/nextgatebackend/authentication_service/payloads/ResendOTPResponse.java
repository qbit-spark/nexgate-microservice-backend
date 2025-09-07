package org.nextgate.nextgatebackend.authentication_service.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ResendOTPResponse {
    private String newTempToken;
    private String message;
    private LocalDateTime expireAt;
    private int attemptsRemaining;
    private LocalDateTime nextResendAllowedAt;
}