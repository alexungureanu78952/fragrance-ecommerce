package com.fragrance.ecommerce.security;

import com.fragrance.ecommerce.domain.User;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthenticatedUser(UUID id, String email, String passwordHash, boolean enabled,
                                Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
            user.getId(),
            user.getEmail(),
            user.getPasswordHash(),
            user.isEnabled(),
            user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .toList()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
