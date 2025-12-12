package dev.parthokr.sseserver.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SseTicketAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final Object token;

    public SseTicketAuthenticationToken(Object principal, Object token, Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
        super(authorities);
        this.principal = principal;
        this.token = token;
        setAuthenticated(authenticated);
    }

    public static SseTicketAuthenticationToken authenticated(SecurityUser principal, Object token, Collection<? extends GrantedAuthority> authorities) {
        return new SseTicketAuthenticationToken(principal, token, authorities, true);
    }


    public static SseTicketAuthenticationToken unauthenticated(Object token) {
        return new SseTicketAuthenticationToken(null, token, null, false);
    }

    @Override
    public Object getCredentials() {
        return this.token;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
