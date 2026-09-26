package com.example.ssms.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final Role role;
    private final AccountStatus status;
    private final int permVersion;
    private final UUID sessionId;
    private final boolean mustChangePassword;
    private final Instant tokenIssuedAt;
    private final ProfileType profileType;
    private final UUID profileId;

    public UserPrincipal(
            UUID id,
            String email,
            Role role,
            AccountStatus status,
            int permVersion,
            UUID sessionId,
            boolean mustChangePassword,
            Instant tokenIssuedAt,
            ProfileType profileType,
            UUID profileId
    ) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.status = status;
        this.permVersion = permVersion;
        this.sessionId = sessionId;
        this.mustChangePassword = mustChangePassword;
        this.tokenIssuedAt = tokenIssuedAt;
        this.profileType = profileType;
        this.profileId = profileId;
    }

    public UUID getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public int getPermVersion() {
        return permVersion;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public Instant getTokenIssuedAt() {
        return tokenIssuedAt;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public UUID getProfileId() {
        return profileId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isStepUpValid(long windowSeconds) {
        if (tokenIssuedAt == null) return false;
        return Instant.now().minusSeconds(windowSeconds).isBefore(tokenIssuedAt);
    }
}
