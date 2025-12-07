package org.nextgate.nextgatebackend.e_social.posts_mng.service;


import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PostService {

    // Create and manage posts
    PostEntity createPost(CreatePostRequest request);

    PostEntity publishPost();

    // Attach to draft (discovery flow)
    PostEntity attachProductToDraft(UUID productId);

    PostEntity attachShopToDraft(UUID shopId);

    PostEntity attachEventToDraft(UUID eventId);

    PostEntity attachBuyTogetherGroupToDraft(UUID groupId);

    PostEntity attachInstallmentPlanToDraft(UUID planId);

    PostEntity getMyCurrentDraft();

    // Remove from draft
    PostEntity removeProductFromDraft(UUID productId);

    PostEntity removeShopFromDraft(UUID shopId);

    PostEntity removeEventFromDraft(UUID eventId);

    PostEntity removeBuyTogetherGroupFromDraft(UUID groupId);

    PostEntity removeInstallmentPlanFromDraft(UUID planId);

    void discardDraft();

    // Get posts
    PostEntity getPostById(UUID postId);

    Page<PostEntity> getPostsByAuthor(UUID authorId, Pageable pageable);

    Page<PostEntity> getPublishedPosts(Pageable pageable);


    List<PostEntity> getMyScheduledPosts();

    // Update draft
    PostEntity updateDraft(UpdateDraftRequest request);

    PostEntity updateDraftContent(String content);

    PostEntity addMediaToDraft(List<MediaRequest> media);

    PostEntity updateDraftPrivacySettings(PrivacySettingsRequest settings);

    // Delete post
    void deletePost(UUID postId);

    PostEntity updateDraftCollaboration(CollaborationRequest collaboration);

    // Collaboration
    PostEntity acceptCollaboration(UUID postId);

    PostEntity declineCollaboration(UUID postId);

    void removeCollaborator(UUID postId, UUID collaboratorId);

}