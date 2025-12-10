package org.nextgate.nextgatebackend.e_social.posts_mng.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.e_social.interactions.service.PostInteractionService;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCommentEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostVisibility;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.CommentService;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.FeedService;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PollService;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PostService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper.CommentResponseMapper;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper.PostResponseMapper;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo.BlockRepository;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/e-social/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostResponseMapper postResponseMapper;
    private final PollService pollService;
    private final PostInteractionService interactionService;
    private final CommentService commentService;
    private final CommentResponseMapper commentResponseMapper;
    private final FeedService feedService;


    @PostMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> createPost(
            @Valid @RequestBody CreatePostRequest request) {

        PostEntity post = postService.createPost(request);
        PostResponse response = postResponseMapper.toPostResponse(post);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post created successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/publish")
    public ResponseEntity<GlobeSuccessResponseBuilder> publishDraftPost() {

        PostEntity post = postService.publishPost();

        PostResponse response = postResponseMapper.toPostResponse(post);

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
                "Post deleted successfully"
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPostById(
            @PathVariable UUID postId) {

        PostEntity post = postService.getPostById(postId);

        PostResponse response = postResponseMapper.toPostResponse(post);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    //Todo: This needs a lot of optimisation for now its fine for development
    @GetMapping
    public ResponseEntity<GlobeSuccessResponseBuilder> getPublishedPosts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<PostEntity> postsPage = postService.getPublishedPosts(pageable);
        List<PostResponse> responses = postResponseMapper.toPostResponseList(postsPage.getContent());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Posts retrieved successfully",
                responses
        );
    
        return ResponseEntity.ok(successResponse);
    }

    //Todo: This needs a lot of optimisation for now its fine for development
    @GetMapping("/author/{authorId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPostsByAuthor(
            @PathVariable UUID authorId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<PostEntity> postsPage = postService.getPostsByAuthor(authorId, pageable);

        List<PostResponse> responses = postResponseMapper.toPostResponseList(postsPage.getContent());
        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Posts retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/scheduled")
    public ResponseEntity<GlobeSuccessResponseBuilder> getScheduledPosts() {

        List<PostEntity> postsPage = postService.getMyScheduledPosts();

        List<PostResponse> responses = postResponseMapper.toPostResponseList(postsPage);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Scheduled posts retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);

    }

    @GetMapping("/draft")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyCurrentDraft() {

        PostEntity draft = postService.getMyCurrentDraft();

        PostResponse response = postResponseMapper.toPostResponse(draft);

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
                "Draft discarded successfully",null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-product/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachProductToDraft(
            @PathVariable UUID productId) {

        PostEntity draft = postService.attachProductToDraft(productId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Product attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachShopToDraft(
            @PathVariable UUID shopId) {

        PostEntity draft = postService.attachShopToDraft(shopId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Shop attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachEventToDraft(
            @PathVariable UUID eventId) {

        PostEntity draft = postService.attachEventToDraft(eventId);

       // PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Event attached to draft successfully",
                "Event attached to draft successfully"
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-group/{groupId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachBuyTogetherGroupToDraft(
            @PathVariable UUID groupId) {

        PostEntity draft = postService.attachBuyTogetherGroupToDraft(groupId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Buy-together group attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/draft/attach-plan/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> attachInstallmentPlanToDraft(
            @PathVariable UUID planId) {

        PostEntity draft = postService.attachInstallmentPlanToDraft(planId);
        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Installment plan attached to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-product/{productId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeProductFromDraft(
            @PathVariable UUID productId) {

        PostEntity draft = postService.removeProductFromDraft(productId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Product removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-shop/{shopId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeShopFromDraft(
            @PathVariable UUID shopId) {

        PostEntity draft = postService.removeShopFromDraft(shopId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Shop removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-event/{eventId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeEventFromDraft(
            @PathVariable UUID eventId) {

        PostEntity draft = postService.removeEventFromDraft(eventId);
        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Event removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-group/{groupId}")
        public ResponseEntity<GlobeSuccessResponseBuilder> removeBuyTogetherGroupFromDraft(
            @PathVariable UUID groupId) {

        PostEntity draft = postService.removeBuyTogetherGroupFromDraft(groupId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Buy-together group removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/draft/remove-plan/{planId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeInstallmentPlanFromDraft(
            @PathVariable UUID planId) {

        PostEntity draft = postService.removeInstallmentPlanFromDraft(planId);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Installment plan removed from draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping()
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraft(
            @Valid @RequestBody UpdateDraftRequest request) {

        PostEntity draft = postService.updateDraft(request);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/content")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraftContent(
            @RequestBody String content) {

        PostEntity draft = postService.updateDraftContent(content);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Draft content updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/media")
    public ResponseEntity<GlobeSuccessResponseBuilder> addMediaToDraft(
            @Valid @RequestBody List<MediaRequest> media) {

        PostEntity draft = postService.addMediaToDraft( media);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Media added to draft successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/privacy")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraftPrivacySettings(
            @Valid @RequestBody PrivacySettingsRequest settings) {

        PostEntity draft = postService.updateDraftPrivacySettings(settings);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Privacy settings updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/collaboration")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateDraftCollaboration(
            @Valid @RequestBody CollaborationRequest collaboration) {

        PostEntity draft = postService.updateDraftCollaboration(collaboration);

        PostResponse response = postResponseMapper.toPostResponse(draft);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Collaboration settings updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/collaboration/accept")
    public ResponseEntity<GlobeSuccessResponseBuilder> acceptCollaboration(
            @PathVariable UUID postId) {

        PostEntity post = postService.acceptCollaboration(postId);

        PostResponse response = postResponseMapper.toPostResponse(post);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Collaboration accepted successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/collaboration/decline")
    public ResponseEntity<GlobeSuccessResponseBuilder> declineCollaboration(
            @PathVariable UUID postId) {

        PostEntity post = postService.declineCollaboration(postId);

        PostResponse response = postResponseMapper.toPostResponse(post);

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
                "Collaborator removed successfully"
        );

        return ResponseEntity.ok(successResponse);
    }

      // ============================================
      // POLL ENDPOINTS
     // ============================================

    @PostMapping("/{postId}/vote")
    public ResponseEntity<GlobeSuccessResponseBuilder> voteOnPoll(
            @PathVariable UUID postId,
            @Valid @RequestBody VotePollRequest request) {

            pollService.voteOnPoll(postId, request.getOptionIds());

            GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                    "Vote recorded successfully",
                    "Vote recorded successfully"
            );

            return ResponseEntity.ok(successResponse);

    }

    @DeleteMapping("/{postId}/vote")
    public ResponseEntity<GlobeSuccessResponseBuilder> removeVote(@PathVariable UUID postId) {

        pollService.removeVote(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Vote removed successfully",
                "Vote removed successfully"
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{postId}/poll-results")
    public ResponseEntity<GlobeSuccessResponseBuilder> getPollResults(@PathVariable UUID postId) {

        PollResultsResponse results = pollService.getPollResults(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Poll results retrieved successfully",
                results
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{postId}/poll-voters/{optionId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getOptionVoters(
            @PathVariable UUID postId,
            @PathVariable UUID optionId) {

        List<VoterInfo> voters = pollService.getOptionVoters(postId, optionId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Voters retrieved successfully",
                voters
        );

        return ResponseEntity.ok(successResponse);
    }

    // ============================================
   // INTERACTION ENDPOINTS
  // ============================================

    @PostMapping("/{postId}/like")
    public ResponseEntity<GlobeSuccessResponseBuilder> likePost(@PathVariable UUID postId) {

        interactionService.likePost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post liked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{postId}/like")
    public ResponseEntity<GlobeSuccessResponseBuilder> unlikePost(@PathVariable UUID postId) {

        interactionService.unlikePost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post unliked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<GlobeSuccessResponseBuilder> bookmarkPost(@PathVariable UUID postId) {

        interactionService.bookmarkPost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post bookmarked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{postId}/bookmark")
    public ResponseEntity<GlobeSuccessResponseBuilder> unbookmarkPost(@PathVariable UUID postId) {

        interactionService.unbookmarkPost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Bookmark removed successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyBookmarks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PostResponse> bookmarks = interactionService.getMyBookmarks(page - 1, size);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Bookmarks retrieved successfully",
                bookmarks
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/repost")
    public ResponseEntity<GlobeSuccessResponseBuilder> repostPost(
            @PathVariable UUID postId,
            @RequestBody(required = false) RepostRequest request) {

        String comment = request != null ? request.getComment() : null;
        interactionService.repostPost(postId, comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Post reposted successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/{postId}/repost")
    public ResponseEntity<GlobeSuccessResponseBuilder> unrepostPost(@PathVariable UUID postId) {

        interactionService.unrepostPost(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Repost removed successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/my-reposts")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyReposts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PostResponse> reposts = interactionService.getMyReposts(page - 1, size);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Reposts retrieved successfully",
                reposts
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/users/{userId}/reposts")
    public ResponseEntity<GlobeSuccessResponseBuilder> getUserReposts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<PostResponse> reposts = interactionService.getUserReposts(userId, page - 1, size);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "User reposts retrieved successfully",
                reposts
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/view")
    public ResponseEntity<GlobeSuccessResponseBuilder> recordView(@PathVariable UUID postId) {

        interactionService.recordView(postId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "View recorded successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

     // ============================================
    // COMMENT ENDPOINTS
    // ============================================

    @PostMapping("/{postId}/comments")
    public ResponseEntity<GlobeSuccessResponseBuilder> createComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateCommentRequest request) {

        PostCommentEntity comment = commentService.createComment(postId, request);
        CommentResponse response = commentResponseMapper.toCommentResponse(comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment created successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<GlobeSuccessResponseBuilder> getComments(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<PostCommentEntity> commentsPage = commentService.getComments(postId, pageable);

        List<CommentResponse> responses = commentResponseMapper.toCommentResponseList(commentsPage.getContent());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comments retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getComment(@PathVariable UUID commentId) {

        PostCommentEntity comment = commentService.getCommentById(commentId);
        CommentResponse response = commentResponseMapper.toCommentResponse(comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment retrieved successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<GlobeSuccessResponseBuilder> getReplies(
            @PathVariable UUID commentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").ascending());
        Page<PostCommentEntity> repliesPage = commentService.getReplies(commentId, pageable);

        List<CommentResponse> responses = commentResponseMapper.toCommentResponseList(repliesPage.getContent());

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Replies retrieved successfully",
                responses
        );

        return ResponseEntity.ok(successResponse);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> updateComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request) {

        PostCommentEntity comment = commentService.updateComment(commentId, request);
        CommentResponse response = commentResponseMapper.toCommentResponse(comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment updated successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> deleteComment(@PathVariable UUID commentId) {

        commentService.deleteComment(commentId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment deleted successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/{postId}/comments/{commentId}/pin")
    public ResponseEntity<GlobeSuccessResponseBuilder> pinComment(
            @PathVariable UUID postId,
            @PathVariable UUID commentId) {

        PostCommentEntity comment = commentService.pinComment(postId, commentId);
        CommentResponse response = commentResponseMapper.toCommentResponse(comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment pinned successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/comments/{commentId}/pin")
    public ResponseEntity<GlobeSuccessResponseBuilder> unpinComment(@PathVariable UUID commentId) {

        PostCommentEntity comment = commentService.unpinComment(commentId);
        CommentResponse response = commentResponseMapper.toCommentResponse(comment);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment unpinned successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<GlobeSuccessResponseBuilder> likeComment(@PathVariable UUID commentId) {

        commentService.likeComment(commentId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment liked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<GlobeSuccessResponseBuilder> unlikeComment(@PathVariable UUID commentId) {

        commentService.unlikeComment(commentId);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Comment unliked successfully",
                null
        );

        return ResponseEntity.ok(successResponse);
    }

    // ============================================
    // FEED/TIMELINE ENDPOINTS HERE....
    // ============================================


    @PostMapping("/quote/{quotedPostId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> createQuotePost(
            @PathVariable UUID quotedPostId,
            @Valid @RequestBody CreateQuotePostRequest request) {

        PostEntity post = postService.createQuotePost(quotedPostId, request);
        PostResponse response = postResponseMapper.toPostResponse(post);

        GlobeSuccessResponseBuilder successResponse = GlobeSuccessResponseBuilder.success(
                "Quote post created successfully",
                response
        );

        return ResponseEntity.ok(successResponse);
    }

}