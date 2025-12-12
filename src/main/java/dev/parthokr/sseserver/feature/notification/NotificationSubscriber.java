package dev.parthokr.sseserver.feature.notification;


import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSubscriber {

    private final Connection natsConnection;

    @Value("${nats.notificationSubject}")
    private String NOTIFICATION_SUBJECT;

    private Dispatcher dispatcher;

    @Value("${core.service.url}")
    private String CORE_SERVICE_URL;

    @Value("${core.service.apiKey}")
    private String X_API_KEY;

    private WebClient client;

    private final SseEmitterRegistry emitterRegistry;

    @PostConstruct
    public void subscribe() {

        this.client = WebClient.builder()
                .baseUrl(CORE_SERVICE_URL)
                .build();

        dispatcher = natsConnection.createDispatcher((msg) -> {
            String message = new String(msg.getData(), StandardCharsets.UTF_8);
            log.info("Received notification: {}", message);
            try {
                // check if message is a valid Long
                Long notificationId = Long.parseLong(message);
                NotificationResponse notification = client.get()
                        .uri("/api/v1/internal/notifications/{id}", notificationId)
                        .headers(h -> h.add("x-api-key", X_API_KEY))
                        .retrieve()
                        .bodyToMono(NotificationResponse.class)
                        .block();
                log.info("Fetched notification details: ID={}, Message={}, Link={}, Type={}, UserID={}",
                        notification.getData().getId(),
                        notification.getData().getMessage(),
                        notification.getData().getLink(),
                        notification.getData().getType(),
                        notification.getData().getUserId()
                );

                // create a notification event out of this
                NotificationEvent notificationEvent = NotificationEvent.builder()
                        .id(notification.getData().getId())
                        .message(notification.getData().getMessage())
                        .link(notification.getData().getLink())
                        .type(notification.getData().getType())
                        .createdAt(notification.getData().getCreatedAt())
                        .read(notification.getData().isRead())
                        .clicked(notification.getData().isClicked())
                        .build();

                // send to all emitters for this user
//                SseEmitter emitter = emitterRegistry.getEmitter(notification.getData().getUserId());
//                if (emitter != null) {
//                    SseEmitter.SseEventBuilder event = SseEmitter.event()
//                            .name("notification")
//                            .data(notificationEvent);
//                    emitter.send(event);
//                    log.info("Sent notification ID {} to user ID {} via SSE", notification.getData().getId(), notification.getData().getUserId());
//                } else {
//                    log.warn("No active SSE emitter found for user ID: {}", notification.getData().getUserId());
//                }

                emitterRegistry.forEachEmitter(notification.getData().getUserId(), emitter -> {
                    try {
                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                                .name("notification")
                                .data(notificationEvent);
                        emitter.send(event);
                        log.info("Sent notification ID {} to user ID {} via SSE", notification.getData().getId(), notification.getData().getUserId());
                    } catch (Exception e) {
                        log.error("Error sending notification ID {} to user ID {}: {}", notification.getData().getId(), notification.getData().getUserId(), e.getMessage());
                    }
                });

            } catch (NumberFormatException e) {
                log.error("Invalid notification ID received: {}", message);
            } catch (Exception e) {
                log.error("Error fetching notification details for ID {}: {}", message, e.getMessage());
            }
        });
        dispatcher.subscribe(NOTIFICATION_SUBJECT);
        log.info("Subscribed to NATS subject: {}", NOTIFICATION_SUBJECT);
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class NotificationResponse {
        private Instant timestamp;
        private Data data;
        private String path;

        @Builder
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        static class Data {
            private Long id;
            private String message;
            private String link;
            private boolean read;
            private boolean clicked;
            private String type;
            private Instant createdAt;
            private Long userId;
        }
    }
}
