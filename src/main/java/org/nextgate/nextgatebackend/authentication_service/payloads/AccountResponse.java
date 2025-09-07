package org.nextgate.nextgatebackend.authentication_service.payloads;

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
public class AccountResponse {
    private UUID id;
    private String userName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private Boolean isVerified;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private Set<String> roles;
}