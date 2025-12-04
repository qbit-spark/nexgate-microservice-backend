package org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.repo;

import org.nextgate.nextgatebackend.e_social.user_relationships.account_privacy.entity.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPrivacyRepository extends JpaRepository<UserPrivacySettings, UUID> {

    Optional<UserPrivacySettings> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}