package com._bit.Unison.users.service;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    @Test
    void createUser_savesAndReturnsUser() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        UserProfileService service = new UserProfileService(repo);
        HashSet<String> emptySet = new HashSet<String>();

        UserProfile saved = new UserProfile("Michael", "test@email.com", "username", "test@email.com", "username", "234567mewciwinc3uedn", emptySet);
        when(repo.save(any(UserProfile.class))).thenReturn(saved);

        UserProfile result = service.registerUser("Michael", "test@email.com", "username", "test@email.com", emptySet);

        assertNotNull(result);
        assertEquals("Michael", result.getDisplayName());
        verify(repo, times(1)).save(any(UserProfile.class));
    }

    @Test
    void getUser_whenFound_returnsUser() {
        UserProfileRepository repo = mock(UserProfileRepository.class);
        UserProfileService service = new UserProfileService(repo);
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
        UserProfileService service = new UserProfileService(repo);

        when(repo.findById("missing")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getUser("missing"));
        assertTrue(ex.getMessage().contains("User not found"));
    }
}