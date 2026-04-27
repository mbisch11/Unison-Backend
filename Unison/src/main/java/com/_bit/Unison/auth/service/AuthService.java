package com._bit.Unison.auth.service;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.AccountRestrictedException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.moderation.service.ModerationService;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthSessionRepository sessionRepo;
    private final SessionResolver sessionResolver;
    private final UserProfileRepository userRepo;
    private final UserProfileService userService;
    private final ModerationService moderationService;

    public AuthService(
            AuthSessionRepository sessionRepo,
            SessionResolver sessionResolver,
            UserProfileRepository userRepo,
            UserProfileService userService,
            ModerationService moderationService
    ) {
        this.sessionRepo = sessionRepo;
        this.sessionResolver = sessionResolver;
        this.userRepo = userRepo;
        this.userService = userService;
        this.moderationService = moderationService;
    }

    public LoginResult signup(String displayName, String email, String username, String password, Set<String> courseIds) {
        UserProfile created = userService.registerUser(displayName, email, username, password, courseIds);
        return createSession(created);
    }

    public LoginResult login(String emailOrUsername, String password) {
        UserProfile user = userService.authenticate(emailOrUsername, password);
        ensureAccountAccess(user);
        return createSession(user);
    }

    public LoginResult adminSignup(
            String token,
            String displayName,
            String email,
            String username,
            String password,
            Set<String> courseIds
    ) {
        UserProfile created = moderationService.redeemAdminInvite(token, displayName, email, username, password, courseIds);
        return createSession(created);
    }

    public void logout(String sessionId) {
        AuthSession session = sessionResolver.requireSession(sessionId);
        sessionRepo.deleteById(session.getSessionId());
    }

    public UserProfile me(String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);
        return userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private LoginResult createSession(UserProfile user) {
        ensureAccountAccess(user);

        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        Set<String> roles = user.getRoles();

        AuthSession session = new AuthSession(sessionId, user.getUserId(), roles, expiresAt);
        sessionRepo.save(session);

        return new LoginResult(sessionId, user.getUserId(), roles, expiresAt);
    }

    private void ensureAccountAccess(UserProfile user) {
        if (user.clearExpiredSuspension()) {
            userRepo.save(user);
        }

        if (user.isBanned()) {
            throw new AccountRestrictedException(
                    AccountRestrictedException.CODE_ACCOUNT_BANNED,
                    user.buildRestrictionMessage()
            );
        }

        if (user.isSuspended()) {
            throw new AccountRestrictedException(
                    AccountRestrictedException.CODE_ACCOUNT_SUSPENDED,
                    user.buildRestrictionMessage()
            );
        }
    }

    public record LoginResult(String sessionId, String userId, Set<String> roles, Instant expiresAt) {
    }
}
