package org.nextgate.nextgatebackend.globesecurity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.globesecurity.configuration.ServiceAuthConfig;
import org.nextgate.nextgatebackend.globesecurity.utils.CachedBodyHttpServletRequest;
import org.nextgate.nextgatebackend.globesecurity.utils.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class ServiceAuthenticationFilter extends OncePerRequestFilter {

    private final ServiceAuthConfig serviceAuthConfig;
    private final HandlerExceptionResolver exceptionResolver;

    @Autowired
    public ServiceAuthenticationFilter(
            ServiceAuthConfig serviceAuthConfig,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.serviceAuthConfig = serviceAuthConfig;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        if (!isServiceEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!serviceAuthConfig.isEnabled()) {
            log.warn("Service authentication is disabled");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String apiKey = request.getHeader("X-Service-Key");
            String timestamp = request.getHeader("X-Timestamp");
            String signature = request.getHeader("X-Signature");

            if (apiKey == null || timestamp == null || signature == null) {
                throw new SecurityException("Missing service authentication headers");
            }

            String secretKey = serviceAuthConfig.getSecretKeyForApiKey(apiKey);
            if (secretKey == null) {
                throw new SecurityException("Invalid API key");
            }

            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            String body = new String(cachedRequest.getCachedBody(), StandardCharsets.UTF_8);

            HmacUtils.validateRequest(signature, timestamp, body, requestUri, secretKey,
                    serviceAuthConfig.getTimestampToleranceMinutes());

            String serviceName = serviceAuthConfig.getServiceNameForApiKey(apiKey);
            log.info("Service auth success: {} -> {}", serviceName, requestUri);

            filterChain.doFilter(cachedRequest, response);

        } catch (Exception e) {
            log.error("Service auth failed: {}", e.getMessage());
            exceptionResolver.resolveException(request, response, null, e);
        }
    }

    private boolean isServiceEndpoint(String uri) {
        return uri.startsWith("/api/v1/notifications/in-app");
    }
}