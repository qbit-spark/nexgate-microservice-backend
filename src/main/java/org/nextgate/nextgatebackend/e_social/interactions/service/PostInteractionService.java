package org.nextgate.nextgatebackend.e_social.interactions.service;

import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PostResponse;
import org.springframework.data.domain.Page;

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

    Page<PostResponse> getMyBookmarks(int page, int size);

    Page<PostResponse> getMyReposts(int page, int size);

    Page<PostResponse> getUserReposts(UUID userId, int page, int size);
}