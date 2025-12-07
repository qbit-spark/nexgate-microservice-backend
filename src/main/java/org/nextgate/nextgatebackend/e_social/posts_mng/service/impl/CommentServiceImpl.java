package org.nextgate.nextgatebackend.e_social.posts_mng.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.CommentLikeEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.CommentMentionEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCommentEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CommentPermission;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.CreateCommentRequest;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.UpdateCommentRequest;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.CommentService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.ContentParsingUtil;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostCommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final PostRepository postRepository;
    private final PostUserMentionRepository postUserMentionRepository;
    private final AccountRepo accountRepo;
    private final FollowRepository followRepository;
    private final ContentParsingUtil contentParsingUtil;

    @Override
    @Transactional
    public PostCommentEntity createComment(UUID postId, CreateCommentRequest request) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only comment on published posts");
        }

        // Check comment permission
        if (!canComment(post, user)) {
            throw new IllegalArgumentException("You don't have permission to comment on this post");
        }

        // If replying to a comment, a validated parent exists
        if (request.getParentCommentId() != null) {
            PostCommentEntity parentComment = commentRepository.findByIdAndIsDeletedFalse(request.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));

            if (!parentComment.getPostId().equals(postId)) {
                throw new IllegalArgumentException("Parent comment does not belong to this post");
            }
        }

        PostCommentEntity comment = new PostCommentEntity();
        comment.setPostId(postId);
        comment.setUserId(user.getId());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setContent(request.getContent());

        PostCommentEntity savedComment = commentRepository.save(comment);

        // Parse and save mentions
        parseAndSaveMentions(savedComment, request.getContent());

        // Update post-comments count (only for top-level comments)
        if (request.getParentCommentId() == null) {
            post.setCommentsCount(post.getCommentsCount() + 1);
            postRepository.save(post);
        } else {
            // Update parent comment replies count
            PostCommentEntity parentComment = commentRepository.findById(request.getParentCommentId()).get();
            parentComment.setRepliesCount(parentComment.getRepliesCount() + 1);
            commentRepository.save(parentComment);
        }

        return savedComment;
    }

    @Override
    @Transactional
    public PostCommentEntity updateComment(UUID commentId, UpdateCommentRequest request) {
        AccountEntity user = getAuthenticatedAccount();

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only edit your own comments");
        }

        // Delete old mentions
        commentMentionRepository.deleteByCommentId(commentId);

        // Update content
        comment.setContent(request.getContent());
        PostCommentEntity updatedComment = commentRepository.save(comment);

        // Parse and save new mentions
        parseAndSaveMentions(updatedComment, request.getContent());

        return updatedComment;
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        AccountEntity user = getAuthenticatedAccount();

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        PostEntity post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Can delete if: owner of comment OR post-author
        if (!comment.getUserId().equals(user.getId()) && !post.getAuthorId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't have permission to delete this comment");
        }

        // Soft delete
        comment.setDeleted(true);
        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);

        // Update counters
        if (comment.getParentCommentId() == null) {
            // Top-level comment
            post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
            postRepository.save(post);
        } else {
            // Reply
            PostCommentEntity parentComment = commentRepository.findById(comment.getParentCommentId()).orElse(null);
            if (parentComment != null) {
                parentComment.setRepliesCount(Math.max(0, parentComment.getRepliesCount() - 1));
                commentRepository.save(parentComment);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentEntity> getComments(UUID postId, Pageable pageable) {
        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        return commentRepository.findByPostIdAndParentCommentIdIsNullAndIsDeletedFalse(postId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostCommentEntity> getReplies(UUID commentId, Pageable pageable) {
        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        return commentRepository.findByParentCommentIdAndIsDeletedFalse(commentId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostCommentEntity getCommentById(UUID commentId) {
        return commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
    }

    @Override
    @Transactional
    public PostCommentEntity pinComment(UUID postId, UUID commentId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new IllegalArgumentException("Only post author can pin comments");
        }

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getPostId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not belong to this post");
        }

        if (comment.getParentCommentId() != null) {
            throw new IllegalArgumentException("Cannot pin replies, only top-level comments");
        }

        comment.setPinned(true);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public PostCommentEntity unpinComment(UUID commentId) {
        AccountEntity user = getAuthenticatedAccount();

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        PostEntity post = postRepository.findById(comment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(user.getId())) {
            throw new IllegalArgumentException("Only post author can unpin comments");
        }

        comment.setPinned(false);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void likeComment(UUID commentId) {
        AccountEntity user = getAuthenticatedAccount();

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, user.getId())) {
            throw new IllegalArgumentException("You have already liked this comment");
        }

        CommentLikeEntity like = new CommentLikeEntity();
        like.setCommentId(commentId);
        like.setUserId(user.getId());
        commentLikeRepository.save(like);

        comment.setLikesCount(comment.getLikesCount() + 1);
        commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void unlikeComment(UUID commentId) {
        AccountEntity user = getAuthenticatedAccount();

        PostCommentEntity comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        CommentLikeEntity like = commentLikeRepository.findByCommentIdAndUserId(commentId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You have not liked this comment"));

        commentLikeRepository.delete(like);

        comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        commentRepository.save(comment);
    }

    // Privacy check for commenting
    private boolean canComment(PostEntity post, AccountEntity user) {
        CommentPermission whoCanComment = post.getWhoCanComment();

        // Author can always comment
        if (post.getAuthorId().equals(user.getId())) {
            return true;
        }

        return switch (whoCanComment) {
            case EVERYONE -> true;
            case FOLLOWERS -> followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                    user.getId(),
                    post.getAuthorId(),
                    FollowStatus.ACCEPTED
            );
            case MENTIONED -> postUserMentionRepository.existsByPostIdAndMentionedUserId(
                    post.getId(),
                    user.getId()
            );
            case DISABLED -> false;
            default -> false;
        };
    }

    private void parseAndSaveMentions(PostCommentEntity comment, String content) {
        List<ContentParsingUtil.ParsedMention> mentions = contentParsingUtil.parseMentions(content);

        for (ContentParsingUtil.ParsedMention mention : mentions) {
            accountRepo.findByUserName(mention.getUserName()).ifPresent(user -> {
                CommentMentionEntity commentMention = new CommentMentionEntity();
                commentMention.setCommentId(comment.getId());
                commentMention.setMentionedUserId(user.getId());
                commentMention.setStartIndex(mention.getStartIndex());
                commentMention.setEndIndex(mention.getEndIndex());
                commentMentionRepository.save(commentMention);
            });
        }
    }

    private AccountEntity getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}