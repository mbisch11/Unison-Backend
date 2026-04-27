package com._bit.Unison.moderation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "admin_invites")
public class AdminInvite {

    @Id
    private String inviteId;
    @Indexed(unique = true)
    private String tokenHash;
    private String email;
    private Set<String> rolesToGrant;
    private Instant createdAt;
    private Instant expiresAt;
    private String createdByUserId;
    private Instant usedAt;
    private String usedByUserId;
    private Instant revokedAt;
    private boolean bootstrap;

    public AdminInvite() {
    }

    public AdminInvite(
            String tokenHash,
            String email,
            Set<String> rolesToGrant,
            Instant expiresAt,
            String createdByUserId,
            boolean bootstrap
    ) {
        this.tokenHash = tokenHash;
        this.email = email;
        this.rolesToGrant = rolesToGrant == null ? new HashSet<>() : new HashSet<>(rolesToGrant);
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.createdByUserId = createdByUserId;
        this.bootstrap = bootstrap;
    }

    public String getInviteId() {
        return inviteId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getRolesToGrant() {
        return rolesToGrant == null ? Set.of() : new HashSet<>(rolesToGrant);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public String getUsedByUserId() {
        return usedByUserId;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public void markRedeemedBy(String userId, Instant usedAt) {
        this.usedByUserId = userId;
        this.usedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}
