package org.nextgate.nextgatebackend.e_social.interactions.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.interactions.entity.PostBookmarkEntity;
import org.nextgate.nextgatebackend.e_social.interactions.entity.PostLikeEntity;
import org.nextgate.nextgatebackend.e_social.interactions.entity.PostViewEntity;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostBookmarkRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostLikeRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostRepostRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostViewRepository;
import org.nextgate.nextgatebackend.e_social.interactions.service.PostInteractionService;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostRepostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CollaboratorStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.RepostPermission;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepostRepository postRepostRepository;
    private final PostViewRepository postViewRepository;
    private final PostUserMentionRepository postUserMentionRepository;
    private final AccountRepo accountRepo;
    private final FollowRepository followRepository;
    private final PostCollaboratorRepository postCollaboratorRepository;

    @Override
    @Transactional
    public void likePost(UUID postId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only like published posts");
        }

        if (postLikeRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new IllegalArgumentException("You have already liked this post");
        }

        PostLikeEntity like = new PostLikeEntity();
        like.setPostId(postId);
        like.setUserId(user.getId());
        postLikeRepository.save(like);

        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void unlikePost(UUID postId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        PostLikeEntity like = postLikeRepository.findByPostIdAndUserId(postId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You have not liked this post"));

        postLikeRepository.delete(like);

        post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void bookmarkPost(UUID postId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only bookmark published posts");
        }

        if (postBookmarkRepository.existsByPostIdAndUserId(postId, user.getId())) {
            throw new IllegalArgumentException("You have already bookmarked this post");
        }

        PostBookmarkEntity bookmark = new PostBookmarkEntity();
        bookmark.setPostId(postId);
        bookmark.setUserId(user.getId());
        postBookmarkRepository.save(bookmark);

        post.setBookmarksCount(post.getBookmarksCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void unbookmarkPost(UUID postId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        PostBookmarkEntity bookmark = postBookmarkRepository.findByPostIdAndUserId(postId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You have not bookmarked this post"));

        postBookmarkRepository.delete(bookmark);

        post.setBookmarksCount(Math.max(0, post.getBookmarksCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void repostPost(UUID postId, String comment) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new IllegalArgumentException("Can only repost published posts");
        }

        // Check privacy settings for repost permission
        if (!canRepost(post, user)) {
            throw new IllegalArgumentException("You don't have permission to repost this post");
        }

        // Unlimited reposts allowed (like Twitter/X)
        PostRepostEntity repost = new PostRepostEntity();
        repost.setPostId(postId);
        repost.setUserId(user.getId());
        repost.setComment(comment);
        postRepostRepository.save(repost);

        post.setRepostsCount(post.getRepostsCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void unrepostPost(UUID postId) {
        AccountEntity user = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Find the most recent repost by this user
        PostRepostEntity repost = postRepostRepository.findByPostIdAndUserId(postId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("You have not reposted this post"));

        postRepostRepository.delete(repost);

        post.setRepostsCount(Math.max(0, post.getRepostsCount() - 1));
        postRepository.save(post);
    }

    @Override
    @Transactional
    public void recordView(UUID postId) {
        AccountEntity user = getAuthenticatedAccountOrNull();

        if (user == null) {
            return;
        }

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            return;
        }

        if (post.getAuthorId().equals(user.getId())) {
            return;
        }

        PostViewEntity view = postViewRepository.findByPostIdAndUserId(postId, user.getId())
                .orElse(null);

        if (view == null) {
            // First view
            view = new PostViewEntity();
            view.setPostId(postId);
            view.setUserId(user.getId());
            view.setViewCount(1);
            postViewRepository.save(view);

            post.setViewsCount(post.getViewsCount() + 1);
            postRepository.save(post);
        } else {
            // Subsequent view - update last viewed time and count
            view.setViewCount(view.getViewCount() + 1);
            view.setLastViewedAt(LocalDateTime.now());
            postViewRepository.save(view);
        }
    }

    private boolean canRepost(PostEntity post, AccountEntity user) {
        RepostPermission whoCanRepost = post.getWhoCanRepost();

        if (post.getAuthorId().equals(user.getId())) {
            return true;
        }

        if (post.isCollaborative()) {
            boolean isCollaborator = postCollaboratorRepository.existsByPostIdAndUserIdAndStatus(
                    post.getId(),
                    user.getId(),
                    CollaboratorStatus.ACCEPTED
            );
            if (isCollaborator) {
                return true;
            }
        }

        return switch (whoCanRepost) {
            case EVERYONE -> true;
            case FOLLOWERS ->
                // Check if the user follows the post-author
                    followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                            user.getId(),
                            post.getAuthorId(),
                            FollowStatus.ACCEPTED
                    );
            case DISABLED ->
                    false;
            default -> false;
        };
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

    private AccountEntity getAuthenticatedAccountOrNull() {
        try {
            return getAuthenticatedAccount();
        } catch (Exception e) {
            return null;
        }
    }
}