package org.nextgate.nextgatebackend.authentication_service.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PswResetAndOTPRequestBody {
    @NotBlank(message = "Temp OTP is mandatory")
    private String tempToken;
    @NotBlank(message = "OTP code is mandatory")
    private String code;
    @NotBlank(message = "Password is mandatory")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String newPassword;
}
