package org.nextgate.nextgatebackend.authentication_service.payloads;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailPasswordResetRequest {
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email must be valid")
    private String email;
}
