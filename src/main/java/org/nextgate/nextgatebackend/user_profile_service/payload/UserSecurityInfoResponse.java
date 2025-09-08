package org.nextgate.nextgatebackend.user_profile_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSecurityInfoResponse {
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean isTwoFactorEnabled;
    private Boolean isAccountLocked;
    private LocalDateTime lastPasswordChange;
    private LocalDateTime accountCreatedAt;
    private Set<String> roles;
    private SecurityStrength securityStrength;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SecurityStrength {
        private int score; // 0-100
        private String level; // WEAK, MEDIUM, STRONG
        private String description;
        private List<String> recommendations;
    }
}