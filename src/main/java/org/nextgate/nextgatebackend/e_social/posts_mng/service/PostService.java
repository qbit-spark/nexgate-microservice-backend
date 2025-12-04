package org.nextgate.nextgatebackend.e_social.posts_mng.service;


import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.CreatePostRequest;


public interface PostService {

    // Create and manage posts
    PostEntity createPost(CreatePostRequest request);

    // PostEntity updatePost(UUID postId, UpdatePostRequest request);

    // void deletePost(UUID postId);

    // PostEntity publishPost(UUID postId);

    // PostEntity schedulePost(UUID postId, LocalDateTime scheduledAt);

    // PostEntity saveDraft(CreatePostRequest request);

    // Get posts
    // PostEntity getPostById(UUID postId);

    // Page<PostEntity> getPostsByAuthor(UUID authorId, Pageable pageable);

    // Page<PostEntity> getFeedPosts(Pageable pageable);

    // Page<PostEntity> getPublicPosts(Pageable pageable);

    // List<PostEntity> getDraftsByAuthor(UUID authorId);

    // List<PostEntity> getScheduledPostsByAuthor(UUID authorId);

    // Collaboration
    // void acceptCollaboration(UUID postId, UUID collaborationId);

    // void declineCollaboration(UUID postId, UUID collaborationId);

    // void removeCollaborator(UUID postId, UUID collaboratorId);

    // Poll operations
    // void voteOnPoll(UUID postId, List<UUID> optionIds);

    // void removeVote(UUID postId);

    // Statistics
    // long getPostsCount(UUID authorId);

    // long getDraftsCount(UUID authorId);
}