package org.nextgate.nextgatebackend.e_social.interactions.service;

import java.util.UUID;

public interface PostInteractionService {

    // Likes
    void likePost(UUID postId);
    void unlikePost(UUID postId);

    // Bookmarks
    void bookmarkPost(UUID postId);
    void unbookmarkPost(UUID postId);

    // Reposts
    void repostPost(UUID postId, String comment);
    void unrepostPost(UUID postId);

    // Views
    void recordView(UUID postId);
}