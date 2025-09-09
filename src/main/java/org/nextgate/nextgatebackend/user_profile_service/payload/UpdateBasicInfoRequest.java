package org.nextgate.nextgatebackend.user_profile_service.payload;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.util.List;

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

    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone number must be in valid international format (e.g., +1234567890)"
    )
    private String phoneNumber;

    @Size(max = 5, message = "Maximum 5 profile pictures allowed")
    private List<@URL @Size(max = 500) String> profilePictureUrls;

}
