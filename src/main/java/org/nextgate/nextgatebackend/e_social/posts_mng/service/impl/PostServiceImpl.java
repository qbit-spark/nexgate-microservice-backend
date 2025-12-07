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
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CollaboratorStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PostService;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.ContentParsingUtil;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.LinkProcessingUtil;
import org.nextgate.nextgatebackend.e_social.posts_mng.utils.PostValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ContentParsingUtil contentParsingUtil;
    private final LinkProcessingUtil linkProcessingUtil;
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
    private final PostUserMentionRepository postUserMentionRepository;
    private final PostShopMentionRepository postShopMentionRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostLinkRepository postLinkRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;

    @Override
    @Transactional
    public PostEntity createPost(CreatePostRequest request) {

        //We have to check if any draft exists
        AccountEntity author = getAuthenticatedAccount();
        postRepository.findByAuthorIdAndStatusAndIsDeletedFalse(author.getId(), PostStatus.DRAFT)
                .ifPresent(existingDraft -> {
                    throw new IllegalStateException("You already have a draft post. " +
                            "Please update or publish your existing draft before creating a new one. ");
                });

        boolean isStrictValidation = request.getPostType() == PostType.POLL;
        validationUtil.validateCreatePostRequest(request, isStrictValidation);

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

        if (request.getPostType() == PostType.POLL && request.getPoll() != null) {
            createPoll(savedPost, request);
        }

        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            parseAndSaveContent(savedPost, request.getContent());
        }

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

    @Override
    @Transactional
    public PostEntity attachProductToDraft(UUID productId) {
        if (!productRepo.existsById(productId)) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        PostEntity draft = getOrCreateDraft();

        if (postProductRepository.existsByPostIdAndProductId(draft.getId(), productId)) {
            throw new IllegalArgumentException("Product already attached to my draft");
        }

        PostProductEntity postProduct = new PostProductEntity();
        postProduct.setPostId(draft.getId());
        postProduct.setProductId(productId);
        postProductRepository.save(postProduct);

        return draft;
    }

    @Override
    @Transactional
    public PostEntity attachShopToDraft(UUID shopId) {
        if (!shopRepo.existsById(shopId)) {
            throw new IllegalArgumentException("Shop not found: " + shopId);
        }

        PostEntity draft = getOrCreateDraft();

        if (postShopRepository.existsByPostIdAndShopId(draft.getId(), shopId)) {
            throw new IllegalArgumentException("Shop already attached to my draft");
        }

        PostShopEntity postShop = new PostShopEntity();
        postShop.setPostId(draft.getId());
        postShop.setShopId(shopId);
        postShopRepository.save(postShop);

        return draft;
    }

    @Override
    @Transactional
    public PostEntity attachEventToDraft(UUID eventId) {

        EventEntity event= eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found "));

        if (event.getStatus().equals(EventStatus.DRAFT)){
            throw new IllegalArgumentException("Event is not published yet");
        }

        PostEntity draft = getOrCreateDraft();

        if (postEventRepository.existsByPostIdAndEventId(draft.getId(), eventId)) {
            throw new IllegalArgumentException("Event already attached to my draft");
        }

        PostEventEntity postEvent = new PostEventEntity();
        postEvent.setPostId(draft.getId());
        postEvent.setEventId(eventId);
        postEventRepository.save(postEvent);

        return draft;
    }

    @Override
    @Transactional
    public PostEntity attachBuyTogetherGroupToDraft(UUID groupId) {
        if (!groupPurchaseRepo.existsById(groupId)) {
            throw new IllegalArgumentException("Buy together group not found: " + groupId);
        }

        PostEntity draft = getOrCreateDraft();

        if (postBuyTogetherGroupRepository.existsByPostIdAndGroupId(draft.getId(), groupId)) {
            throw new IllegalArgumentException("This buy together group already attached to your draft");
        }

        PostBuyTogetherGroupEntity postGroup = new PostBuyTogetherGroupEntity();
        postGroup.setPostId(draft.getId());
        postGroup.setGroupId(groupId);
        postBuyTogetherGroupRepository.save(postGroup);

        return draft;
    }

    @Override
    @Transactional
    public PostEntity attachInstallmentPlanToDraft(UUID planId) {
        if (!installmentPlanRepo.existsById(planId)) {
            throw new IllegalArgumentException("Installment plan not found: " + planId);
        }

        PostEntity draft = getOrCreateDraft();

        if (postInstallmentPlanRepository.existsByPostIdAndPlanId(draft.getId(), planId)) {
            throw new IllegalArgumentException("Installment plan already attached to this draft");
        }

        PostInstallmentPlanEntity postPlan = new PostInstallmentPlanEntity();
        postPlan.setPostId(draft.getId());
        postPlan.setPlanId(planId);
        postInstallmentPlanRepository.save(postPlan);

        return draft;
    }

    @Override
    @Transactional(readOnly = true)
    public PostEntity getMyCurrentDraft() {
        AccountEntity author = getAuthenticatedAccount();

        return postRepository
                .findByAuthorIdAndStatusAndIsDeletedFalse(author.getId(), PostStatus.DRAFT)
                .orElse(null);
    }

    private PostEntity getOrCreateDraft() {
        AccountEntity author = getAuthenticatedAccount();

        return postRepository
                .findByAuthorIdAndStatusAndIsDeletedFalse(author.getId(), PostStatus.DRAFT)
                .orElseGet(() -> createEmptyDraft(author));
    }

    private PostEntity createEmptyDraft(AccountEntity author) {
        PostEntity draft = new PostEntity();
        draft.setAuthorId(author.getId());
        draft.setPostType(PostType.REGULAR);
        draft.setStatus(PostStatus.DRAFT);
        return postRepository.save(draft);
    }

    @Override
    @Transactional
    public PostEntity removeProductFromDraft(UUID productId) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only modify your own draft");
        }

        PostProductEntity postProduct = postProductRepository.findByPostId(draft.getId())
                .stream()
                .filter(pp -> pp.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not attached to draft"));

        postProductRepository.delete(postProduct);
        return draft;
    }

    @Override
    @Transactional
    public PostEntity removeShopFromDraft(UUID shopId) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only modify your own draft");
        }

        PostShopEntity postShop = postShopRepository.findByPostId(draft.getId())
                .stream()
                .filter(ps -> ps.getShopId().equals(shopId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Shop not attached to draft"));

        postShopRepository.delete(postShop);
        return draft;
    }

    @Override
    @Transactional
    public PostEntity removeEventFromDraft(UUID eventId) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only modify your own draft");
        }

        PostEventEntity postEvent = postEventRepository.findByPostId(draft.getId())
                .stream()
                .filter(pe -> pe.getEventId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Event not attached to draft"));

        postEventRepository.delete(postEvent);
        return draft;
    }

    @Override
    @Transactional
    public PostEntity removeBuyTogetherGroupFromDraft(UUID groupId) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only modify your own draft");
        }

        PostBuyTogetherGroupEntity postGroup = postBuyTogetherGroupRepository.findByPostId(draft.getId())
                .stream()
                .filter(pg -> pg.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Buy together group not attached to draft"));

        postBuyTogetherGroupRepository.delete(postGroup);
        return draft;
    }

    @Override
    @Transactional
    public PostEntity removeInstallmentPlanFromDraft(UUID planId) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only modify your own draft");
        }

        PostInstallmentPlanEntity postPlan = postInstallmentPlanRepository.findByPostId(draft.getId())
                .stream()
                .filter(pp -> pp.getPlanId().equals(planId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Installment plan not attached to draft"));

        postInstallmentPlanRepository.delete(postPlan);
        return draft;
    }

    @Override
    @Transactional
    public void discardDraft() {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity draft = getMyCurrentDraft();

        if (draft == null) {
            throw new IllegalArgumentException("No draft post found");
        }

        if (!draft.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only discard your own draft");
        }

        postProductRepository.deleteByPostId(draft.getId());
        postShopRepository.deleteByPostId(draft.getId());
        postEventRepository.deleteByPostId(draft.getId());
        postBuyTogetherGroupRepository.deleteByPostId(draft.getId());
        postInstallmentPlanRepository.deleteByPostId(draft.getId());
        postCollaboratorRepository.deleteByPostId(draft.getId());

        postRepository.delete(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public PostEntity getPostById(UUID postId) {
        return postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostEntity> getPostsByAuthor(UUID authorId, Pageable pageable) {
        if (!accountRepo.existsById(authorId)) {
            throw new IllegalArgumentException("Author not found");
        }
        return postRepository.findByAuthorIdAndIsDeletedFalse(authorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostEntity> getPublishedPosts(Pageable pageable) {
        return postRepository.findByStatusAndIsDeletedFalse(PostStatus.PUBLISHED, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostEntity> getMyScheduledPosts() {
        AccountEntity author = getAuthenticatedAccount();

        return postRepository.findByAuthorIdAndStatusAndScheduledAtBeforeAndIsDeletedFalse(
                author.getId(),
                PostStatus.SCHEDULED,
                LocalDateTime.now().plusYears(100)
        );
    }

    @Override
    @Transactional
    public PostEntity updateDraft(UpdateDraftRequest request) {
        AccountEntity author = getAuthenticatedAccount();
        PostEntity post = getMyCurrentDraft();

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be updated");
        }

        boolean contentChanged = request.getContent() != null;
        boolean mediaChanged = request.getMedia() != null;
        boolean privacyChanged = request.getPrivacySettings() != null;

        if (contentChanged) {
            post.setContent(request.getContent());
        }
        if (mediaChanged) {
            setMediaData(post, request);
        }
        if (privacyChanged) {
            updatePrivacySettings(post, request.getPrivacySettings());
        }

        // CRITICAL FIX: Save + FLUSH first so post is attached and ID is safe
        post = postRepository.saveAndFlush(post);

        // NOW safe to parse — post is managed, ID is guaranteed
        if (contentChanged) {
            // Delete old ones using the confirmed ID
            postUserMentionRepository.deleteByPostId(post.getId());
            postShopMentionRepository.deleteByPostId(post.getId());
            postHashtagRepository.deleteByPostId(post.getId());

            // Now parse and save — this will work 100%
            parseAndSaveContent(post, request.getContent());
        }

        return post;
    }

    @Override
    @Transactional
    public PostEntity updateDraftContent(UUID postId, String content) {
        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be updated");
        }

        post.setContent(content);

        postUserMentionRepository.deleteByPostId(post.getId());
        postShopMentionRepository.deleteByPostId(post.getId());
        postHashtagRepository.deleteByPostId(post.getId());

        if (content != null && !content.trim().isEmpty()) {
            parseAndSaveContent(post, content);
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public PostEntity addMediaToDraft(UUID postId, List<MediaRequest> media) {
        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be updated");
        }

        validationUtil.validateMedia(media);

        UpdateDraftRequest request = new UpdateDraftRequest();
        request.setMedia(media);
        setMediaData(post, request);

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public PostEntity updateDraftPrivacySettings(PrivacySettingsRequest settings) {
        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = getMyCurrentDraft();


        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be updated");
        }

        updatePrivacySettings(post, settings);

        return postRepository.save(post);
    }

    private void updatePrivacySettings(PostEntity post, PrivacySettingsRequest settings) {
        if (settings.getVisibility() != null) {
            post.setVisibility(settings.getVisibility());
        }
        if (settings.getWhoCanComment() != null) {
            post.setWhoCanComment(settings.getWhoCanComment());
        }
        if (settings.getWhoCanRepost() != null) {
            post.setWhoCanRepost(settings.getWhoCanRepost());
        }
        if (settings.getHideLikesCount() != null) {
            post.setHideLikesCount(settings.getHideLikesCount());
        }
        if (settings.getHideCommentsCount() != null) {
            post.setHideCommentsCount(settings.getHideCommentsCount());
        }
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        AccountEntity author = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only delete your own posts");
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());

        postRepository.save(post);
    }

    @Override
    @Transactional
    public PostEntity updateDraftCollaboration(CollaborationRequest collaboration) {
        AccountEntity author = getAuthenticatedAccount();


        PostEntity post = getMyCurrentDraft();

        if (!post.getAuthorId().equals(author.getId())) {
            throw new IllegalArgumentException("You can only update your own posts");
        }

        if (post.getStatus() != PostStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft posts can be updated");
        }

        validationUtil.validateCollaboration(collaboration);

        // Remove existing collaborators
        postCollaboratorRepository.deleteByPostId(post.getId());

        // Update collaborative flag
        post.setCollaborative(collaboration.getIsCollaborative());

        // Add new collaborators if collaborative
        if (collaboration.getIsCollaborative() && collaboration.getCollaboratorIds() != null) {
            for (UUID collaboratorId : collaboration.getCollaboratorIds()) {
                if (collaboratorId.equals(author.getId())) {
                    throw new IllegalArgumentException("You cannot add yourself as a collaborator");
                }

                if (!accountRepo.existsById(collaboratorId)) {
                    throw new IllegalArgumentException("Collaborator not found: " + collaboratorId);
                }

                PostCollaboratorEntity collaborator = new PostCollaboratorEntity();
                collaborator.setPostId(post.getId());
                collaborator.setUserId(collaboratorId);
                collaborator.setStatus(CollaboratorStatus.PENDING);
                postCollaboratorRepository.save(collaborator);
            }
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public PostEntity acceptCollaboration(UUID postId) {
        AccountEntity collaborator = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        PostCollaboratorEntity collaboration = postCollaboratorRepository
                .findByPostIdAndUserId(postId, collaborator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Collaboration invitation not found"));

        if (collaboration.getStatus() != CollaboratorStatus.PENDING) {
            throw new IllegalArgumentException("Collaboration already " + collaboration.getStatus().name().toLowerCase());
        }

        collaboration.setStatus(CollaboratorStatus.ACCEPTED);
        collaboration.setRespondedAt(LocalDateTime.now());
        postCollaboratorRepository.save(collaboration);

        return post;
    }

    @Override
    @Transactional
    public PostEntity declineCollaboration(UUID postId) {
        AccountEntity collaborator = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        PostCollaboratorEntity collaboration = postCollaboratorRepository
                .findByPostIdAndUserId(postId, collaborator.getId())
                .orElseThrow(() -> new IllegalArgumentException("Collaboration invitation not found"));

        if (collaboration.getStatus() != CollaboratorStatus.PENDING) {
            throw new IllegalArgumentException("Collaboration already " + collaboration.getStatus().name().toLowerCase());
        }

        collaboration.setStatus(CollaboratorStatus.DECLINED);
        collaboration.setRespondedAt(LocalDateTime.now());
        postCollaboratorRepository.save(collaboration);

        return post;
    }

    @Override
    @Transactional
    public void removeCollaborator(UUID postId, UUID collaboratorId) {
        AccountEntity currentUser = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        PostCollaboratorEntity collaboration = postCollaboratorRepository
                .findByPostIdAndUserId(postId, collaboratorId)
                .orElseThrow(() -> new IllegalArgumentException("Collaborator not found"));

        boolean isAuthor = post.getAuthorId().equals(currentUser.getId());
        boolean isSelf = collaboratorId.equals(currentUser.getId());

        if (!isAuthor && !isSelf) {
            throw new IllegalArgumentException("You can only remove yourself or be the post author to remove collaborators");
        }

        if (collaboration.getStatus() != CollaboratorStatus.ACCEPTED) {
            throw new IllegalArgumentException("Can only remove accepted collaborators");
        }

        postCollaboratorRepository.delete(collaboration);

        long remainingCollaborators = postCollaboratorRepository
                .findByPostIdAndStatus(postId, CollaboratorStatus.ACCEPTED)
                .size();

        if (remainingCollaborators == 0) {
            post.setCollaborative(false);
            postRepository.save(post);
        }
    }

    private void parseAndSaveContent(PostEntity post, String content) {
        parseAndSaveMentions(post, content);
        parseAndSaveHashtags(post, content);
        parseAndSaveShopMentions(post, content);
    }

    private void parseAndSaveMentions(PostEntity post, String content) {
        List<ContentParsingUtil.ParsedMention> mentions = contentParsingUtil.parseMentions(content);

        for (ContentParsingUtil.ParsedMention mention : mentions) {
            accountRepo.findByUserName(mention.getUserName()).ifPresent(user -> {
                if (!postUserMentionRepository.existsByPostIdAndMentionedUserId(post.getId(), user.getId())) {
                    PostUserMentionEntity mentionEntity = new PostUserMentionEntity();
                    mentionEntity.setPostId(post.getId());
                    mentionEntity.setMentionedUserId(user.getId());
                    mentionEntity.setStartIndex(mention.getStartIndex());
                    mentionEntity.setEndIndex(mention.getEndIndex());
                    postUserMentionRepository.save(mentionEntity);
                }
            });
        }
    }

    private void parseAndSaveHashtags(PostEntity post, String content) {
        List<ContentParsingUtil.ParsedHashtag> hashtags = contentParsingUtil.parseHashtags(content);

        for (ContentParsingUtil.ParsedHashtag hashtag : hashtags) {
            PostHashtagEntity hashtagEntity = new PostHashtagEntity();
            hashtagEntity.setPostId(post.getId());
            hashtagEntity.setHashtag(hashtag.getHashtag());
            hashtagEntity.setStartIndex(hashtag.getStartIndex());
            hashtagEntity.setEndIndex(hashtag.getEndIndex());
            postHashtagRepository.save(hashtagEntity);
        }
    }

    private void parseAndSaveShopMentions(PostEntity post, String content) {
        List<ContentParsingUtil.ParsedShopMention> shopMentions = contentParsingUtil.parseShopMentions(content);

        for (ContentParsingUtil.ParsedShopMention shopMention : shopMentions) {
            shopRepo.findByShopSlugAndIsDeletedFalse(shopMention.getShopSlug()).ifPresent(shop -> {
                if (!postShopMentionRepository.existsByPostIdAndMentionedShopId(post.getId(), shop.getShopId())) {
                    PostShopMentionEntity mentionEntity = new PostShopMentionEntity();
                    mentionEntity.setPostId(post.getId());
                    mentionEntity.setMentionedShopId(shop.getShopId());
                    mentionEntity.setStartIndex(shopMention.getStartIndex());
                    mentionEntity.setEndIndex(shopMention.getEndIndex());
                    postShopMentionRepository.save(mentionEntity);
                }
            });
        }
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
            post.setStatus(PostStatus.DRAFT);
            // post.setPublishedAt(LocalDateTime.now());
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

    private void setMediaData(PostEntity post, UpdateDraftRequest request) {
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

        if (attachments.getExternalLink() != null) {
            saveExternalLink(post, attachments.getExternalLink().getUrl());
        }
    }

    private void saveProductAttachments(PostEntity post, List<UUID> productIds) {
        // Remove duplicates from the input list
        List<UUID> uniqueProductIds = productIds.stream()
                .distinct()
                .toList();

        for (UUID productId : uniqueProductIds) {
            // Validate product exists
            if (!productRepo.existsById(productId)) {
                throw new IllegalArgumentException("Product not found, " + productId);
            }

            // Check if this product is already attached to the post
            if (!postProductRepository.existsByPostIdAndProductId(post.getId(), productId)) {
                PostProductEntity postProduct = new PostProductEntity();
                postProduct.setPostId(post.getId());
                postProduct.setProductId(productId);
                postProductRepository.save(postProduct);
            }
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

    private void saveExternalLink(PostEntity post, String url) {
        String shortCode = generateUniqueShortCode();
        String domain = linkProcessingUtil.extractDomain(url);
        boolean isSafe = linkProcessingUtil.isUrlSafe(url);

        PostLinkEntity link = new PostLinkEntity();
        link.setPostId(post.getId());
        link.setOriginalUrl(url);
        link.setShortCode(shortCode);
        link.setDomain(domain);
        link.setSafe(isSafe);

        postLinkRepository.save(link);

        post.setHasExternalLink(true);
        postRepository.save(post);
    }

    private String generateUniqueShortCode() {
        String shortCode;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            shortCode = linkProcessingUtil.generateShortCode();
            attempts++;

            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique short code");
            }
        } while (postLinkRepository.findByShortCode(shortCode).isPresent());

        return shortCode;
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

    private void createPoll(PostEntity post, CreatePostRequest request) {
        PollEntity poll = new PollEntity();
        poll.setPostId(post.getId());
        poll.setTitle(request.getPoll().getTitle());
        poll.setDescription(request.getPoll().getDescription());
        poll.setAllowMultipleVotes(request.getPoll().getAllowMultipleVotes());
        poll.setAnonymous(request.getPoll().getIsAnonymous());
        poll.setExpiresAt(request.getPoll().getExpiresAt());
        poll.setTotalVotes(0L);

        PollEntity savedPoll = pollRepository.save(poll);

        if (request.getPoll().getOptions() != null) {
            for (int i = 0; i < request.getPoll().getOptions().size(); i++) {
                var optionRequest = request.getPoll().getOptions().get(i);
                PollOptionEntity option = new PollOptionEntity();
                option.setPollId(savedPoll.getId());
                option.setOptionText(optionRequest.getOptionText());
                option.setOptionImageUrl(optionRequest.getOptionImageUrl());
                option.setOptionOrder(i + 1);
                option.setVotesCount(0L);
                pollOptionRepository.save(option);
            }
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