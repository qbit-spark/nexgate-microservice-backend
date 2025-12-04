
package org.nextgate.nextgatebackend.e_events.events_mng.feedbacks_reviews.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFeedbackResponse {
    private UUID id;
    private UUID eventId;
    private String eventTitle;
    private UUID userId;
    private String userName; // Or fullName, depending on what you want to show
    private Integer rating;
    private String comment;
    private Instant createdAt;
}