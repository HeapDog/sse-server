package dev.parthokr.sseserver.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SseTicketAuthenticationService {

    private final AuthenticationManager authenticationManager;

    public SseTicketAuthenticationToken verifyToken(String token) throws SseTicketValidationFailedException {
        SseTicketAuthenticationToken unauthenticated = SseTicketAuthenticationToken.unauthenticated(token);
        return (SseTicketAuthenticationToken) authenticationManager.authenticate(unauthenticated);
    }
}
