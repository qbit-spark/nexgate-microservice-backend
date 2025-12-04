package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo;

import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.MuteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MuteRepository extends JpaRepository<MuteEntity, UUID> {

    Optional<MuteEntity> findByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    boolean existsByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    void deleteByMuterIdAndMutedId(UUID muterId, UUID mutedId);

    List<MuteEntity> findByMuterIdOrderByCreatedAtDesc(UUID muterId);
}