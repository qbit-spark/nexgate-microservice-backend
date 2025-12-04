package org.nextgate.nextgatebackend.e_social.posts_mng.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class LinkProcessingUtil {

    private static final String SHORT_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int SHORT_CODE_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Generate unique short code for URL
    public String generateShortCode() {
        StringBuilder code = new StringBuilder(SHORT_CODE_LENGTH);
        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            code.append(SHORT_CODE_CHARS.charAt(RANDOM.nextInt(SHORT_CODE_CHARS.length())));
        }
        return code.toString();
    }

    // Extract domain from URL
    public String extractDomain(String url) {
        try {
            String domain = url.replaceFirst("^(https?://)?(www\\.)?", "");
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }
            return domain;
        } catch (Exception e) {
            return url;
        }
    }

    // Check if URL is potentially unsafe (basic check)
    public boolean isUrlSafe(String url) {
        String lowerUrl = url.toLowerCase();

        String[] unsafePatterns = {
                "malware", "phishing", "spam", "scam",
                "bit.ly/", "tinyurl.com/", "goo.gl/" // Known URL shorteners (could be abused)
        };

        for (String pattern : unsafePatterns) {
            if (lowerUrl.contains(pattern)) {
                return false;
            }
        }

        return true;
    }

    // Generate short URL
    public String generateShortUrl(String shortCode) {
        return "https://nexgate.it/" + shortCode;
    }
}