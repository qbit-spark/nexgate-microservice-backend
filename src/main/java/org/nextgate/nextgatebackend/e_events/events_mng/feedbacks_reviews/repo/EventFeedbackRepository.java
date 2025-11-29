package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.entity.EventFeedbackEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventFeedbackRepository extends JpaRepository<EventFeedbackEntity, UUID> {

    boolean existsByEventAndUser(EventEntity event, AccountEntity user);

    Page<EventFeedbackEntity> findByEventId(UUID eventId, Pageable pageable);

}