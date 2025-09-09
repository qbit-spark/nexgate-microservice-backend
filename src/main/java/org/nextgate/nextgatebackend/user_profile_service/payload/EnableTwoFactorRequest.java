package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnableTwoFactorRequest {

    @NotBlank(message = "Password is required to enable two-factor authentication")
    private String password;
}
