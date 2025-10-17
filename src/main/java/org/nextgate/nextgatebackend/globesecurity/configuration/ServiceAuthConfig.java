package org.nextgate.nextgatebackend.globesecurity.configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "service.auth")
public class ServiceAuthConfig {

    private boolean enabled = true;
    private int timestampToleranceMinutes = 5;
    private Map<String, ServiceCredentials> services = new HashMap<>();

    @Data
    public static class ServiceCredentials {
        private String apiKey;
        private String secretKey;
    }

    @PostConstruct
    public void init() {
        if (services.isEmpty()) {
            log.warn("No services configured for authentication");
        } else {
            log.info("Service auth enabled: {} services registered", services.size());
        }
    }

    public String getSecretKeyForApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }

        return services.values().stream()
                .filter(creds -> apiKey.equals(creds.getApiKey()))
                .map(ServiceCredentials::getSecretKey)
                .findFirst()
                .orElse(null);
    }

    public String getServiceNameForApiKey(String apiKey) {
        if (apiKey == null) return "unknown";

        return services.entrySet().stream()
                .filter(entry -> apiKey.equals(entry.getValue().getApiKey()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("unknown");
    }
}