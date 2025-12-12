package dev.parthokr.sseserver.security;

import org.springframework.security.authentication.BadCredentialsException;

public class SseTicketValidationFailedException extends BadCredentialsException {
    public SseTicketValidationFailedException(String msg) {
        super(msg);
    }
}
