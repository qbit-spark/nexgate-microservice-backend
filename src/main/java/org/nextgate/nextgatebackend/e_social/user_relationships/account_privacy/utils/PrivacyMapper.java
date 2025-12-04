package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.utils;

import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.payloads.PrivacySettingsResponse;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.entity.UserPrivacySettings;
import org.springframework.stereotype.Component;

@Component
public class PrivacyMapper {

    public PrivacySettingsResponse toResponse(UserPrivacySettings settings) {
        if (settings == null) {
            return null;
        }

        PrivacySettingsResponse response = new PrivacySettingsResponse();
        response.setId(settings.getId());
        response.setUserId(settings.getUserId());
        response.setPrivate(settings.isPrivate());
        response.setCreatedAt(settings.getCreatedAt());
        response.setUpdatedAt(settings.getUpdatedAt());

        return response;
    }
}