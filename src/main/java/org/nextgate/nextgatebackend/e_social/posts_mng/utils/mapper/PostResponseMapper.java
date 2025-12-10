package org.nextgate.nextgatebackend.e_social.posts_mng.utils.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_commerce.group_purchase_mng.repo.GroupPurchaseInstanceRepo;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.entity.InstallmentPlanEntity;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.FulfillmentTiming;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.enums.PaymentFrequency;
import org.nextgate.nextgatebackend.e_commerce.installment_purchase.repo.InstallmentPlanRepo;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.entity.ProductEntity;
import org.nextgate.nextgatebackend.e_commerce.products_mng_service.products.repo.ProductRepo;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopStatus;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.repo.ShopRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostBookmarkRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostLikeRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostRepostRepository;
import org.nextgate.nextgatebackend.e_social.interactions.repo.PostViewRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollOptionEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollVoteEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.*;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.MediaData;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PostResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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
    private final PostLikeRepository postLikeRepository;
    private final PostBookmarkRepository postBookmarkRepository;
    private final PostRepostRepository postRepostRepository;
    private final PostViewRepository postViewRepository;
    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;

    public PostResponse toPostResponse(PostEntity post) {
        // Handle null post
        if (post == null) {
            return null;
        }

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setQuotedPost(mapQuotedPost(post));
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
                    .map(PollVoteEntity::getOptionId)
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

    private PostResponse.Poll mapQuotedPoll(PostEntity post) {
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
            pollResponse.setHasExpired(poll.getExpiresAt() != null &&
                    LocalDateTime.now().isAfter(poll.getExpiresAt()));

            // IMPORTANT: In quoted polls → NEVER reveal current user's votes
            pollResponse.setUserHasVoted(false);
            pollResponse.setUserVotedOptions(Collections.emptyList());

            List<PollOptionEntity> options = pollOptionRepository
                    .findByPollIdOrderByOptionOrder(poll.getId());

            List<PostResponse.PollOption> pollOptions = options.stream()
                    .map(option -> {
                        PostResponse.PollOption opt = new PostResponse.PollOption();
                        opt.setId(option.getId());
                        opt.setOptionText(option.getOptionText());
                        opt.setOptionImageUrl(option.getOptionImageUrl());
                        opt.setOptionOrder(option.getOptionOrder());
                        opt.setVotesCount(option.getVotesCount());

                        double percentage = poll.getTotalVotes() > 0
                                ? (option.getVotesCount() * 100.0) / poll.getTotalVotes()
                                : 0.0;
                        opt.setPercentage(Math.round(percentage * 10.0) / 10.0);

                        // Never highlight user's vote in quoted poll
                        opt.setHasVoted(false);

                        return opt;
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
                attachedGroup.setRemainingSlots(group.getSeatsRemaining());

                if (group.getRegularPrice() != null && group.getGroupPrice() != null) {
                    BigDecimal savings = group.getRegularPrice().subtract(group.getGroupPrice());
                    attachedGroup.setSavingsAmount(savings);
                    attachedGroup.setSavingsPercentage(
                            savings.multiply(new BigDecimal("100")).divide(group.getRegularPrice(), 2, BigDecimal.ROUND_HALF_UP)
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
                if (plan.getIsActive() && plan.getProduct() != null) {
                    plans.add(mapInstallmentPlan(plan));
                }
            });
        });

        return plans;
    }

    private PostResponse.AttachedInstallmentPlan mapInstallmentPlan(InstallmentPlanEntity plan) {
        PostResponse.AttachedInstallmentPlan attached = new PostResponse.AttachedInstallmentPlan();

        ProductEntity product = plan.getProduct();
        BigDecimal productPrice = product.getPrice();

        // Plan identification
        attached.setId(plan.getPlanId());
        attached.setPlanName(plan.getPlanName());
        attached.setFeatured(plan.getIsFeatured());

        // Product info
        attached.setProductId(product.getProductId());
        attached.setProductName(product.getProductName());
        attached.setProductImageUrl(getFirstProductImage(product));
        attached.setProductPrice(productPrice);

        // Shop info
        if (plan.getShop() != null) {
            attached.setShopId(plan.getShop().getShopId());
            attached.setShopName(plan.getShop().getShopName());
        }

        // Payment schedule
        attached.setNumberOfPayments(plan.getNumberOfPayments());
        attached.setPaymentFrequency(plan.getPaymentFrequency().name());
        attached.setFrequencyDisplay(getFrequencyDisplay(plan.getPaymentFrequency()));

        // Duration
        attached.setDurationDays(plan.getCalculatedDurationDays());
        attached.setDurationDisplay(plan.getCalculatedDurationDisplay());

        // Down payment calculation
        Integer downPercent = plan.getMinDownPaymentPercent();
        attached.setDownPaymentPercent(downPercent);

        BigDecimal downPaymentAmount = productPrice
                .multiply(BigDecimal.valueOf(downPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        attached.setDownPaymentAmount(downPaymentAmount);

        // Interest
        BigDecimal apr = plan.getApr();
        attached.setApr(apr);
        attached.setInterestFree(apr.compareTo(BigDecimal.ZERO) == 0);

        // Calculate totals
        BigDecimal remainingAfterDown = productPrice.subtract(downPaymentAmount);
        BigDecimal interestAmount = calculateInterest(
                remainingAfterDown,
                apr,
                plan.getCalculatedDurationDays()
        );
        attached.setInterestAmount(interestAmount);
        attached.setTotalAmount(productPrice.add(interestAmount));

        // Amount per payment (excluding down payment)
        BigDecimal totalPayments = remainingAfterDown.add(interestAmount);
        BigDecimal amountPerPayment = totalPayments
                .divide(BigDecimal.valueOf(plan.getNumberOfPayments()), 2, RoundingMode.HALF_UP);
        attached.setAmountPerPayment(amountPerPayment);

        // Fulfillment
        attached.setFulfillmentTiming(plan.getFulfillmentTiming().name());
        attached.setFulfillmentDisplay(getFulfillmentDisplay(plan.getFulfillmentTiming()));

        // Grace period
        attached.setPaymentStartDelayDays(plan.getPaymentStartDelayDays());
        attached.setGraceDisplay(getGraceDisplay(plan.getPaymentStartDelayDays()));

        return attached;
    }

    private String getFirstProductImage(ProductEntity product) {
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            return product.getProductImages().getFirst();
        }
        return null;
    }

    private String getFrequencyDisplay(PaymentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> "per day";
            case WEEKLY -> "per week";
            case BI_WEEKLY -> "every 2 weeks";
            case SEMI_MONTHLY -> "twice a month";
            case MONTHLY -> "per month";
            case QUARTERLY -> "per quarter";
            case CUSTOM_DAYS -> "per payment";
        };
    }

    private String getFulfillmentDisplay(FulfillmentTiming timing) {
        return switch (timing) {
            case  AFTER_PAYMENT  -> "Ships after full payment";
            case IMMEDIATE -> "Ships immediately";
        };
    }

    private String getGraceDisplay(Integer days) {
        if (days == null || days == 0) {
            return "First payment today";
        }
        if (days == 1) {
            return "First payment tomorrow";
        }
        if (days == 7) {
            return "First payment in 1 week";
        }
        if (days == 14) {
            return "First payment in 2 weeks";
        }
        if (days == 30) {
            return "First payment in 1 month";
        }
        return "First payment in " + days + " days";
    }

    private BigDecimal calculateInterest(BigDecimal principal, BigDecimal apr, Integer durationDays) {
        if (apr == null || apr.compareTo(BigDecimal.ZERO) == 0 || durationDays == null) {
            return BigDecimal.ZERO;
        }

        // Simple interest: I = P × R × T
        // Where T is in years (durationDays / 365)
        BigDecimal years = BigDecimal.valueOf(durationDays)
                .divide(BigDecimal.valueOf(365), 10, RoundingMode.HALF_UP);

        BigDecimal rate = apr.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        return principal
                .multiply(rate)
                .multiply(years)
                .setScale(2, RoundingMode.HALF_UP);
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
        engagement.setQuotesCount(post.getQuotesCount());
        engagement.setRepostsCount(post.getRepostsCount());
        engagement.setQuotesCount(post.getQuotesCount());
        engagement.setBookmarksCount(post.getBookmarksCount());
        engagement.setSharesCount(0);
        engagement.setViewsCount(post.getViewsCount());
        engagement.setCanLike(true);
        engagement.setCanComment(true);
        engagement.setCanRepost(true);
        engagement.setCanShare(true);
        return engagement;
    }

    private PostResponse.QuotedPost mapQuotedPost(PostEntity post) {
        // 1. No quote at all → genuinely nothing to show
        if (post.getQuotedPostId() == null) {
            return null;
        }

        UUID quotedPostId = post.getQuotedPostId();

        // 2. Try to fetch the quoted post (respecting soft-delete)
        PostEntity quotedPost = postRepository.findByIdAndIsDeletedFalse(quotedPostId)
                .orElse(null);

        //Todo: Here we will fetch curret or null user if needed for privacy checks
   //AccountEntity currentUser = getAuthenticatedAccountOrNull();

        // 3. PRIVACY + VISIBILITY CHECKS
        // TODO: extend this block for more privacy rules (suspended account, muted, etc.)
        if (quotedPost != null) {
            if (quotedPost.getStatus() != PostStatus.PUBLISHED) {
                quotedPost = null;
            }

            // — Current user is blocked by quoted post author → hide entirely
//            else if (currentUser != null && blockRepository.existsByBlockerIdAndBlockedId(
//                    quotedPost.getAuthorId(), currentUser.getId())) {
//                quotedPost = null;
//
//            — Quoted post is from private account + current user doesn't follow → hide
//            else if (quotedPost.getAuthor().isPrivate() && currentUser != null &&
//                    !followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), quotedPost.getAuthorId())) {
//                quotedPost = null;
//            }
            // — Current user blocked the author? Still show (personal choice), or hide if you prefer
            // else if (currentUser != null && blockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), quotedPost.getAuthorId())) { quotedPost = null; }
        }

        // 4. If quoted post is unavailable for ANY reason → tombstone
        if (quotedPost == null) {
            return PostResponse.QuotedPost.builder()
                    .id(quotedPostId)
                    .content("This post is no longer available.")
                    .author(null)
                    .postType(null)
                    .status(PostStatus.DELETED)
                    .unavailable(true)
                    .build();
        }

        // 5. Fully visible & published → return rich quoted post
        PostResponse.QuotedPost response = new PostResponse.QuotedPost();
        response.setId(quotedPost.getId());
        response.setAuthor(mapAuthor(quotedPost));
        response.setContent(quotedPost.getContent());
        response.setContentParsed(mapContentParsed(quotedPost));
        response.setPostType(quotedPost.getPostType());
        response.setStatus(quotedPost.getStatus());
        response.setMedia(mapMedia(quotedPost));
        response.setCreatedAt(quotedPost.getCreatedAt().atZone(ZoneId.of("UTC")).toInstant());
        response.setPublishedAt(quotedPost.getPublishedAt().atZone(ZoneId.of("UTC")).toInstant());

        response.setEngagement(mapEngagement(quotedPost));

        if (quotedPost.getPostType() == PostType.POLL) {
            response.setPoll(mapQuotedPoll(quotedPost));
        }

        return response;
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

        // ✅ Query actual interaction tables
        interaction.setHasLiked(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        interaction.setHasBookmarked(postBookmarkRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        interaction.setHasReposted(postRepostRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        interaction.setHasCommented(commentRepository.existsByPostIdAndUserIdAndIsDeletedFalse(post.getId(), currentUserId));        interaction.setHasViewed(postViewRepository.existsByPostIdAndUserId(post.getId(), currentUserId));

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