package org.nextgate.nextgatebackend.user_profile_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String userName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phoneNumber;
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isTwoFactorEnabled;
    private Boolean isAccountLocked;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private Set<String> roles;
}