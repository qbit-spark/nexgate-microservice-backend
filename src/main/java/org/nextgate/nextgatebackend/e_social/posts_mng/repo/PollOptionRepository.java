package org.nextgate.nextgatebackend.e_social.posts_mng.repo;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOptionEntity, UUID> {

    List<PollOptionEntity> findByPollIdOrderByOptionOrder(UUID pollId);

    void deleteByPollId(UUID pollId);
}