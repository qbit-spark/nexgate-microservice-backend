package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.utils;

import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.entity.EventFeedbackEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads.EventFeedbackResponse;
import org.springframework.stereotype.Component;

@Component
public class EventFeedbackMapper {

    public EventFeedbackResponse toResponse(EventFeedbackEntity feedback) {
        return EventFeedbackResponse.builder()
                .id(feedback.getId())
                .eventId(feedback.getEvent().getId())
                .eventTitle(feedback.getEvent().getTitle())
                .userId(feedback.getUser().getId())
                .userName(feedback.getUser().getUserName()) // Assuming AccountEntity has userName
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}