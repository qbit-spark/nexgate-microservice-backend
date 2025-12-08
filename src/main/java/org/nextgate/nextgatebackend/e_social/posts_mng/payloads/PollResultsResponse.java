package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PollResultsResponse {

    private UUID pollId;
    private String title;
    private String description;
    private long totalVotes;
    private boolean allowMultipleVotes;
    private boolean isAnonymous;
    private boolean allowVoteChange;
    private LocalDateTime expiresAt;
    private boolean hasExpired;
    private boolean userHasVoted;
    private List<PollOptionResult> options = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PollOptionResult {
        private UUID optionId;
        private String optionText;
        private String optionImageUrl;
        private int optionOrder;
        private long votesCount;
        private double percentage;
        private boolean userVoted;
    }
}