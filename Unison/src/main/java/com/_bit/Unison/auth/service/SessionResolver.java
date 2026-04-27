package com._bit.Unison.auth.service;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.AccountRestrictedException;
import com._bit.Unison.common.ForbiddenException;
import com._bit.Unison.common.InvalidSessionException;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SessionResolver {

    private final AuthSessionRepository sessionRepo;
    private final UserProfileRepository userRepo;

    public SessionResolver(AuthSessionRepository sessionRepo, UserProfileRepository userRepo) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
    }

    public AuthSession requireSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidSessionException("Missing session id");
        }

        AuthSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new InvalidSessionException("Invalid session id"));

        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
            sessionRepo.deleteById(sessionId);
            throw new InvalidSessionException("Session expired");
        }

        UserProfile user = userRepo.findById(session.getUserId())
                .orElseThrow(() -> new InvalidSessionException("Session user no longer exists"));

        if (user.clearExpiredSuspension()) {
            userRepo.save(user);
        }

        if (user.isBanned()) {
            sessionRepo.deleteByUserId(user.getUserId());
            throw new AccountRestrictedException(
                    AccountRestrictedException.CODE_ACCOUNT_BANNED,
                    user.buildRestrictionMessage()
            );
        }

        if (user.isSuspended()) {
            sessionRepo.deleteByUserId(user.getUserId());
            throw new AccountRestrictedException(
                    AccountRestrictedException.CODE_ACCOUNT_SUSPENDED,
                    user.buildRestrictionMessage()
            );
        }

        return session;
    }

    public String requireUserId(String sessionId) {
        return requireSession(sessionId).getUserId();
    }

    public void requireRole(String sessionId, String role) {
        AuthSession session = requireSession(sessionId);
        if (!session.hasRole(role)) {
            throw new ForbiddenException("Required role missing: " + role);
        }
    }

    public void requireAdmin(String sessionId) {
        requireRole(sessionId, UserProfileService.ROLE_ADMIN);
    }
}
