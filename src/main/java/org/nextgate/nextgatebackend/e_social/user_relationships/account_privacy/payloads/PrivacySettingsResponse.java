package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PrivacySettingsResponse {
    private UUID id;
    private UUID userId;
    private boolean isPrivate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}