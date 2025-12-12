package dev.parthokr.sseserver.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@AllArgsConstructor
@Slf4j
public class SseTicketAuthenticationFilter extends OncePerRequestFilter {

    private final SseTicketAuthenticationService sseTicketAuthenticationService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // extract ticket from query param
        String token = request.getParameter("token");

        if (token == null || token.isEmpty()) {
            throw new SseTicketValidationFailedException("Missing token");
        }

        try {
            log.info("Received X-SSE-Ticket: {}", token);
            Authentication authentication = sseTicketAuthenticationService.verifyToken(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (SseTicketValidationFailedException e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
            return;
        }
        filterChain.doFilter(request, response);

    }
}