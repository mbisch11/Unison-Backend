package com._bit.Unison.auth.service;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.AccountRestrictedException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.moderation.service.ModerationService;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void signup_registersUser_andCreatesUserSession() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        UserProfile createdUser = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of("HCDD440")
        );
        ReflectionTestUtils.setField(createdUser, "userId", "u1");

        when(userService.registerUser("Michael", "michael@example.com", "michael", "password123", Set.of("HCDD440")))
                .thenReturn(createdUser);

        AuthService.LoginResult result = authService.signup("Michael", "michael@example.com", "michael", "password123", Set.of("HCDD440"));

        ArgumentCaptor<AuthSession> sessionCaptor = ArgumentCaptor.forClass(AuthSession.class);
        verify(sessionRepo).save(sessionCaptor.capture());

        assertEquals("u1", result.userId());
        assertEquals(Set.of("USER"), result.roles());
        assertEquals("u1", sessionCaptor.getValue().getUserId());
        assertTrue(sessionCaptor.getValue().hasRole("USER"));
        assertTrue(sessionCaptor.getValue().getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void login_usesPersistedUserRolesInSessionPayload() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        UserProfile adminUser = new UserProfile(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "admin@example.com",
                "adminuser",
                "hash",
                Set.of("HCDD440"),
                Set.of("USER", "ADMIN")
        );
        ReflectionTestUtils.setField(adminUser, "userId", "admin-1");

        when(userService.authenticate("adminuser", "password123")).thenReturn(adminUser);

        AuthService.LoginResult result = authService.login("adminuser", "password123");

        assertEquals(Set.of("USER", "ADMIN"), result.roles());
    }

    @Test
    void logout_deletesResolvedSession() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        AuthSession session = new AuthSession("s1", "u1", Set.of("USER"), Instant.now().plusSeconds(3600));
        when(sessionResolver.requireSession("s1")).thenReturn(session);

        authService.logout("s1");

        verify(sessionRepo).deleteById("s1");
    }

    @Test
    void me_whenUserMissing_throwsNotFound() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(userRepo.findById("u1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authService.me("s1"));
    }

    @Test
    void adminSignup_consumesInviteAndCreatesAdminSession() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        UserProfile createdAdmin = new UserProfile(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "admin@example.com",
                "adminuser",
                "hash",
                Set.of("HCDD440"),
                Set.of("USER", "ADMIN")
        );
        ReflectionTestUtils.setField(createdAdmin, "userId", "admin-1");

        when(moderationService.redeemAdminInvite(
                "invite-token",
                "Admin User",
                "admin@example.com",
                "adminuser",
                "password123",
                Set.of("HCDD440")
        )).thenReturn(createdAdmin);

        AuthService.LoginResult result = authService.adminSignup(
                "invite-token",
                "Admin User",
                "admin@example.com",
                "adminuser",
                "password123",
                Set.of("HCDD440")
        );

        assertEquals(Set.of("USER", "ADMIN"), result.roles());
    }

    @Test
    void login_whenUserIsSuspended_throwsRestrictionAndSkipsSessionCreation() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        ModerationService moderationService = mock(ModerationService.class);
        AuthService authService = new AuthService(sessionRepo, sessionResolver, userRepo, userService, moderationService);

        UserProfile suspendedUser = new UserProfile(
                "Suspended User",
                "suspended@example.com",
                "suspendeduser",
                "suspended@example.com",
                "suspendeduser",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(suspendedUser, "userId", "u-suspended");
        suspendedUser.suspendUntil(Instant.now().plusSeconds(3600), "Pending moderation review");

        when(userService.authenticate("suspendeduser", "password123")).thenReturn(suspendedUser);

        assertThrows(AccountRestrictedException.class, () -> authService.login("suspendeduser", "password123"));
    }
}
