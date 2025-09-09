package org.nextgate.nextgatebackend.user_profile_service.utils;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globe_api_client.GlobalApiClientGate;
import org.nextgate.nextgatebackend.globe_api_client.payloads.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class SecurityInfoUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityInfoUtils.class);


    private final GlobalApiClientGate apiClient;

    // SECURITY: Whitelist of allowed HTTPS geolocation APIs only
    private static final Set<String> ALLOWED_GEOLOCATION_DOMAINS = Set.of(
            "ipinfo.io",
            "ipapi.co",
            "api.ipgeolocation.io"
    );

    // SECURITY: Pattern to validate public IP addresses only
    private static final Pattern PUBLIC_IPV4_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    // Headers to check for real IP address
    private static final String[] IP_HEADER_NAMES = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED", "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR", "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
    };

    /**
     * Extract complete security information from HTTP request
     */
    public SecurityInfo extractSecurityInfo(HttpServletRequest request) {
        SecurityInfo securityInfo = new SecurityInfo();

        try {
            String ipAddress = getClientIpAddress(request);
            securityInfo.setIpAddress(ipAddress);

            String userAgent = request.getHeader("User-Agent");
            String deviceInfo = parseDeviceInfo(userAgent);
            securityInfo.setDeviceInfo(deviceInfo);

            securityInfo.setRequestTime(LocalDateTime.now());

            String location = getLocationFromIpSecure(ipAddress);
            securityInfo.setLocation(location);

            log.debug("Security info extracted: IP={}, Device={}, Location={}",
                    maskIpAddress(ipAddress), deviceInfo, location);

        } catch (Exception e) {
            log.warn("Error extracting security information", e);
            securityInfo.setIpAddress("Unknown");
            securityInfo.setDeviceInfo("Unknown Device");
            securityInfo.setLocation("Unknown Location");
            securityInfo.setRequestTime(LocalDateTime.now());
        }

        return securityInfo;
    }

    /**
     * Get a client IP address considering proxy headers
     */
    public String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "Unknown";

        for (String headerName : IP_HEADER_NAMES) {
            String ip = request.getHeader(headerName);
            if (isValidIp(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "Unknown";
    }

    /**
     * Parse device info from User-Agent
     */
    public String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "Unknown Device";

        try {
            if (userAgent.contains("Mobile")) {
                if (userAgent.contains("iPhone")) return "iPhone";
                if (userAgent.contains("Android")) return "Android Phone";
                return "Mobile Device";
            }
            if (userAgent.contains("iPad")) return "iPad";
            if (userAgent.contains("Tablet")) return "Tablet";

            // Desktop browsers
            if (userAgent.contains("Chrome")) return "Chrome";
            if (userAgent.contains("Firefox")) return "Firefox";
            if (userAgent.contains("Safari")) return "Safari";
            if (userAgent.contains("Edge")) return "Edge";

            return "Desktop Browser";
        } catch (Exception e) {
            return "Unknown Device";
        }
    }

    /**
     * SECURE: Get location with SSRF protection
     */
    public String getLocationFromIpSecure(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return "Unknown Location";
        }

        String cleanIp = ipAddress.trim();

        if (isPrivateOrLocalIp(cleanIp)) {
            return "Local/Private Network";
        }

        if (!isValidPublicIp(cleanIp)) {
            log.warn("Invalid or suspicious IP format for geolocation: {}", maskIpAddress(cleanIp));
            return "Invalid IP Format";
        }

        if (!isWithinRateLimit(cleanIp)) {
            log.warn("Rate limit exceeded for IP geolocation: {}", maskIpAddress(cleanIp));
            return "Rate Limited";
        }

        return callGeolocationApiSecurely(cleanIp);
    }

    /**
     * SECURITY: Validate IP is a proper public IPv4 address
     */
    private boolean isValidPublicIp(String ip) {
        if (!PUBLIC_IPV4_PATTERN.matcher(ip).matches()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;

        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);

            // Block reserved ranges
            if (first == 0 || first == 127 || first >= 224) return false;
            if (first == 10) return false;
            if (first == 172 && second >= 16 && second <= 31) return false;
            if (first == 192 && second == 168) return false;
            if (first == 169 && second == 254) return false;

            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * SECURITY: Call geolocation API with strict HTTPS controls using GlobalApiClientGate
     */
    private String callGeolocationApiSecurely(String ipAddress) {
        String location = tryIpInfoIo(ipAddress);
        if (!"Unknown Location".equals(location)) return location;

        location = tryIpApiCo(ipAddress);
        if (!"Unknown Location".equals(location)) return location;

        location = tryIpGeolocationIo(ipAddress);
        if (!"Unknown Location".equals(location)) return location;

        return "Unknown Location";
    }

    /**
     * Try ipinfo.io using GlobalApiClientGate
     */
    private String tryIpInfoIo(String ipAddress) {
        try {
            String url = "https://ipinfo.io/" + ipAddress + "/json";

            if (!isSecureUrl(url)) {
                log.warn("URL security check failed: {}", url);
                return "Unknown Location";
            }

            ApiResponse<Map> response = apiClient.get(url, Map.class);

            if (response.isSuccess() && response.getData() != null) {
                Map<String, Object> data = response.getData();

                if (!data.containsKey("error")) {
                    String city = sanitizeLocationString((String) data.get("city"));
                    String country = sanitizeLocationString((String) data.get("country"));

                    return formatLocationSecurely(city, country);
                }
            } else {
                log.debug("ipinfo.io API error: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            log.debug("ipinfo.io lookup failed for IP {}: {}",
                    maskIpAddress(ipAddress), e.getMessage());
        }
        return "Unknown Location";
    }

    /**
     * Try ipapi.co using GlobalApiClientGate
     */
    private String tryIpApiCo(String ipAddress) {
        try {
            String url = "https://ipapi.co/" + ipAddress + "/json/";

            if (!isSecureUrl(url)) {
                log.warn("URL security check failed: {}", url);
                return "Unknown Location";
            }

            ApiResponse<Map> response = apiClient.get(url, Map.class);

            if (response.isSuccess() && response.getData() != null) {
                Map<String, Object> data = response.getData();

                if (!data.containsKey("error")) {
                    String city = sanitizeLocationString((String) data.get("city"));
                    String country = sanitizeLocationString((String) data.get("country_name"));

                    return formatLocationSecurely(city, country);
                }
            } else {
                log.debug("ipapi.co API error: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            log.debug("ipapi.co lookup failed for IP {}: {}",
                    maskIpAddress(ipAddress), e.getMessage());
        }
        return "Unknown Location";
    }

    /**
     * Try geolocation.io using GlobalApiClientGate
     */
    private String tryIpGeolocationIo(String ipAddress) {
        try {
            String url = "https://api.ipgeolocation.io/ipgeo?apiKey=free&ip=" + ipAddress;

            if (!isSecureUrl(url)) {
                log.warn("URL security check failed: {}", url);
                return "Unknown Location";
            }

            ApiResponse<Map> response = apiClient.get(url, Map.class);

            if (response.isSuccess() && response.getData() != null) {
                Map<String, Object> data = response.getData();

                String city = sanitizeLocationString((String) data.get("city"));
                String country = sanitizeLocationString((String) data.get("country_name"));

                return formatLocationSecurely(city, country);
            } else {
                log.debug("ipgeolocation.io API error: {}", response.getErrorMessage());
            }
        } catch (Exception e) {
            log.debug("ipgeolocation.io lookup failed for IP {}: {}",
                    maskIpAddress(ipAddress), e.getMessage());
        }
        return "Unknown Location";
    }

    /**
     * SECURITY: Validate URL is HTTPS and safe
     */
    private boolean isSecureUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        String lowerUrl = url.toLowerCase().trim();

        if (!lowerUrl.startsWith("https://")) {
            log.warn("Non-HTTPS URL blocked: {}", url);
            return false;
        }

        boolean domainAllowed = ALLOWED_GEOLOCATION_DOMAINS.stream()
                .anyMatch(domain -> lowerUrl.contains(domain));

        if (!domainAllowed) {
            log.warn("Non-whitelisted domain blocked: {}", url);
            return false;
        }

        if (lowerUrl.contains("localhost") ||
                lowerUrl.contains("127.0.0.1") ||
                lowerUrl.contains("192.168.") ||
                lowerUrl.contains("10.") ||
                lowerUrl.contains("172.") ||
                lowerUrl.contains("@") ||
                lowerUrl.contains("..")) {
            log.warn("Suspicious URL pattern blocked: {}", url);
            return false;
        }

        return true;
    }

    /**
     * SECURITY: Rate limiting check
     */
    private boolean isWithinRateLimit(String ipAddress) {
        // Implement rate-limiting logic here if needed
        return true;
    }

    /**
     * SECURITY: Sanitize location strings from API responses
     */
    private String sanitizeLocationString(String location) {
        if (location == null) return null;

        String sanitized = location.replaceAll("[<>\"'&]", "").trim();

        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }

        return sanitized.isEmpty() ? null : sanitized;
    }

    /**
     * SECURITY: Format location safely
     */
    private String formatLocationSecurely(String city, String country) {
        if (city != null && !city.isEmpty() && country != null && !country.isEmpty()) {
            return city + ", " + country;
        } else if (country != null && !country.isEmpty()) {
            return country;
        }
        return "Unknown Location";
    }

    /**
     * Check if IP is valid
     */
    private boolean isValidIp(String ip) {
        return ip != null && !ip.trim().isEmpty() &&
                !"unknown".equalsIgnoreCase(ip.trim()) &&
                !"null".equalsIgnoreCase(ip.trim());
    }

    /**
     * Check if IP is private/local
     */
    private boolean isPrivateOrLocalIp(String ipAddress) {
        if (ipAddress == null) return false;

        String ip = ipAddress.trim();
        return ip.equals("127.0.0.1") || ip.equals("::1") || ip.equals("localhost") ||
                ip.startsWith("192.168.") || ip.startsWith("10.") ||
                ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
                ip.startsWith("172.2") || ip.startsWith("172.30.") || ip.startsWith("172.31.");
    }

    /**
     * Mask IP for privacy
     */
    public String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) return "Unknown";

        try {
            String ip = ipAddress.trim();
            if (ip.contains(".")) {
                String[] parts = ip.split("\\.");
                if (parts.length == 4) {
                    return parts[0] + "." + parts[1] + ".*.***";
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "***.***.***";
    }

    /**
     * Security information container
     */
    public static class SecurityInfo {
        private String ipAddress;
        private String deviceInfo;
        private LocalDateTime requestTime;
        private String location;

        public SecurityInfo() {}

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getDeviceInfo() { return deviceInfo; }
        public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }

        public LocalDateTime getRequestTime() { return requestTime; }
        public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getFormattedRequestTime() {
            if (requestTime == null) return "Unknown";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss");
            return requestTime.format(formatter);
        }

        public String getMaskedIpAddress() {
            if (ipAddress == null || ipAddress.trim().isEmpty()) return "Unknown";
            try {
                String ip = ipAddress.trim();
                if (ip.contains(".")) {
                    String[] parts = ip.split("\\.");
                    if (parts.length == 4) {
                        return parts[0] + "." + parts[1] + ".*.***";
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            return "***.***.***";
        }
    }
}