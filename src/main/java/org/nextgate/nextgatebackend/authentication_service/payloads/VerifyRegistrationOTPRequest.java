package org.nextgate.nextgatebackend.authentication_service.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyRegistrationOTPRequest {

    @NotBlank(message = "Temp token is mandatory")
    private String tempToken;

    @NotBlank(message = "OTP code is mandatory")
    @Pattern(regexp = "\\d{6}", message = "OTP code must be a 6-digit number")
    private String otpCode;
}
