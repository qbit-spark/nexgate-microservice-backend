package org.nextgate.nextgatebackend.e_social.posts_mng.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {

    private UUID id;
    private Author author;
    private String content;
    private ContentParsed contentParsed;
    private PostType postType;
    private PostStatus status;
    private List<Media> media = new ArrayList<>();
    private Poll poll;
    private Attachments attachments;
    private Collaboration collaboration;
    private PrivacySettings privacySettings;
    private Engagement engagement;
    private UserInteraction userInteraction;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime scheduledAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Author {
        private UUID id;
        private String userName;
        private String firstName;
        private String lastName;
        private String profilePictureUrl;
        private boolean isVerified;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContentParsed {
        private String text;
        private List<ContentEntity> entities = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContentEntity {
        private ContentEntityType type;
        private String text;
        private int startIndex;
        private int endIndex;
        private User user;
        private String hashtag;
        private Shop shop;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class User {
        private UUID id;
        private String userName;
        private String firstName;
        private String lastName;
        private String profilePictureUrl;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Shop {
        private UUID id;
        private String shopName;
        private String shopSlug;
        private String logoUrl;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Media {
        private String id;
        private MediaType mediaType;
        private String originalUrl;
        private String thumbnailUrl;
        private String placeholderBase64;
        private Integer width;
        private Integer height;
        private Integer duration;
        private int order;
        private List<ImageTag> imageTags = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageTag {
        private String id;
        private ImageTagType tagType;
        private double xPosition;
        private double yPosition;
        private User user;
        private Product product;
        private Shop shop;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Product {
        private UUID id;
        private String name;
        private String imageUrl;
        private Double price;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Poll {
        private UUID id;
        private String title;
        private String description;
        private List<PollOption> options = new ArrayList<>();
        private long totalVotes;
        private boolean allowMultipleVotes;
        private boolean isAnonymous;
        private LocalDateTime expiresAt;
        private boolean hasExpired;
        private boolean userHasVoted;
        private List<UUID> userVotedOptions = new ArrayList<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PollOption {
        private UUID id;
        private String optionText;
        private String optionImageUrl;
        private int optionOrder;
        private long votesCount;
        private double percentage;
        private boolean hasVoted;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attachments {
        private List<AttachedProduct> products = new ArrayList<>();
        private List<AttachedShop> shops = new ArrayList<>();
        private List<AttachedBuyTogetherGroup> buyTogetherGroups = new ArrayList<>();
        private List<AttachedInstallmentPlan> installmentPlans = new ArrayList<>();
        private List<AttachedEvent> events = new ArrayList<>();
        private ExternalLink externalLink;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachedProduct {
        private UUID id;
        private String name;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private String imageUrl;
        private String shopName;
        private UUID shopId;
        private boolean inStock;
        private SocialContext socialContext;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachedShop {
        private UUID id;
        private String name;
        private String logoUrl;
        private String description;
        private Double rating;
        private Integer totalProducts;
        private boolean isVerified;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachedBuyTogetherGroup {
        private UUID id;
        private String productName;
        private String productImageUrl;
        private int currentCount;
        private int totalSlots;
        private int remainingSlots;
        private BigDecimal originalPrice;
        private BigDecimal discountPrice;
        private BigDecimal savingsAmount;
        private BigDecimal savingsPercentage;
        private LocalDateTime expiresAt;
        private String status;
        private SocialContext socialContext;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachedInstallmentPlan {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productImageUrl;
        private Double monthlyAmount;
        private int duration;
        private Double totalAmount;
        private Double interestRate;
        private Double downPayment;
        private boolean isInterestFree;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachedEvent {
        private UUID id;
        private String title;
        private String description;
        private String imageUrl;
        private LocalDateTime date;
        private LocalDateTime endDate;
        private String location;
        private String address;
        private Double ticketPrice;
        private boolean ticketsAvailable;
        private Integer attendeesCount;
        private Integer maxAttendees;
        private SocialContext socialContext;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ExternalLink {
        private UUID id;
        private String originalUrl;
        private String shortUrl;
        private String shortCode;
        private LinkPreview preview;
        private boolean isValidated;
        private boolean isSafe;
        private long clickCount;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LinkPreview {
        private String title;
        private String description;
        private String imageUrl;
        private String domain;
        private String favicon;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocialContext {
        private int friendsCount;
        private List<Friend> friends = new ArrayList<>();
        private String message;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Friend {
        private UUID id;
        private String userName;
        private String profilePictureUrl;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Collaboration {
        private boolean isCollaborative;
        private List<Collaborator> collaborators = new ArrayList<>();
        private String byline;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Collaborator {
        private UUID id;
        private User user;
        private CollaboratorStatus status;
        private LocalDateTime invitedAt;
        private LocalDateTime respondedAt;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PrivacySettings {
        private PostVisibility visibility;
        private CommentPermission whoCanComment;
        private RepostPermission whoCanRepost;
        private boolean hideLikesCount;
        private boolean hideCommentsCount;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Engagement {
        private long likesCount;
        private long commentsCount;
        private long repostsCount;
        private long quotesCount;
        private long bookmarksCount;
        private long sharesCount;
        private long viewsCount;
        private boolean canLike;
        private boolean canComment;
        private boolean canRepost;
        private boolean canShare;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInteraction {
        private boolean hasLiked;
        private boolean hasBookmarked;
        private boolean hasReposted;
        private boolean hasCommented;
        private boolean hasViewed;
    }
}