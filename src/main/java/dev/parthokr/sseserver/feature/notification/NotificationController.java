package dev.parthokr.sseserver.feature.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterRegistry emitterRegistry;

    @GetMapping("/subscribe")
    @CrossOrigin(origins = "*")
    SseEmitter subscribeToNotifications(Authentication authentication) throws IOException {
        SseEmitter sseEmitter = emitterRegistry.createEmitter(authentication);
        sseEmitter.send(SseEmitter.event().name("connection").data("Connected to notification stream"));
        return sseEmitter;
    }

}
