package org.nextgate.nextgatebackend.e_social.posts_mng.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostRepostRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostRepostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostVisibility;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.TimelineItemType;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PostResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.TimelineItemResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PostRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.FeedService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper.PostResponseMapper;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.entity.FollowEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.enums.FollowStatus;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;
    private final PostRepostRepository postRepostRepository;
    private final FollowRepository followRepository;
    private final AccountRepo accountRepo;
    private final PostResponseMapper postResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getUserTimeline(Pageable pageable) {
        AccountEntity currentUser = getAuthenticatedAccountOrNull();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;


        // Get user's original posts
        List<PostEntity> userPosts = postRepository.findByAuthorIdAndIsDeletedFalseAndStatus(
                currentUserId,
                PostStatus.PUBLISHED
        );

        // Get user's reposts
        List<PostRepostEntity> userReposts = postRepostRepository.findAllByUserId(
                currentUserId
        );

        // Combine and create timeline items
        List<TimelineItemResponse> timelineItems = new ArrayList<>();

        // Add original posts
        for (PostEntity post : userPosts) {
            TimelineItemResponse item = new TimelineItemResponse();
            item.setType(TimelineItemType.POST);
            item.setPost(postResponseMapper.toPostResponse(post));
            item.setRepostMetadata(null);
            timelineItems.add(item);
        }

        // Add reposts
        for (PostRepostEntity repost : userReposts) {
            PostEntity originalPost = postRepository.findByIdAndIsDeletedFalse(repost.getPostId()).orElse(null);
            if (originalPost != null && originalPost.getStatus() == PostStatus.PUBLISHED) {
                TimelineItemResponse item = createRepostItem(repost, originalPost, currentUserId);
                timelineItems.add(item);
            }
        }

        // Sort by created/reposted time
        timelineItems.sort((a, b) -> {
            Date timeA = a.getType() == TimelineItemType.POST
                    ? Date.from(a.getPost().getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    : Date.from(a.getRepostMetadata().getRepostedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            Date timeB = b.getType() == TimelineItemType.POST
                    ? Date.from(b.getPost().getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    : Date.from(b.getRepostMetadata().getRepostedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            return timeB.compareTo(timeA); // Descending
        });

        // Paginate
        return paginateList(timelineItems, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getFollowingFeed(Pageable pageable) {
        AccountEntity currentUser = getAuthenticatedAccount();

        // Get list of users the current user follows
        List<UUID> followingIds = followRepository.findByFollowerIdAndStatus(
                        currentUser.getId(),
                        FollowStatus.ACCEPTED
                ).stream()
                .map(FollowEntity::getFollowingId)
                .collect(Collectors.toList());

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Get posts by followed users
        List<PostEntity> followingPosts = postRepository.findByAuthorIdInAndIsDeletedFalseAndStatusOrderByCreatedAtDesc(
                followingIds,
                PostStatus.PUBLISHED
        );

        // Get reposts by followed users
        List<PostRepostEntity> followingReposts = postRepostRepository.findByUserIdIn(followingIds);

        // Combine and create timeline items
        List<TimelineItemResponse> timelineItems = new ArrayList<>();

        // Add posts
        for (PostEntity post : followingPosts) {
            // Check visibility
            if (canViewPost(post, currentUser)) {
                TimelineItemResponse item = new TimelineItemResponse();
                item.setType(TimelineItemType.POST);
                item.setPost(postResponseMapper.toPostResponse(post));
                item.setRepostMetadata(null);
                timelineItems.add(item);
            }
        }

        // Add reposts
        for (PostRepostEntity repost : followingReposts) {
            PostEntity originalPost = postRepository.findByIdAndIsDeletedFalse(repost.getPostId()).orElse(null);
            if (originalPost != null && originalPost.getStatus() == PostStatus.PUBLISHED && canViewPost(originalPost, currentUser)) {
                TimelineItemResponse item = createRepostItem(repost, originalPost, currentUser.getId());
                timelineItems.add(item);
            }
        }

        // Sort by time (newest first)
        timelineItems.sort((a, b) -> {
            Date timeA = a.getType() == TimelineItemType.POST
                    ? Date.from(a.getPost().getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    : Date.from(a.getRepostMetadata().getRepostedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            Date timeB = b.getType() == TimelineItemType.POST
                    ? Date.from(b.getPost().getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())
                    : Date.from(b.getRepostMetadata().getRepostedAt().atZone(java.time.ZoneId.systemDefault()).toInstant());
            return timeB.compareTo(timeA);
        });

        return paginateList(timelineItems, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TimelineItemResponse> getExploreFeed(Pageable pageable) {
        AccountEntity currentUser = getAuthenticatedAccountOrNull();
        UUID currentUserId = currentUser != null ? currentUser.getId() : null;

        // Get public posts only
        List<PostEntity> publicPosts = postRepository.findByIsDeletedFalseAndStatusAndVisibilityOrderByCreatedAtDesc(
                PostStatus.PUBLISHED,
                PostVisibility.PUBLIC
        );

        List<TimelineItemResponse> timelineItems = new ArrayList<>();

        for (PostEntity post : publicPosts) {
            TimelineItemResponse item = new TimelineItemResponse();
            item.setType(TimelineItemType.POST);
            item.setPost(postResponseMapper.toPostResponse(post));
            item.setRepostMetadata(null);
            timelineItems.add(item);
        }

        return paginateList(timelineItems, pageable);
    }

    private TimelineItemResponse createRepostItem(PostRepostEntity repost, PostEntity originalPost, UUID currentUserId) {
        TimelineItemResponse item = new TimelineItemResponse();
        item.setType(TimelineItemType.REPOST);
        item.setPost(postResponseMapper.toPostResponse(originalPost));

        // Build repost metadata
        TimelineItemResponse.RepostMetadata metadata = new TimelineItemResponse.RepostMetadata();
        metadata.setRepostId(repost.getId());
        metadata.setReposterId(repost.getUserId());
        metadata.setRepostComment(repost.getComment());
        metadata.setRepostedAt(repost.getCreatedAt());

        accountRepo.findById(repost.getUserId()).ifPresent(reposter -> {
            metadata.setReposterUserName(reposter.getUserName());
            metadata.setReposterFirstName(reposter.getFirstName());
            metadata.setReposterLastName(reposter.getLastName());
            metadata.setReposterProfilePictureUrl(
                    reposter.getProfilePictureUrls() != null && !reposter.getProfilePictureUrls().isEmpty()
                            ? reposter.getProfilePictureUrls().getFirst()
                            : null
            );
            metadata.setReposterIsVerified(reposter.getIsVerified());
        });

        item.setRepostMetadata(metadata);
        return item;
    }

    private boolean canViewPost(PostEntity post, AccountEntity viewer) {
        PostVisibility visibility = post.getVisibility();

        return switch (visibility) {
            case PUBLIC -> true;
            case FOLLOWERS -> followRepository.existsByFollowerIdAndFollowingIdAndStatus(
                    viewer.getId(),
                    post.getAuthorId(),
                    FollowStatus.ACCEPTED
            );
            case MENTIONED ->
                // Check if viewer is mentioned in post
                    false; // Implement if needed

            case PRIVATE -> post.getAuthorId().equals(viewer.getId());
            default -> false;
        };
    }

    private Page<TimelineItemResponse> paginateList(List<TimelineItemResponse> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), items.size());

        if (start > items.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, items.size());
        }

        List<TimelineItemResponse> pageContent = items.subList(start, end);
        return new PageImpl<>(pageContent, pageable, items.size());
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