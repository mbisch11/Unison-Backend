package com._bit.Unison.auth.service;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionResolver {

    private final AuthSessionRepository sessionRepo;

    public SessionResolver(AuthSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    public String requireUserId(String sessionId) {
        AuthSession s = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sessionId"));

        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Session expired");
        }
        return s.getUserId();
    }
}