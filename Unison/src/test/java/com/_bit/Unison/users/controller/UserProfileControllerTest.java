package com._bit.Unison.users.controller;

import com._bit.Unison.common.ConflictException;
import com._bit.Unison.common.InvalidSessionException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserProfileService userService;

    @Test
    void createUser_returns201_andUserJson() throws Exception {
        UserProfile saved = new UserProfile(
                "Michael", "michael@example.com", "michael",
                "michael@example.com", "michael", "hash", Set.of("HCDD440"));

        when(userService.registerUser(
                eq("Michael"), eq("michael@example.com"), eq("michael"),
                eq("password123"), anySet()))
                .thenReturn(saved);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "michael@example.com",
                                  "username": "michael",
                                  "rawPassword": "password123",
                                  "courseIds": ["HCDD440"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.displayName").value("Michael"))
                .andExpect(jsonPath("$.email").value("michael@example.com"))
                .andExpect(jsonPath("$.username").value("michael"));

        verify(userService).registerUser(
                eq("Michael"), eq("michael@example.com"), eq("michael"),
                eq("password123"), anySet());
    }

    @Test
    void createUser_withNullCourseIds_returns201() throws Exception {
        UserProfile saved = new UserProfile(
                "Michael", "michael@example.com", "michael",
                "michael@example.com", "michael", "hash", Set.of());

        when(userService.registerUser(
                eq("Michael"), eq("michael@example.com"), eq("michael"),
                eq("password123"), isNull()))
                .thenReturn(saved);

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "michael@example.com",
                                  "username": "michael",
                                  "rawPassword": "password123"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_whenServiceThrows_returns409() throws Exception {
        when(userService.registerUser(any(), any(), any(), any(), any()))
                .thenThrow(new ConflictException("email is already in use"));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "taken@example.com",
                                  "username": "michael",
                                  "rawPassword": "password123"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getUser_returns200_andUserJson() throws Exception {
        UserProfile u = new UserProfile(
                "Michael", "michael@example.com", "michael",
                "michael@example.com", "michael", "hash", Set.of("HCDD440"));

        when(userService.getUser("u1")).thenReturn(u);

        mvc.perform(get("/users/u1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.displayName").value("Michael"))
                .andExpect(jsonPath("$.email").value("michael@example.com"));

        verify(userService).getUser("u1");
    }

    @Test
    void getUser_whenNotFound_returns404() throws Exception {
        when(userService.getUser("missing"))
                .thenThrow(new NotFoundException("User not found: missing"));

        mvc.perform(get("/users/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCurrentUser_returns200_andUpdatedUserJson() throws Exception {
        UserProfile updated = new UserProfile(
                "Michael Brown", "michaelb@example.com", "michaelb",
                "michaelb@example.com", "michaelb", "hash", Set.of("HCDD440", "CMPSC131"));
        ReflectionTestUtils.setField(updated, "userId", "u1");

        when(userService.updateCurrentUser(
                eq("s1"),
                eq("Michael Brown"),
                eq("michaelb@example.com"),
                eq("michaelb"),
                anySet()))
                .thenReturn(updated);

        mvc.perform(patch("/users/me")
                        .header("X-Session-Id", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael Brown",
                                  "email": "michaelb@example.com",
                                  "username": "michaelb",
                                  "courseIds": ["HCDD440", "CMPSC131"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.displayName").value("Michael Brown"))
                .andExpect(jsonPath("$.email").value("michaelb@example.com"))
                .andExpect(jsonPath("$.username").value("michaelb"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.normalizedEmail").doesNotExist())
                .andExpect(jsonPath("$.normalizedUsername").doesNotExist());

        verify(userService).updateCurrentUser(
                eq("s1"),
                eq("Michael Brown"),
                eq("michaelb@example.com"),
                eq("michaelb"),
                anySet());
    }

    @Test
    void updateCurrentUser_whenPayloadInvalid_returns400() throws Exception {
        when(userService.updateCurrentUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("email format is invalid"));

        mvc.perform(patch("/users/me")
                        .header("X-Session-Id", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "not-an-email",
                                  "username": "michael"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email format is invalid"));
    }

    @Test
    void updateCurrentUser_whenSessionInvalid_returns401() throws Exception {
        when(userService.updateCurrentUser(anyString(), any(), any(), any(), any()))
                .thenThrow(new InvalidSessionException("Session expired"));

        mvc.perform(patch("/users/me")
                        .header("X-Session-Id", "expired-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "michael@example.com",
                                  "username": "michael"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Session expired"));
    }
}
