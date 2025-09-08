package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBasicInfoRequest {

    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
            regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
            message = "Username must start with a letter and contain only letters, numbers, underscores, and hyphens"
    )
    private String userName;

    @Size(min = 1, max = 30, message = "First name must be between 1 and 30 characters")
    private String firstName;

    @Size(min = 1, max = 30, message = "Last name must be between 1 and 30 characters")
    private String lastName;

    @Size(max = 30, message = "Middle name should be less than 30 characters")
    private String middleName;

    @Email(message = "Email should be valid")
    private String email;
}
