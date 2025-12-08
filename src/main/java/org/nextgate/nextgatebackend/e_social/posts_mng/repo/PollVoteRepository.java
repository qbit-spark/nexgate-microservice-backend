package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVoteEntity, UUID> {

    boolean existsByPollIdAndVoterId(UUID pollId, UUID voterId);

    boolean existsByPollIdAndVoterIdAndOptionId(UUID pollId, UUID voterId, UUID optionId);

    List<PollVoteEntity> findByPollIdAndVoterId(UUID pollId, UUID voterId);

    long countByOptionId(UUID optionId);

    void deleteByPollId(UUID pollId);

    void deleteByPollIdAndVoterId(UUID pollId, UUID voderId);

    boolean existsByOptionIdAndVoterId(UUID optionId, UUID voterId);

    List<PollVoteEntity> findByOptionId(UUID optionId);

    List<PollVoteEntity> findByOptionIdOrderByCreatedAtDesc(UUID optionId);
}