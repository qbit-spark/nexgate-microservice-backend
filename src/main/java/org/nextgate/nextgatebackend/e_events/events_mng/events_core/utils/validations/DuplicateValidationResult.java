package org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.validations;

import lombok.Builder;
import lombok.Data;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity; /**
 * Result of duplicate validation check
 */
@Data
@Builder
public class DuplicateValidationResult {
    private boolean isAllowed;
    private boolean isWarning;
    private boolean isBlocked;
    private EventEntity duplicateEvent;
    private int similarityScore;
    private String message;

    /**
     * No duplicate found - event is allowed
     */
    public static DuplicateValidationResult allowed() {
        return DuplicateValidationResult.builder()
                .isAllowed(true)
                .isWarning(false)
                .isBlocked(false)
                .build();
    }

    /**
     * Similar event found - show warning but allow
     */
    public static DuplicateValidationResult warning(
            EventEntity event,
            int score,
            String message) {
        return DuplicateValidationResult.builder()
                .isAllowed(true)
                .isWarning(true)
                .isBlocked(false)
                .duplicateEvent(event)
                .similarityScore(score)
                .message(message)
                .build();
    }

    /**
     * Duplicate found - block creation
     */
    public static DuplicateValidationResult blocked(
            EventEntity event,
            int score,
            String message) {
        return DuplicateValidationResult.builder()
                .isAllowed(false)
                .isWarning(false)
                .isBlocked(true)
                .duplicateEvent(event)
                .similarityScore(score)
                .message(message)
                .build();
    }
}
