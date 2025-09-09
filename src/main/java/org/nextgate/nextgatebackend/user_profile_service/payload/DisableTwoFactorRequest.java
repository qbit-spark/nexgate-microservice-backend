package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisableTwoFactorRequest {

    @NotBlank(message = "Password is required to disable two-factor authentication")
    private String password;
}
