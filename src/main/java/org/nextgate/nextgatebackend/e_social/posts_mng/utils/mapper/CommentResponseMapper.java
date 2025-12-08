package org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.CommentMentionEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCommentEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.CommentResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.CommentLikeRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.CommentMentionRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PostRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommentResponseMapper {

    private final AccountRepo accountRepo;
    private final PostRepository postRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentResponse toCommentResponse(PostCommentEntity comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setParentCommentId(comment.getParentCommentId());
        response.setAuthor(mapAuthor(comment));
        response.setContent(comment.getContent());
        response.setContentParsed(mapContentParsed(comment));
        response.setLikesCount(comment.getLikesCount());
        response.setRepliesCount(comment.getRepliesCount());
        response.setPinned(comment.isPinned());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());

        AccountEntity currentUser = getAuthenticatedAccountOrNull();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        response.setHasLiked(mapHasLiked(comment, currentUserId));
        response.setCanEdit(mapCanEdit(comment, currentUserId));
        response.setCanDelete(mapCanDelete(comment, currentUserId));
        response.setCanPin(mapCanPin(comment, currentUserId));

        return response;
    }

    public List<CommentResponse> toCommentResponseList(List<PostCommentEntity> comments) {
        List<CommentResponse> responses = new ArrayList<>();
        for (PostCommentEntity comment : comments) {
            responses.add(toCommentResponse(comment));
        }
        return responses;
    }

    private CommentResponse.Author mapAuthor(PostCommentEntity comment) {
        CommentResponse.Author author = new CommentResponse.Author();
        author.setId(comment.getUserId());

        accountRepo.findById(comment.getUserId()).ifPresent(account -> {
            author.setUserName(account.getUserName());
            author.setFirstName(account.getFirstName());
            author.setLastName(account.getLastName());
            author.setProfilePictureUrl(
                    account.getProfilePictureUrls() != null && !account.getProfilePictureUrls().isEmpty()
                            ? account.getProfilePictureUrls().getFirst()
                            : null
            );
            author.setVerified(account.getIsVerified());
        });

        return author;
    }

    private CommentResponse.ContentParsed mapContentParsed(PostCommentEntity comment) {
        CommentResponse.ContentParsed parsed = new CommentResponse.ContentParsed();
        parsed.setText(comment.getContent());

        List<CommentResponse.MentionEntity> mentions = new ArrayList<>();
        List<CommentMentionEntity> commentMentions = commentMentionRepository.findByCommentId(comment.getId());

        for (CommentMentionEntity mention : commentMentions) {
            accountRepo.findById(mention.getMentionedUserId()).ifPresent(user -> {
                CommentResponse.MentionEntity mentionEntity = new CommentResponse.MentionEntity();
                mentionEntity.setUserId(user.getId());
                mentionEntity.setUserName(user.getUserName());
                mentionEntity.setDisplayName(user.getFirstName() + " " + user.getLastName());
                mentionEntity.setStartIndex(mention.getStartIndex());
                mentionEntity.setEndIndex(mention.getEndIndex());
                mentions.add(mentionEntity);
            });
        }

        parsed.setMentions(mentions);
        return parsed;
    }

    private boolean mapHasLiked(PostCommentEntity comment, UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
    }

    private boolean mapCanEdit(PostCommentEntity comment, UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        // Only comment owner can edit
        return comment.getUserId().equals(currentUserId);
    }

    private boolean mapCanDelete(PostCommentEntity comment, UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        // Comment owner can delete
        if (comment.getUserId().equals(currentUserId)) {
            return true;
        }

        // Post author can delete any comment on their post
        PostEntity post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post != null && post.getAuthorId().equals(currentUserId)) {
            return true;
        }

        return false;
    }

    private boolean mapCanPin(PostCommentEntity comment, UUID currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        // Only post author can pin comments
        PostEntity post = postRepository.findById(comment.getPostId()).orElse(null);
        if (post == null) {
            return false;
        }

        // Must be post author AND top-level comment
        return post.getAuthorId().equals(currentUserId) && comment.getParentCommentId() == null;
    }

    private AccountEntity getAuthenticatedAccountOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String userName = userDetails.getUsername();
                return accountRepo.findByUserName(userName).orElse(null);
            }
        } catch (Exception e) {
            // Anonymous user
        }
        return null;
    }
}