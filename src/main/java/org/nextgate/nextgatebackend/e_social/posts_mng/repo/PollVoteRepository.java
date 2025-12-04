package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVoteEntity, UUID> {

    boolean existsByPollIdAndUserId(UUID pollId, UUID userId);

    boolean existsByPollIdAndUserIdAndOptionId(UUID pollId, UUID userId, UUID optionId);

    List<PollVoteEntity> findByPollIdAndUserId(UUID pollId, UUID userId);

    long countByOptionId(UUID optionId);

    void deleteByPollId(UUID pollId);

    void deleteByPollIdAndUserId(UUID pollId, UUID userId);
}