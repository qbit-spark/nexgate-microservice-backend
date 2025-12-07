package org.nextgate.nextgatebackend.e_social.posts_mng.service;

import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.TimelineItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FeedService {

    // User's own timeline (profile view)
    Page<TimelineItemResponse> getUserTimeline(UUID userId, Pageable pageable);

    // Following feed (home feed)
    Page<TimelineItemResponse> getFollowingFeed(Pageable pageable);

    // Explore/public feed
    Page<TimelineItemResponse> getExploreFeed(Pageable pageable);
}