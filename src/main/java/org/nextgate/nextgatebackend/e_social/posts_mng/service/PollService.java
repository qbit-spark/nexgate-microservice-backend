package org.nextgate.nextgatebackend.e_social.posts_mng.service;


import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PollResultsResponse;

import java.util.List;
import java.util.UUID;

public interface PollService {

    void voteOnPoll(UUID postId, List<UUID> optionIds);

    void removeVote(UUID postId);

    PollResultsResponse getPollResults(UUID postId);

    boolean hasUserVoted(UUID postId, UUID userId);

    List<UUID> getUserVotedOptions(UUID postId, UUID userId);
}