package com._bit.Unison.users.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.common.InvalidSessionException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    @Test
    void createUser_savesAndReturnsUser() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);
        HashSet<String> emptySet = new HashSet<String>();

        UserProfile saved = new UserProfile("Michael", "test@email.com", "username", "test@email.com", "username", "234567mewciwinc3uedn", emptySet);
        when(repo.save(any(UserProfile.class))).thenReturn(saved);

        UserProfile result = service.registerUser("Michael", "test@email.com", "username", "test@email.com", emptySet);

        assertNotNull(result);
        assertEquals("Michael", result.getDisplayName());
        assertEquals(Set.of("USER"), result.getRoles());
        verify(repo, times(1)).save(any(UserProfile.class));
    }

    @Test
    void createUser_withExplicitRoles_normalizesAndPersistsThem() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        when(repo.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = service.registerUser(
                "Admin Michael",
                "admin@email.com",
                "admin_michael",
                "password123",
                Set.of("HCDD440"),
                Set.of("admin", "user")
        );

        assertEquals(Set.of("ADMIN", "USER"), result.getRoles());
    }

    @Test
    void getUser_whenFound_returnsUser() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);
        HashSet<String> emptySet = new HashSet<String>();

        UserProfile u = new UserProfile("Michael", "test@email.com", "username", "test@email.com", "username", "234567mewciwinc3uedn", emptySet);
        when(repo.findById("u1")).thenReturn(Optional.of(u));

        UserProfile result = service.getUser("u1");

        assertNotNull(result);
        verify(repo).findById("u1");
    }

    @Test
    void getUser_whenMissing_throws() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        when(repo.findById("missing")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getUser("missing"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void createUser_whenEmailAlreadyExists_throwsConflict() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        when(repo.existsByNormalizedEmail("test@email.com")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.registerUser("Michael", "test@email.com", "username", "password123", Set.of())
        );
    }

    @Test
    void updateCurrentUser_updatesEditableFields_andNormalizesCourseIds() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        UserProfile existing = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of("CMPSC 131")
        );
        ReflectionTestUtils.setField(existing, "userId", "u1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(repo.findById("u1")).thenReturn(Optional.of(existing));
        when(repo.existsByNormalizedEmailAndUserIdNot("michaelb@example.com", "u1")).thenReturn(false);
        when(repo.existsByNormalizedUsernameAndUserIdNot("michaelb", "u1")).thenReturn(false);
        when(repo.save(existing)).thenReturn(existing);

        UserProfile result = service.updateCurrentUser(
                "s1",
                "Michael Brown",
                "MichaelB@example.com",
                "MichaelB",
                Set.of(" HCDD 440 ", "CMPSC 131", " ")
        );

        assertEquals("Michael Brown", result.getDisplayName());
        assertEquals("MichaelB@example.com", result.getEmail());
        assertEquals("MichaelB", result.getUsername());
        assertEquals(Set.of("HCDD 440", "CMPSC 131"), result.getCourseIds());
        assertEquals("michaelb@example.com", result.getNormalizedEmail());
        assertEquals("michaelb", result.getNormalizedUsername());
        verify(repo).save(existing);
    }

    @Test
    void updateCurrentUser_whenEmailAlreadyExists_throwsConflict() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        UserProfile existing = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(existing, "userId", "u1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(repo.findById("u1")).thenReturn(Optional.of(existing));
        when(repo.existsByNormalizedEmailAndUserIdNot("taken@example.com", "u1")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.updateCurrentUser("s1", "Michael", "taken@example.com", "michael", Set.of())
        );
    }

    @Test
    void updateCurrentUser_whenUsernameAlreadyExists_throwsConflict() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        UserProfile existing = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(existing, "userId", "u1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(repo.findById("u1")).thenReturn(Optional.of(existing));
        when(repo.existsByNormalizedEmailAndUserIdNot("michael@example.com", "u1")).thenReturn(false);
        when(repo.existsByNormalizedUsernameAndUserIdNot("takenuser", "u1")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                service.updateCurrentUser("s1", "Michael", "michael@example.com", "takenuser", Set.of())
        );
    }

    @Test
    void updateCurrentUser_whenSessionInvalid_throws() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        when(sessionResolver.requireUserId("missing")).thenThrow(new InvalidSessionException("Invalid session id"));

        assertThrows(InvalidSessionException.class, () ->
                service.updateCurrentUser("missing", "Michael", "michael@example.com", "michael", Set.of())
        );
    }

    @Test
    void updateCurrentUser_whenDisplayNameBlank_throwsBadRequest() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        SessionResolver sessionResolver = mock(SessionResolver.class);
        UserProfileService service = new UserProfileService(repo, sessionResolver);

        UserProfile existing = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(existing, "userId", "u1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(repo.findById("u1")).thenReturn(Optional.of(existing));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.updateCurrentUser("s1", "   ", "michael@example.com", "michael", Set.of())
        );

        assertEquals("displayName is required", ex.getMessage());
    }
}
