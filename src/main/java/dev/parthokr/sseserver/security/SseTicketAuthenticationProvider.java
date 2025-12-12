package dev.parthokr.sseserver.security;

import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseTicketAuthenticationProvider implements AuthenticationProvider {
    private WebClient client;

    @Value("${core.service.url}")
    private String CORE_SERVICE_URL;


    @Value("${core.service.apiKey}")
    private String X_API_KEY;

    @PostConstruct
    void initClient() {
        this.client = WebClient.builder()
                .baseUrl(CORE_SERVICE_URL)
                .build();
    }


    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = authentication.getCredentials().toString();
        try {
            // check if token is valid
            SseTokenValidateResponse tokenValidateResponse = client.get()
                    .uri("/api/v1/internal/sse-token/validate?token={token}", token)
                    .headers(h -> h.add("X-Api-Key", X_API_KEY))
                    .retrieve()
                    .bodyToMono(SseTokenValidateResponse.class)
                    .block();

            if (tokenValidateResponse == null || tokenValidateResponse.getData() == null) {
                throw new SseTicketValidationFailedException("Invalid token");
            }

            log.info("SSE token validated for user ID: {}", tokenValidateResponse.getData().getUserId());

            // fetch user details
            Long userId = tokenValidateResponse.getData().getUserId();
            UserResponse resp = client.get()
                    .uri("/api/v1/internal/users/{id}", userId)
                    .headers(h -> h.add("X-Api-Key", X_API_KEY))
                    .retrieve()
                    .bodyToMono(UserResponse.class)
                    .block();

            SecurityUser securityUser = SecurityUser.builder()
                    .id(resp.getData().getId())
                    .username(resp.getData().getUsername())
                    .authorities(AuthorityUtils.createAuthorityList(resp.getData().getRole()))
                    .enabled(resp.getData().isEnabled())
                    .authorities(null)
                    .build();
            return SseTicketAuthenticationToken.authenticated(securityUser, token, securityUser.getAuthorities());
        } catch (Exception e) {
            log.error("SSE token validation failed for user ID: {}", token, e);
//            throw new BadCredentialsException("SSE token validation failed for user ID: " + token, e);
            throw new SseTicketValidationFailedException("Failed to validate SSE token");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SseTicketAuthenticationToken.class.isAssignableFrom(authentication);
    }


    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class SseTokenValidateResponse {
        private Instant timestamp;
        private _Data data;
        private String path;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        static class _Data {
            private Long userId;
        }

    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    static class UserResponse {

        private Instant timestamp;
        private _Data data;
        private String path;

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        static class _Data {
            private Long id;
            private String username;
            private String role;
            private boolean enabled;
        }

    }
}
