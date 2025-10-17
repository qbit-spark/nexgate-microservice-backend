package org.nextgate.nextgatebackend.globesecurity.configuration;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.globesecurity.JWTAuthFilter;
import org.nextgate.nextgatebackend.globesecurity.ServiceAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@RequiredArgsConstructor
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver exceptionResolver;

    private final ServiceAuthenticationFilter serviceAuthenticationFilter;

    @Bean
    public JWTAuthFilter jwtAuthenticationFilter() {
        return new JWTAuthFilter(exceptionResolver);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/**").permitAll()

                        // Service endpoint - validated by ServiceAuthenticationFilter
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications/in-app").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/categories/all").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/categories/all-paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/all-paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/all").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/featured").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/featured-paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/summary-stats").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/category/{categoryId}/paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/category/{categoryId}").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/reviews/{shopId}/active-reviews-by-shop").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/categories/all").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/categories/all-paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/categories/parent-categories").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/categories/parent/{parentId}/children").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/categories/{categoryId}").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/products/find-by-slug/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/products/{productId}").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/products/public-view/all").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/products/public-view/all-paged").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/products/search").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/v1/shops/{shopId}/products/advanced-filter").permitAll()

                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add filters: ServiceAuth BEFORE JWT
        httpSecurity.addFilterBefore(serviceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }
}