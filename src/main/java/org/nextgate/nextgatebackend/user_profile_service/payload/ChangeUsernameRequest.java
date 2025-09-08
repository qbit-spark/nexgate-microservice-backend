package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangeUsernameRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
            message = "Username must start with a letter and contain only letters, numbers, underscores, and hyphens"
    )
    private String newUsername;

    @NotBlank(message = "Password is required for username change")
    private String password;
}
