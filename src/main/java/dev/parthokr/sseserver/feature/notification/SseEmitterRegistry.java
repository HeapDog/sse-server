package dev.parthokr.sseserver.feature.notification;


import dev.parthokr.sseserver.security.SecurityUser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
@Slf4j
public class SseEmitterRegistry {

    @Value("${core.service.url}")
    private String CORE_SERVICE_URL;

    @Value("${core.service.apiKey}")
    private String X_API_KEY;

    private WebClient client;

    @PostConstruct
    void initClient() {
        this.client = WebClient.builder()
                .baseUrl(CORE_SERVICE_URL)
                .build();
    }

    record HeapdogSseEmitter(SseEmitter sseEmitter, String token) {}

    private final Map<Long, Set<HeapdogSseEmitter>> emitters = new ConcurrentHashMap<>();

    @Value("${sse.emitter.timeout:3600000}") // Default timeout of 1 hour
    private long timeout;

    public SseEmitter createEmitter(Authentication authentication) {
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        Long userId = securityUser.getId();
        Object credentials = authentication.getCredentials();
        if (!(credentials instanceof String)) {
            throw new IllegalArgumentException("Invalid authentication credentials");
        }
        String token = (String) credentials;

        SseEmitter emitter = new SseEmitter(timeout);

        HeapdogSseEmitter heapdogSseEmitter = new HeapdogSseEmitter(emitter, token);

        emitters.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(heapdogSseEmitter);

        emitter.onCompletion(() -> cleanUp(userId, token));
        emitter.onError((e) -> cleanUp(userId, token));
        emitter.onTimeout(() -> cleanUp(userId, token));

        // Send heartbeat every 10 seconds to keep the connection alive
        Thread t = Thread.ofVirtual().unstarted(() -> {
            try {
                while (true) {
                    Thread.sleep(10000);
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        t.setDaemon(true);
        t.start();

        return emitter;
    }


    private void cleanUp(Long userId, String token) {

        log.info("SSE Emitter for user {} completed", userId);
        log.info("Removed emitter associated with token: {}", token);
        // Remove the emitter where token matches
        emitters.getOrDefault(userId, ConcurrentHashMap.newKeySet()).removeIf(e -> e.token().equals(token));
        client.delete()
                .uri("/api/v1/internal/sse-token/invalidate?token={token}", token)
                .headers(h -> h.add("X-Api-Key", X_API_KEY))
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.error("Failed to invalidate SSE token: {}", token, e))
                .subscribe();
    }

    public void forEachEmitter(Long userId, Consumer<SseEmitter> action) {
        Set<HeapdogSseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            for (var emitter : userEmitters) {
                action.accept(emitter.sseEmitter());
            }
        }
    }
}
