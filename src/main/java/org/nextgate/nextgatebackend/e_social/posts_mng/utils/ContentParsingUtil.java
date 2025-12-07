package org.nextgate.nextgatebackend.e_social.posts_mng.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ContentParsingUtil {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([a-zA-Z0-9_]+)");        // Correct — hashtags rarely allow -
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_.-]+)");      // Perfect — allows hyphens & dots
    private static final Pattern SHOP_MENTION_PATTERN = Pattern.compile("\\$([a-zA-Z0-9_.-]+)"); // Perfect — same as mentions

    // Parse @mentions from content
    public List<ParsedMention> parseMentions(String content) {
        List<ParsedMention> mentions = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return mentions;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            ParsedMention mention = new ParsedMention();
            mention.setUserName(matcher.group(1));
            mention.setStartIndex(matcher.start());
            mention.setEndIndex(matcher.end());
            mentions.add(mention);
        }

        return mentions;
    }

    // Parse #hashtags from content
    public List<ParsedHashtag> parseHashtags(String content) {
        List<ParsedHashtag> hashtags = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return hashtags;
        }

        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            ParsedHashtag hashtag = new ParsedHashtag();
            hashtag.setHashtag(matcher.group(1).toLowerCase());
            hashtag.setStartIndex(matcher.start());
            hashtag.setEndIndex(matcher.end());
            hashtags.add(hashtag);
        }

        return hashtags;
    }

    // Parse $shop mentions from content
    public List<ParsedShopMention> parseShopMentions(String content) {
        List<ParsedShopMention> shopMentions = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return shopMentions;
        }

        Matcher matcher = SHOP_MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            ParsedShopMention shopMention = new ParsedShopMention();
            shopMention.setShopSlug(matcher.group(1));
            shopMention.setStartIndex(matcher.start());
            shopMention.setEndIndex(matcher.end());
            shopMentions.add(shopMention);
        }

        return shopMentions;
    }

    // Inner classes for parsed data
    @Setter
    @Getter
    public static class ParsedMention {
        private String userName;
        private int startIndex;
        private int endIndex;

    }

    @Setter
    @Getter
    public static class ParsedHashtag {
        private String hashtag;
        private int startIndex;
        private int endIndex;

    }

    @Setter
    @Getter
    public static class ParsedShopMention {
        private String shopSlug;
        private int startIndex;
        private int endIndex;

    }
}