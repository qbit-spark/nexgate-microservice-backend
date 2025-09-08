package org.nextgate.nextgatebackend.user_profile_service.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.nextgate.nextgatebackend.user_profile_service.payload.ChangePasswordRequest;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, ChangePasswordRequest> {

    @Override
    public void initialize(PasswordsMatch constraintAnnotation) {
        // Initialization logic if needed
    }

    @Override
    public boolean isValid(ChangePasswordRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let other validators handle null checks
        }

        boolean isValid = request.isPasswordsMatch();

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("New password and confirm password do not match")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
