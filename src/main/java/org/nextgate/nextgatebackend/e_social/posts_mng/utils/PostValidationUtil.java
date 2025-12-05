package org.nextgate.nextgatebackend.e_social.posts_mng.utils;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.*;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class PostValidationUtil {

    private static final int MAX_CONTENT_LENGTH = 5000;
    private static final int MAX_MEDIA_PER_POST = 10;
    private static final int MAX_POLL_OPTIONS = 10;
    private static final int MIN_POLL_OPTIONS = 2;
    private static final int MAX_COLLABORATORS = 5;
    private static final int MAX_PRODUCTS_PER_POST = 10;
    private static final int MAX_SHOPS_PER_POST = 5;
    private static final int MAX_EVENTS_PER_POST = 3;

    // Validates entire create post request
    public void validateCreatePostRequest(CreatePostRequest request) {
        validateCreatePostRequest(request, false); // Default: not strict
    }

    // Validates with strict mode option
    public void validateCreatePostRequest(CreatePostRequest request, boolean strict) {
        validatePostType(request, strict);

        if (strict) {
            validateContent(request.getContent(), request.getMedia());
        }

        if (request.getMedia() != null && !request.getMedia().isEmpty()) {
            validateMedia(request.getMedia());
        }

        if (request.getPoll() != null) {
            validatePoll(request.getPoll(), request.getPostType(), strict);
        }

        if (request.getAttachments() != null) {
            validateAttachments(request.getAttachments());
        }

        if (request.getCollaboration() != null) {
            validateCollaboration(request.getCollaboration());
        }

        if (request.getScheduledAt() != null) {
            validateScheduledTime(request.getScheduledAt());
        }
    }

    // Validates post ready for publishing
    public void validateForPublish(PostEntity post) {
        boolean hasContent = post.getContent() != null && !post.getContent().trim().isEmpty();

        if (!hasContent) {
            throw new IllegalArgumentException("Post content is required before publishing");
        }

        // Poll posts are now fully supported - no error needed
    }

    // Validates post type matches data (poll posts must have poll data)
    private void validatePostType(CreatePostRequest request, boolean strict) {
        if (request.getPostType() == null) {
            throw new IllegalArgumentException("Post type is required");
        }

        if (strict && request.getPostType() == PostType.POLL && request.getPoll() == null) {
            throw new IllegalArgumentException("Poll data is required for poll posts");
        }

        if (request.getPostType() == PostType.REGULAR && request.getPoll() != null) {
            throw new IllegalArgumentException("Regular posts cannot have poll data");
        }
    }

    // Validates post has either content or media
    public void validateContent(String content, List<MediaRequest> media) {
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasMedia = media != null && !media.isEmpty();

        if (!hasContent && !hasMedia) {
            throw new IllegalArgumentException("Post must have either content or media");
        }

        if (content != null) {
            validateContentLength(content);
        }
    }

    // Validates content length limit
    public void validateContentLength(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }
    }

    // Validates media count and URLs
    public void validateMedia(List<MediaRequest> media) {
        if (media.size() > MAX_MEDIA_PER_POST) {
            throw new IllegalArgumentException("Maximum " + MAX_MEDIA_PER_POST + " media items allowed per post");
        }

        for (MediaRequest mediaRequest : media) {
            validateMediaUrl(mediaRequest.getMediaUrl());
        }
    }

    // Validates media URL format
    public void validateMediaUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Media URL cannot be empty");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid media URL format");
        }
    }

    // Validates poll structure and options
    public void validatePoll(PollRequest poll, PostType postType, boolean strict) {
        if (postType != PostType.POLL) {
            throw new IllegalArgumentException("Poll data only allowed for poll posts");
        }

        if (strict) {
            if (poll.getTitle() == null || poll.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Poll title is required");
            }

            validatePollOptions(poll.getOptions());
        }

        if (poll.getExpiresAt() != null && poll.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Poll expiry date must be in the future");
        }
    }

    // Validates poll options count and text
    public void validatePollOptions(List<PollOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Poll options are required");
        }

        if (options.size() < MIN_POLL_OPTIONS) {
            throw new IllegalArgumentException("Poll must have at least " + MIN_POLL_OPTIONS + " options");
        }

        if (options.size() > MAX_POLL_OPTIONS) {
            throw new IllegalArgumentException("Poll cannot have more than " + MAX_POLL_OPTIONS + " options");
        }

        for (PollOptionRequest option : options) {
            if (option.getOptionText() == null || option.getOptionText().trim().isEmpty()) {
                throw new IllegalArgumentException("Poll option text cannot be empty");
            }
        }
    }

    // Validates attachment limits
    public void validateAttachments(AttachmentsRequest attachments) {
        if (attachments.getProductIds() != null && attachments.getProductIds().size() > MAX_PRODUCTS_PER_POST) {
            throw new IllegalArgumentException("Maximum " + MAX_PRODUCTS_PER_POST + " products allowed per post");
        }

        if (attachments.getShopIds() != null && attachments.getShopIds().size() > MAX_SHOPS_PER_POST) {
            throw new IllegalArgumentException("Maximum " + MAX_SHOPS_PER_POST + " shops allowed per post");
        }

        if (attachments.getEventIds() != null && attachments.getEventIds().size() > MAX_EVENTS_PER_POST) {
            throw new IllegalArgumentException("Maximum " + MAX_EVENTS_PER_POST + " events allowed per post");
        }

        if (attachments.getExternalLink() != null) {
            validateExternalLink(attachments.getExternalLink().getUrl());
        }
    }

    // Validates external link URL format
    public void validateExternalLink(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("External link URL cannot be empty");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid external link URL format");
        }
    }

    // Validates collaboration settings
    public void validateCollaboration(CollaborationRequest collaboration) {
        if (collaboration.getIsCollaborative() &&
                (collaboration.getCollaboratorIds() == null || collaboration.getCollaboratorIds().isEmpty())) {
            throw new IllegalArgumentException("Collaborative posts must have at least one collaborator");
        }

        if (collaboration.getCollaboratorIds() != null &&
                collaboration.getCollaboratorIds().size() > MAX_COLLABORATORS) {
            throw new IllegalArgumentException("Maximum " + MAX_COLLABORATORS + " collaborators allowed per post");
        }
    }

    // Validates scheduled time is in future
    public void validateScheduledTime(LocalDateTime scheduledAt) {
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }
    }
}