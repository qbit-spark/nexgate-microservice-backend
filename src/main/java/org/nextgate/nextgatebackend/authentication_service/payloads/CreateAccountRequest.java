package org.nextgate.nextgatebackend.authentication_service.payloads;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.nextgate.nextgatebackend.authentication_service.enums.VerificationChannels;

@Data
public class CreateAccountRequest {
    // Phone number validation for any country using E.164 format
    @NotBlank(message = "Phone number is mandatory")
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone number must be in valid international format (e.g., +1234567890)"
    )
    private String phoneNumber;

    // Password validation for strong password
    @NotBlank(message = "Password is mandatory")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
            message = "Password must be at least 8 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(max = 30, message = "First name should be less than 30 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 30, message = "Last name should be less than 30 characters")
    private String lastName;

    @NotBlank(message = "Middle name is required")
    @Size(max = 30, message = "Middle name should be less than 30 characters")
    private String middleName;

    @NotNull(message = "Verification channel is mandatory")
    private VerificationChannels verificationChannel;
}
