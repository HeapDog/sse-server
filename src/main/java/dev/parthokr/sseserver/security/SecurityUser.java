package dev.parthokr.sseserver.security;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


@Builder
@Getter
@Setter
public class SecurityUser {
    private Long id;
    private String username;
    private Collection<? extends GrantedAuthority> authorities;
    private Boolean enabled;
}
