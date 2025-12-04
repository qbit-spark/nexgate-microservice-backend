package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.entity.UserPrivacySettings;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.repo.UserPrivacyRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.service.AccountPrivacyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountPrivacyServiceImpl implements AccountPrivacyService {

    private final UserPrivacyRepository privacyRepository;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public UserPrivacySettings getOrCreatePrivacySettings() {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID userId = authenticatedUser.getId();

        return privacyRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    @Override
    @Transactional
    public UserPrivacySettings updatePrivacySettings(boolean isPrivate) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID userId = authenticatedUser.getId();

        UserPrivacySettings settings = privacyRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setPrivate(isPrivate);
        settings.setUpdatedAt(LocalDateTime.now());

        return privacyRepository.save(settings);
    }

    @Override
    public boolean isAccountPrivate(UUID userId) {
        return privacyRepository.findByUserId(userId)
                .map(UserPrivacySettings::isPrivate)
                .orElse(false);
    }

    @Override
    public UserPrivacySettings getPrivacySettings(UUID userId) {
        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return privacyRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
    }

    private UserPrivacySettings createDefaultSettings(UUID userId) {
        UserPrivacySettings settings = new UserPrivacySettings();
        settings.setUserId(userId);
        settings.setPrivate(false);
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());

        return privacyRepository.save(settings);
    }

    private AccountEntity getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}