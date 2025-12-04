package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.service;

import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.entity.UserPrivacySettings;

import java.util.UUID;

public interface AccountPrivacyService {

    UserPrivacySettings getOrCreatePrivacySettings();

    UserPrivacySettings updatePrivacySettings(boolean isPrivate);

    boolean isAccountPrivate(UUID userId);

    UserPrivacySettings getPrivacySettings(UUID userId);
}