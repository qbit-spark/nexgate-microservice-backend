package org.nextgate.nextgatebackend.e_social.posts_mng.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.CreatePostRequest;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.MediaData;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PostService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.PostValidationUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final AccountRepo accountRepo;
    private final PostValidationUtil validationUtil;
    private final ObjectMapper objectMapper;

    // Attachment repositories
    private final ProductRepo productRepo;
    private final ShopRepo shopRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseRepo;
    private final InstallmentPlanRepo installmentPlanRepo;
    private final EventsRepo eventsRepo;

    // Post-related repositories
    private final PostProductRepository postProductRepository;
    private final PostShopRepository postShopRepository;
    private final PostBuyTogetherGroupRepository postBuyTogetherGroupRepository;
    private final PostInstallmentPlanRepository postInstallmentPlanRepository;
    private final PostEventRepository postEventRepository;
    private final PostCollaboratorRepository postCollaboratorRepository;

    @Override
    @Transactional
    public PostEntity createPost(CreatePostRequest request) {
        validationUtil.validateCreatePostRequest(request, false); // Lenient validation for draft

        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = new PostEntity();
        post.setAuthorId(author.getId());
        post.setContent(request.getContent());
        post.setPostType(request.getPostType());

        setPrivacySettings(post, request);
        setPostStatus(post, request);

        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            setMediaData(post, request);
        }

        PostEntity savedPost = postRepository.save(post);

        if (request.getAttachments() != null) {
            saveAttachments(savedPost, request);
        }

        if (request.getCollaboration() != null && request.getCollaboration().getIsCollaborative()) {
            saveCollaborators(savedPost, request);
        }

        return savedPost;
    }

    @Override
    @Transactional
    public PostEntity publishPost(UUID postId) {
        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only publish your own posts");
        }

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be published");
        }

        validationUtil.validateForPublish(post);

        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());

        return postRepository.save(post);
    }

    private void setPrivacySettings(PostEntity post, CreatePostRequest request) {
        if (request.getPrivacySettings() != null) {
            post.setVisibility(request.getPrivacySettings().getVisibility());
            post.setWhoCanComment(request.getPrivacySettings().getWhoCanComment());
            post.setWhoCanRepost(request.getPrivacySettings().getWhoCanRepost());
            post.setHideLikesCount(request.getPrivacySettings().getHideLikesCount());
            post.setHideCommentsCount(request.getPrivacySettings().getHideCommentsCount());
        }
    }

    private void setPostStatus(PostEntity post, CreatePostRequest request) {
        if (request.getScheduledAt() != null) {
            post.setStatus(PostStatus.SCHEDULED);
            post.setScheduledAt(request.getScheduledAt());
        } else {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
        }
    }

    private void setMediaData(PostEntity post, CreatePostRequest request) {
        try {
            List<MediaData> mediaDataList = new ArrayList<>();
            for (int i = 0; i < request.getMedia().size(); i++) {
                var mediaRequest = request.getMedia().get(i);
                MediaData mediaData = new MediaData();
                mediaData.setId(UUID.randomUUID().toString());
                mediaData.setMediaType(mediaRequest.getMediaType());
                mediaData.setOriginalUrl(mediaRequest.getMediaUrl());
                mediaData.setPlaceholderBase64(mediaRequest.getPlaceholderBase64());
                mediaData.setWidth(mediaRequest.getWidth());
                mediaData.setHeight(mediaRequest.getHeight());
                mediaData.setDuration(mediaRequest.getDuration());
                mediaData.setOrder(i + 1);
                mediaData.setImageTags(new ArrayList<>());
                mediaDataList.add(mediaData);
            }
            post.setMediaData(objectMapper.writeValueAsString(mediaDataList));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize media data", e);
        }
    }

    private void saveAttachments(PostEntity post, CreatePostRequest request) {
        var attachments = request.getAttachments();

        if (attachments.getProductIds() != null) {
            saveProductAttachments(post, attachments.getProductIds());
        }

        if (attachments.getShopIds() != null) {
            saveShopAttachments(post, attachments.getShopIds());
        }

        if (attachments.getBuyTogetherGroupIds() != null) {
            saveBuyTogetherGroupAttachments(post, attachments.getBuyTogetherGroupIds());
        }

        if (attachments.getInstallmentPlanIds() != null) {
            saveInstallmentPlanAttachments(post, attachments.getInstallmentPlanIds());
        }

        if (attachments.getEventIds() != null) {
            saveEventAttachments(post, attachments.getEventIds());
        }
    }

    private void saveProductAttachments(PostEntity post, List<UUID> productIds) {
        for (UUID productId : productIds) {
            if (!productRepo.existsById(productId)) {
                throw new IllegalArgumentException("Product not found: " + productId);
            }
            PostProductEntity postProduct = new PostProductEntity();
            postProduct.setPostId(post.getId());
            postProduct.setProductId(productId);
            postProductRepository.save(postProduct);
        }
    }

    private void saveShopAttachments(PostEntity post, List<UUID> shopIds) {
        for (UUID shopId : shopIds) {
            if (!shopRepo.existsById(shopId)) {
                throw new IllegalArgumentException("Shop not found: " + shopId);
            }
            PostShopEntity postShop = new PostShopEntity();
            postShop.setPostId(post.getId());
            postShop.setShopId(shopId);
            postShopRepository.save(postShop);
        }
    }

    private void saveBuyTogetherGroupAttachments(PostEntity post, List<UUID> groupIds) {
        for (UUID groupId : groupIds) {
            if (!groupPurchaseRepo.existsById(groupId)) {
                throw new IllegalArgumentException("Buy together group not found: " + groupId);
            }
            PostBuyTogetherGroupEntity postGroup = new PostBuyTogetherGroupEntity();
            postGroup.setPostId(post.getId());
            postGroup.setGroupId(groupId);
            postBuyTogetherGroupRepository.save(postGroup);
        }
    }

    private void saveInstallmentPlanAttachments(PostEntity post, List<UUID> planIds) {
        for (UUID planId : planIds) {
            if (!installmentPlanRepo.existsById(planId)) {
                throw new IllegalArgumentException("Installment plan not found: " + planId);
            }
            PostInstallmentPlanEntity postPlan = new PostInstallmentPlanEntity();
            postPlan.setPostId(post.getId());
            postPlan.setPlanId(planId);
            postInstallmentPlanRepository.save(postPlan);
        }
    }

    private void saveEventAttachments(PostEntity post, List<UUID> eventIds) {
        for (UUID eventId : eventIds) {
            if (!eventsRepo.existsById(eventId)) {
                throw new IllegalArgumentException("Event not found: " + eventId);
            }
            PostEventEntity postEvent = new PostEventEntity();
            postEvent.setPostId(post.getId());
            postEvent.setEventId(eventId);
            postEventRepository.save(postEvent);
        }
    }

    private void saveCollaborators(PostEntity post, CreatePostRequest request) {
        post.setCollaborative(true);

        for (UUID collaboratorId : request.getCollaboration().getCollaboratorIds()) {
            if (!accountRepo.existsById(collaboratorId)) {
                throw new IllegalArgumentException("Collaborator not found: " + collaboratorId);
            }

            if (collaboratorId.equals(post.getAuthorId())) {
                throw new IllegalArgumentException("Cannot add yourself as collaborator");
            }

            PostCollaboratorEntity collaborator = new PostCollaboratorEntity();
            collaborator.setPostId(post.getId());
            collaborator.setUserId(collaboratorId);
            postCollaboratorRepository.save(collaborator);
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