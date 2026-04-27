package com._bit.Unison.auth.service;

import com._bit.Unison.auth.model.AuthSession;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.AccountRestrictedException;
import com._bit.Unison.common.ForbiddenException;
import com._bit.Unison.common.InvalidSessionException;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionResolverTest {

    @Test
    void requireSession_withExpiredSession_deletesAndThrows() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        SessionResolver resolver = new SessionResolver(sessionRepo, userRepo);

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(
                new AuthSession("s1", "u1", Set.of("USER"), Instant.now().minusSeconds(60))
        ));

        assertThrows(InvalidSessionException.class, () -> resolver.requireSession("s1"));
        verify(sessionRepo).deleteById("s1");
    }

    @Test
    void requireUserId_returnsUserIdForValidSession() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        SessionResolver resolver = new SessionResolver(sessionRepo, userRepo);
        UserProfile user = new UserProfile("User", "u@example.com", "user", "u@example.com", "user", "hash", Set.of());
        ReflectionTestUtils.setField(user, "userId", "u1");

        when(sessionRepo.findById("s1")).thenReturn(Optional.of(
                new AuthSession("s1", "u1", Set.of("USER"), Instant.now().plusSeconds(60))
        ));
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertEquals("u1", resolver.requireUserId("s1"));
    }

    @Test
    void requireAdmin_whenRolePresent_allowsAccess() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        SessionResolver resolver = new SessionResolver(sessionRepo, userRepo);
        UserProfile user = new UserProfile("Admin", "a@example.com", "admin", "a@example.com", "admin", "hash", Set.of(), Set.of("USER", "ADMIN"));
        ReflectionTestUtils.setField(user, "userId", "u1");

        when(sessionRepo.findById("admin-session")).thenReturn(Optional.of(
                new AuthSession("admin-session", "u1", Set.of("USER", "ADMIN"), Instant.now().plusSeconds(60))
        ));
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        resolver.requireAdmin("admin-session");
    }

    @Test
    void requireAdmin_whenRoleMissing_throwsForbidden() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        SessionResolver resolver = new SessionResolver(sessionRepo, userRepo);
        UserProfile user = new UserProfile("User", "u@example.com", "user", "u@example.com", "user", "hash", Set.of());
        ReflectionTestUtils.setField(user, "userId", "u1");

        when(sessionRepo.findById("user-session")).thenReturn(Optional.of(
                new AuthSession("user-session", "u1", Set.of("USER"), Instant.now().plusSeconds(60))
        ));
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> resolver.requireAdmin("user-session"));
    }

    @Test
    void requireSession_whenUserBanned_revokesSessionsAndThrowsRestriction() {
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        SessionResolver resolver = new SessionResolver(sessionRepo, userRepo);
        UserProfile user = new UserProfile("User", "u@example.com", "user", "u@example.com", "user", "hash", Set.of());
        ReflectionTestUtils.setField(user, "userId", "u1");
        user.ban("Repeated harassment");

        when(sessionRepo.findById("banned-session")).thenReturn(Optional.of(
                new AuthSession("banned-session", "u1", Set.of("USER"), Instant.now().plusSeconds(60))
        ));
        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(AccountRestrictedException.class, () -> resolver.requireSession("banned-session"));
        verify(sessionRepo).deleteByUserId("u1");
    }
}
