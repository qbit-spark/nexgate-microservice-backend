package org.nextgate.nextgatebackend.e_social.posts_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PostService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper.PostResponseMapper;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/e-social/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostResponseMapper postResponseMapper;

    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createPost(
            @Valid @RequestBody CreatePostRequest request) {

        PostEntity post = postService.createPost(request);
        PostResponse response = postResponseMapper.toPostResponse(post, null);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post created successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/publish")
    public ResponseEntity<GlobeSuccessResponseBuilder> publishPost(
            @PathVariable UUID postId,
            Authentication authentication) {

        PostEntity post = postService.publishPost(postId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(post, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post published successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deletePost(@PathVariable UUID postId) {

        postService.deletePost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post deleted successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPostById(
            @PathVariable UUID postId,
            Authentication authentication) {

        PostEntity post = postService.getPostById(postId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(post, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getPublishedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<PostEntity> postsPage = postService.getPublishedPosts(pageable);

        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        List<PostResponse> responses = postResponseMapper.toPostResponseList(postsPage.getContent(), currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Posts retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPostsByAuthor(
            @PathVariable UUID authorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<PostEntity> postsPage = postService.getPostsByAuthor(authorId, pageable);

        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        List<PostResponse> responses = postResponseMapper.toPostResponseList(postsPage.getContent(), currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Posts retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> getCurrentDraft(Authentication authentication) {

        PostEntity draft = postService.getCurrentDraft();

        if (draft == null) {
            GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                    "No draft found",
                    null
            );
            return ResponseEntity.ok(successResponse);
        }

        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> discardDraft() {

        postService.discardDraft();

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft discarded successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-product/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachProductToDraft(
            @PathVariable UUID productId,
            Authentication authentication) {

        PostEntity draft = postService.attachProductToDraft(productId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Product attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachShopToDraft(
            @PathVariable UUID shopId,
            Authentication authentication) {

        PostEntity draft = postService.attachShopToDraft(shopId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Shop attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachEventToDraft(
            @PathVariable UUID eventId,
            Authentication authentication) {

        PostEntity draft = postService.attachEventToDraft(eventId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Event attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-group/{groupId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachBuyTogetherGroupToDraft(
            @PathVariable UUID groupId,
            Authentication authentication) {

        PostEntity draft = postService.attachBuyTogetherGroupToDraft(groupId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Buy-together group attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-plan/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachInstallmentPlanToDraft(
            @PathVariable UUID planId,
            Authentication authentication) {

        PostEntity draft = postService.attachInstallmentPlanToDraft(planId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Installment plan attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-product/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeProductFromDraft(
            @PathVariable UUID productId,
            Authentication authentication) {

        PostEntity draft = postService.removeProductFromDraft(productId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Product removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeShopFromDraft(
            @PathVariable UUID shopId,
            Authentication authentication) {

        PostEntity draft = postService.removeShopFromDraft(shopId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Shop removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeEventFromDraft(
            @PathVariable UUID eventId,
            Authentication authentication) {

        PostEntity draft = postService.removeEventFromDraft(eventId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Event removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-group/{groupId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeBuyTogetherGroupFromDraft(
            @PathVariable UUID groupId,
            Authentication authentication) {

        PostEntity draft = postService.removeBuyTogetherGroupFromDraft(groupId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Buy-together group removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-plan/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeInstallmentPlanFromDraft(
            @PathVariable UUID planId,
            Authentication authentication) {

        PostEntity draft = postService.removeInstallmentPlanFromDraft(planId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Installment plan removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraft(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdateDraftRequest request,
            Authentication authentication) {

        PostEntity draft = postService.updateDraft(postId, request);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/{postId}/content")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraftContent(
            @PathVariable UUID postId,
            @RequestBody String content,
            Authentication authentication) {

        PostEntity draft = postService.updateDraftContent(postId, content);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft content updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/{postId}/media")
    public ResponseEntity<GlobeSuccessResponseBuilder> addMediaToDraft(
            @PathVariable UUID postId,
            @Valid @RequestBody List<MediaRequest> media,
            Authentication authentication) {

        PostEntity draft = postService.addMediaToDraft(postId, media);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Media added to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/{postId}/privacy")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraftPrivacySettings(
            @PathVariable UUID postId,
            @Valid @RequestBody PrivacySettingsRequest settings,
            Authentication authentication) {

        PostEntity draft = postService.updateDraftPrivacySettings(postId, settings);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(draft, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Privacy settings updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/collaboration/accept")
    public ResponseEntity<GlobeSuccessResponseBuilder> acceptCollaboration(
            @PathVariable UUID postId,
            Authentication authentication) {

        PostEntity post = postService.acceptCollaboration(postId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(post, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Collaboration accepted successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/collaboration/decline")
    public ResponseEntity<GlobeSuccessResponseBuilder> declineCollaboration(
            @PathVariable UUID postId,
            Authentication authentication) {

        PostEntity post = postService.declineCollaboration(postId);
        UUID currentUserId = authentication != null ? UUID.fromString(authentication.getName()) : null;
        PostResponse response = postResponseMapper.toPostResponse(post, currentUserId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Collaboration declined successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{postId}/collaborators/{collaboratorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeCollaborator(
            @PathVariable UUID postId,
            @PathVariable UUID collaboratorId) {

        postService.removeCollaborator(postId, collaboratorId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Collaborator removed successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }
}