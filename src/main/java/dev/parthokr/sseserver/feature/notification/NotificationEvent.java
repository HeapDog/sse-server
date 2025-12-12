package dev.parthokr.sseserver.feature.notification;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;


@Builder
@Getter
@Setter
public class NotificationEvent {
    private Long id;
    private String message;
    private String link;
    private boolean read;
    private boolean clicked;
    private String type;
    private Instant createdAt;
}
