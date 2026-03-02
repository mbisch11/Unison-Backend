package com._bit.Unison.auth.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document(collection = "auth_sessions")
public class AuthSession {

    @Id
    private String sessionId;
    private String userId;
    private Set<String> roles;
    private Instant expiresAt;

    public AuthSession() {}

    public AuthSession(String sessionId, String userId, Set<String> roles, Instant expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.roles = roles;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}