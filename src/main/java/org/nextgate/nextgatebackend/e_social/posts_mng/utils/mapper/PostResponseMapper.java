package org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollOptionEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.CollaboratorStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.ContentEntityType;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.LinkStatus;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.MediaData;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PostResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostResponseMapper {

    private final ObjectMapper objectMapper;
    private final AccountRepo accountRepo;

    private final PostUserMentionRepository postUserMentionRepository;
    private final PostShopMentionRepository postShopMentionRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostCollaboratorRepository postCollaboratorRepository;
    private final PostLinkRepository postLinkRepository;

    private final PostProductRepository postProductRepository;
    private final PostShopRepository postShopRepository;
    private final PostEventRepository postEventRepository;
    private final PostBuyTogetherGroupRepository postBuyTogetherGroupRepository;
    private final PostInstallmentPlanRepository postInstallmentPlanRepository;

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;

    private final ProductRepo productRepo;
    private final ShopRepo shopRepo;
    private final EventsRepo eventsRepo;
    private final GroupPurchaseInstanceRepo groupPurchaseInstanceRepo;
    private final InstallmentPlanRepo installmentPlanRepo;

    public PostResponse toPostResponse(PostEntity post) {
        // Handle null post
        if (post == null) {
            return null;
        }

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setAuthor(mapAuthor(post));
        response.setContent(post.getContent());
        response.setContentParsed(mapContentParsed(post));
        response.setPostType(post.getPostType());
        response.setStatus(post.getStatus());
        response.setMedia(mapMedia(post));
        response.setPoll(mapPoll(post));
        response.setAttachments(mapAttachments(post));
        response.setCollaboration(mapCollaboration(post));
        response.setPrivacySettings(mapPrivacySettings(post));
        response.setEngagement(mapEngagement(post));

        AccountEntity currentUser = getAuthenticatedAccountOrNull();
        response.setUserInteraction(mapUserInteraction(post, currentUser != null ? currentUser.getId() : null));

        response.setCreatedAt(post.getCreatedAt());
        response.setPublishedAt(post.getPublishedAt());
        response.setScheduledAt(post.getScheduledAt());
        return response;
    }

    private PostResponse.Author mapAuthor(PostEntity post) {
        PostResponse.Author author = new PostResponse.Author();
        author.setId(post.getAuthorId());

        accountRepo.findById(post.getAuthorId()).ifPresent(account -> {
            author.setUserName(account.getUserName());
            author.setFirstName(account.getFirstName());
            author.setLastName(account.getLastName());
            author.setProfilePictureUrl(account.getProfilePictureUrls() != null && !account.getProfilePictureUrls().isEmpty()
                    ? account.getProfilePictureUrls().getFirst()
                    : null);
            author.setVerified(account.getIsVerified());
        });

        return author;
    }

    private PostResponse.ContentParsed mapContentParsed(PostEntity post) {
        PostResponse.ContentParsed parsed = new PostResponse.ContentParsed();
        parsed.setText(post.getContent());
        List<PostResponse.ContentEntity> entities = new ArrayList<>();

        postUserMentionRepository.findByPostId(post.getId()).forEach(mention -> {
            accountRepo.findById(mention.getMentionedUserId()).ifPresent(user -> {
                PostResponse.ContentEntity entity = new PostResponse.ContentEntity();
                entity.setType(ContentEntityType.MENTION);
                entity.setText("@" + user.getUserName());
                entity.setStartIndex(mention.getStartIndex());
                entity.setEndIndex(mention.getEndIndex());

                PostResponse.User mentionedUser = new PostResponse.User();
                mentionedUser.setId(user.getId());
                mentionedUser.setUserName(user.getUserName());
                mentionedUser.setFirstName(user.getFirstName());
                mentionedUser.setLastName(user.getLastName());
                mentionedUser.setProfilePictureUrl(user.getProfilePictureUrls() != null && !user.getProfilePictureUrls().isEmpty()
                        ? user.getProfilePictureUrls().getFirst()
                        : null);
                entity.setUser(mentionedUser);

                entities.add(entity);
            });
        });

        postHashtagRepository.findByPostId(post.getId()).forEach(hashtag -> {
            PostResponse.ContentEntity entity = new PostResponse.ContentEntity();
            entity.setType(ContentEntityType.HASHTAG);
            entity.setText("#" + hashtag.getHashtag());
            entity.setStartIndex(hashtag.getStartIndex());
            entity.setEndIndex(hashtag.getEndIndex());
            entity.setHashtag(hashtag.getHashtag());
            entities.add(entity);
        });

        postShopMentionRepository.findByPostId(post.getId()).forEach(shopMention -> {
            shopRepo.findByShopIdAndIsDeletedFalseAndStatus(shopMention.getMentionedShopId(), ShopStatus.ACTIVE).ifPresent(shop -> {
                PostResponse.ContentEntity entity = new PostResponse.ContentEntity();
                entity.setType(ContentEntityType.SHOP_MENTION);
                entity.setText("$" + shop.getShopSlug());
                entity.setStartIndex(shopMention.getStartIndex());
                entity.setEndIndex(shopMention.getEndIndex());

                PostResponse.Shop mentionedShop = new PostResponse.Shop();
                mentionedShop.setId(shop.getShopId());
                mentionedShop.setShopName(shop.getShopName());
                mentionedShop.setShopSlug(shop.getShopSlug());
                mentionedShop.setLogoUrl(shop.getLogoUrl());
                entity.setShop(mentionedShop);

                entities.add(entity);
            });
        });

        parsed.setEntities(entities);
        return parsed;
    }

    private List<PostResponse.Media> mapMedia(PostEntity post) {
        List<PostResponse.Media> mediaList = new ArrayList<>();

        if (post.getMediaData() == null || post.getMediaData().trim().isEmpty()) {
            return mediaList;
        }

        try {
            List<MediaData> mediaDataList = objectMapper.readValue(
                    post.getMediaData(),
                    new TypeReference<List<MediaData>>() {}
            );

            for (MediaData mediaData : mediaDataList) {
                PostResponse.Media media = new PostResponse.Media();
                media.setId(mediaData.getId());
                media.setMediaType(mediaData.getMediaType());
                media.setOriginalUrl(mediaData.getOriginalUrl());
                media.setThumbnailUrl(mediaData.getThumbnailUrl());
                media.setPlaceholderBase64(mediaData.getPlaceholderBase64());
                media.setWidth(mediaData.getWidth());
                media.setHeight(mediaData.getHeight());
                media.setDuration(mediaData.getDuration());
                media.setOrder(mediaData.getOrder());
                media.setImageTags(new ArrayList<>());
                mediaList.add(media);
            }
        } catch (JsonProcessingException e) {
            // Silently handle error
        }

        return mediaList;
    }

    private PostResponse.Poll mapPoll(PostEntity post) {
        if (post.getPostType() != PostType.POLL) {
            return null;
        }

        return pollRepository.findByPostId(post.getId()).map(poll -> {
            PostResponse.Poll pollResponse = new PostResponse.Poll();
            pollResponse.setId(poll.getId());
            pollResponse.setTitle(poll.getTitle());
            pollResponse.setDescription(poll.getDescription());
            pollResponse.setTotalVotes(poll.getTotalVotes());
            pollResponse.setAllowMultipleVotes(poll.isAllowMultipleVotes());
            pollResponse.setAnonymous(poll.isAnonymous());
            pollResponse.setExpiresAt(poll.getExpiresAt());
            pollResponse.setHasExpired(poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt()));

            List<PollOptionEntity> options = pollOptionRepository.findByPollIdOrderByOptionOrder(poll.getId());

            AccountEntity currentUser = getAuthenticatedAccountOrNull();
            UUID currentUserId = currentUser != null ? currentUser.getId() : null;

            boolean userHasVoted = currentUserId != null && pollVoteRepository.existsByPollIdAndVoterId(poll.getId(), currentUserId);
            pollResponse.setUserHasVoted(userHasVoted);

            List<UUID> userVotedOptions = currentUserId != null
                    ? pollVoteRepository.findByPollIdAndVoterId(poll.getId(), currentUserId)
                    .stream()
                    .map(vote -> vote.getOptionId())
                    .collect(Collectors.toList())
                    : new ArrayList<>();
            pollResponse.setUserVotedOptions(userVotedOptions);

            List<PostResponse.PollOption> pollOptions = options.stream()
                    .map(option -> {
                        PostResponse.PollOption pollOption = new PostResponse.PollOption();
                        pollOption.setId(option.getId());
                        pollOption.setOptionText(option.getOptionText());
                        pollOption.setOptionImageUrl(option.getOptionImageUrl());
                        pollOption.setOptionOrder(option.getOptionOrder());
                        pollOption.setVotesCount(option.getVotesCount());

                        double percentage = poll.getTotalVotes() > 0
                                ? (option.getVotesCount() * 100.0) / poll.getTotalVotes()
                                : 0.0;
                        pollOption.setPercentage(Math.round(percentage * 10.0) / 10.0);

                        pollOption.setHasVoted(userVotedOptions.contains(option.getId()));

                        return pollOption;
                    })
                    .collect(Collectors.toList());

            pollResponse.setOptions(pollOptions);

            return pollResponse;
        }).orElse(null);
    }

    private PostResponse.Attachments mapAttachments(PostEntity post) {
        PostResponse.Attachments attachments = new PostResponse.Attachments();

        attachments.setProducts(mapAttachedProducts(post));
        attachments.setShops(mapAttachedShops(post));
        attachments.setEvents(mapAttachedEvents(post));
        attachments.setBuyTogetherGroups(mapAttachedBuyTogetherGroups(post));
        attachments.setInstallmentPlans(mapAttachedInstallmentPlans(post));
        attachments.setExternalLink(mapExternalLink(post));

        return attachments;
    }

    private List<PostResponse.AttachedProduct> mapAttachedProducts(PostEntity post) {
        List<PostResponse.AttachedProduct> products = new ArrayList<>();

        postProductRepository.findByPostId(post.getId()).forEach(postProduct -> {
            productRepo.findById(postProduct.getProductId()).ifPresent(product -> {
                PostResponse.AttachedProduct attachedProduct = new PostResponse.AttachedProduct();
                attachedProduct.setId(product.getProductId());
                attachedProduct.setName(product.getProductName());
                attachedProduct.setPrice(product.getPrice());
                attachedProduct.setImageUrl(product.getProductImages() != null && !product.getProductImages().isEmpty()
                        ? product.getProductImages().getFirst()
                        : null);
                attachedProduct.setInStock(product.getStockQuantity() > 0);

                if (product.getShop() != null && product.getShop().getShopId() != null) {
                    shopRepo.findByShopIdAndIsDeletedFalseAndStatus(product.getShop().getShopId(), ShopStatus.ACTIVE).ifPresent(shop -> {
                        attachedProduct.setShopName(shop.getShopName());
                        attachedProduct.setShopId(shop.getShopId());
                    });
                }

                attachedProduct.setSocialContext(null);
                products.add(attachedProduct);
            });
        });

        return products;
    }

    private List<PostResponse.AttachedShop> mapAttachedShops(PostEntity post) {
        List<PostResponse.AttachedShop> shops = new ArrayList<>();

        postShopRepository.findByPostId(post.getId()).forEach(postShop -> {
            shopRepo.findByShopIdAndIsDeletedFalseAndStatus(postShop.getShopId(), ShopStatus.ACTIVE).ifPresent(shop -> {
                PostResponse.AttachedShop attachedShop = new PostResponse.AttachedShop();
                attachedShop.setId(shop.getShopId());
                attachedShop.setName(shop.getShopName());
                attachedShop.setLogoUrl(shop.getLogoUrl());
                attachedShop.setDescription(shop.getShopDescription());
                attachedShop.setVerified(shop.getIsVerified());
                shops.add(attachedShop);
            });
        });

        return shops;
    }

    private List<PostResponse.AttachedEvent> mapAttachedEvents(PostEntity post) {
        List<PostResponse.AttachedEvent> events = new ArrayList<>();

        postEventRepository.findByPostId(post.getId()).forEach(postEvent -> {
            eventsRepo.findById(postEvent.getEventId()).ifPresent(event -> {
                PostResponse.AttachedEvent attachedEvent = new PostResponse.AttachedEvent();
                attachedEvent.setId(event.getId());
                attachedEvent.setTitle(event.getTitle());
                attachedEvent.setDescription(event.getDescription());
                attachedEvent.setImageUrl(event.getMedia() != null ? event.getMedia().getBanner() : null);
                attachedEvent.setDate(event.getStartDateTime() != null ? event.getStartDateTime().toLocalDateTime() : null);
                attachedEvent.setEndDate(event.getEndDateTime() != null ? event.getEndDateTime().toLocalDateTime() : null);
                attachedEvent.setLocation(event.getVenue() != null ? event.getVenue().getName() : null);
                attachedEvent.setAddress(event.getVenue() != null ? event.getVenue().getAddress() : null);
                attachedEvent.setSocialContext(null);
                events.add(attachedEvent);
            });
        });

        return events;
    }

    private List<PostResponse.AttachedBuyTogetherGroup> mapAttachedBuyTogetherGroups(PostEntity post) {
        List<PostResponse.AttachedBuyTogetherGroup> groups = new ArrayList<>();

        postBuyTogetherGroupRepository.findByPostId(post.getId()).forEach(postGroup -> {
            groupPurchaseInstanceRepo.findById(postGroup.getGroupId()).ifPresent(group -> {
                PostResponse.AttachedBuyTogetherGroup attachedGroup = new PostResponse.AttachedBuyTogetherGroup();
                attachedGroup.setId(group.getGroupInstanceId());
                attachedGroup.setCurrentCount(group.getSeatsOccupied());
                attachedGroup.setTotalSlots(group.getTotalSeats());
                attachedGroup.setOriginalPrice(group.getRegularPrice());
                attachedGroup.setDiscountPrice(group.getGroupPrice());

                if (group.getRegularPrice() != null && group.getGroupPrice() != null) {
                    BigDecimal savings = group.getRegularPrice().subtract(group.getGroupPrice());
                    attachedGroup.setSavingsAmount(savings);
                    attachedGroup.setSavingsPercentage(
                            savings.multiply(new BigDecimal("100"))
                                    .divide(group.getRegularPrice(), 2, BigDecimal.ROUND_HALF_UP)
                    );
                }
                attachedGroup.setExpiresAt(group.getExpiresAt());
                attachedGroup.setStatus(group.getStatus().name());

                if (group.getProduct() != null && group.getProduct().getProductId() != null) {
                    productRepo.findById(group.getProduct().getProductId()).ifPresent(product -> {
                        attachedGroup.setProductName(product.getProductName());
                        attachedGroup.setProductImageUrl(product.getProductImages() != null && !product.getProductImages().isEmpty()
                                ? product.getProductImages().getFirst()
                                : null);
                    });
                }

                attachedGroup.setSocialContext(null);
                groups.add(attachedGroup);
            });
        });

        return groups;
    }

    private List<PostResponse.AttachedInstallmentPlan> mapAttachedInstallmentPlans(PostEntity post) {
        List<PostResponse.AttachedInstallmentPlan> plans = new ArrayList<>();

        postInstallmentPlanRepository.findByPostId(post.getId()).forEach(postPlan -> {
            installmentPlanRepo.findById(postPlan.getPlanId()).ifPresent(plan -> {
                PostResponse.AttachedInstallmentPlan attachedPlan = new PostResponse.AttachedInstallmentPlan();
                attachedPlan.setId(plan.getPlanId());
                attachedPlan.setProductId(plan.getProduct() != null ? plan.getProduct().getProductId() : null);

                if (plan.getProduct() != null && plan.getProduct().getProductId() != null) {
                    productRepo.findById(plan.getProduct().getProductId()).ifPresent(product -> {
                        attachedPlan.setProductName(product.getProductName());
                        attachedPlan.setProductImageUrl(product.getProductImages() != null && !product.getProductImages().isEmpty()
                                ? product.getProductImages().getFirst()
                                : null);
                    });
                }

                plans.add(attachedPlan);
            });
        });

        return plans;
    }

    private PostResponse.ExternalLink mapExternalLink(PostEntity post) {
        return postLinkRepository.findByPostId(post.getId()).map(link -> {
            PostResponse.ExternalLink externalLink = new PostResponse.ExternalLink();
            externalLink.setId(link.getId());
            externalLink.setOriginalUrl(link.getOriginalUrl());
            externalLink.setShortUrl("https://nexgate.link/" + link.getShortCode());
            externalLink.setShortCode(link.getShortCode());
            externalLink.setValidated(link.getStatus() == LinkStatus.VALIDATED);
            externalLink.setSafe(link.isSafe());
            externalLink.setClickCount(link.getClickCount());
            externalLink.setCreatedAt(link.getCreatedAt());

            PostResponse.LinkPreview preview = new PostResponse.LinkPreview();
            preview.setDomain(link.getDomain());
            preview.setTitle(null);
            preview.setDescription(null);
            preview.setImageUrl(null);
            preview.setFavicon(null);
            externalLink.setPreview(preview);

            return externalLink;
        }).orElse(null);
    }

    private PostResponse.Collaboration mapCollaboration(PostEntity post) {
        PostResponse.Collaboration collaboration = new PostResponse.Collaboration();
        collaboration.setCollaborative(post.isCollaborative());

        if (!post.isCollaborative()) {
            collaboration.setCollaborators(new ArrayList<>());
            collaboration.setByline(null);
            return collaboration;
        }

        List<PostResponse.Collaborator> collaborators = new ArrayList<>();
        postCollaboratorRepository.findByPostId(post.getId()).forEach(collab -> {
            PostResponse.Collaborator collaborator = new PostResponse.Collaborator();
            collaborator.setId(collab.getId());
            collaborator.setStatus(collab.getStatus());
            collaborator.setInvitedAt(collab.getInvitedAt());
            collaborator.setRespondedAt(collab.getRespondedAt());

            accountRepo.findById(collab.getUserId()).ifPresent(user -> {
                PostResponse.User collabUser = new PostResponse.User();
                collabUser.setId(user.getId());
                collabUser.setUserName(user.getUserName());
                collabUser.setFirstName(user.getFirstName());
                collabUser.setLastName(user.getLastName());
                collabUser.setProfilePictureUrl(user.getProfilePictureUrls() != null && !user.getProfilePictureUrls().isEmpty()
                        ? user.getProfilePictureUrls().getFirst()
                        : null);
                collaborator.setUser(collabUser);
            });

            collaborators.add(collaborator);
        });

        collaboration.setCollaborators(collaborators);
        collaboration.setByline(buildByline(post, collaborators));

        return collaboration;
    }

    private String buildByline(PostEntity post, List<PostResponse.Collaborator> collaborators) {
        StringBuilder byline = new StringBuilder("By ");

        accountRepo.findById(post.getAuthorId()).ifPresent(author -> {
            byline.append(author.getFirstName()).append(" ").append(author.getLastName());
        });

        long acceptedCount = collaborators.stream()
                .filter(c -> c.getStatus() == CollaboratorStatus.ACCEPTED)
                .count();

        if (acceptedCount > 0) {
            byline.append(" and ");
            if (acceptedCount == 1) {
                collaborators.stream()
                        .filter(c -> c.getStatus() == CollaboratorStatus.ACCEPTED)
                        .findFirst()
                        .ifPresent(c -> {
                            if (c.getUser() != null) {
                                byline.append(c.getUser().getFirstName())
                                        .append(" ")
                                        .append(c.getUser().getLastName());
                            }
                        });
            } else {
                byline.append(acceptedCount).append(" others");
            }
        }

        return byline.toString();
    }

    private PostResponse.PrivacySettings mapPrivacySettings(PostEntity post) {
        PostResponse.PrivacySettings settings = new PostResponse.PrivacySettings();
        settings.setVisibility(post.getVisibility());
        settings.setWhoCanComment(post.getWhoCanComment());
        settings.setWhoCanRepost(post.getWhoCanRepost());
        settings.setHideLikesCount(post.isHideLikesCount());
        settings.setHideCommentsCount(post.isHideCommentsCount());
        return settings;
    }

    private PostResponse.Engagement mapEngagement(PostEntity post) {
        PostResponse.Engagement engagement = new PostResponse.Engagement();
        engagement.setLikesCount(post.getLikesCount());
        engagement.setCommentsCount(post.getCommentsCount());
        engagement.setRepostsCount(post.getRepostsCount());
        engagement.setQuotesCount(0);
        engagement.setBookmarksCount(post.getBookmarksCount());
        engagement.setSharesCount(0);
        engagement.setViewsCount(post.getViewsCount());
        engagement.setCanLike(true);
        engagement.setCanComment(true);
        engagement.setCanRepost(true);
        engagement.setCanShare(true);
        return engagement;
    }

    private PostResponse.UserInteraction mapUserInteraction(PostEntity post, UUID currentUserId) {
        PostResponse.UserInteraction interaction = new PostResponse.UserInteraction();

        if (currentUserId == null) {
            interaction.setHasLiked(false);
            interaction.setHasBookmarked(false);
            interaction.setHasReposted(false);
            interaction.setHasCommented(false);
            interaction.setHasViewed(false);
            return interaction;
        }

        // TODO: Query interaction tables when implemented
        interaction.setHasLiked(false);
        interaction.setHasBookmarked(false);
        interaction.setHasReposted(false);
        interaction.setHasCommented(false);
        interaction.setHasViewed(false);

        return interaction;
    }

    public List<PostResponse> toPostResponseList(List<PostEntity> posts) {
        List<PostResponse> responses = new ArrayList<>();
        for (PostEntity post : posts) {
            responses.add(toPostResponse(post));
        }
        return responses;
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