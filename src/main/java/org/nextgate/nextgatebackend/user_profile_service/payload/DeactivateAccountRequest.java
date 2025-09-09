package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeactivateAccountRequest {

    @NotBlank(message = "Password is required to deactivate account")
    private String password;

    @Size(max = 500, message = "Reason should not exceed 500 characters")
    private String reason; // Optional reason for deactivation
}