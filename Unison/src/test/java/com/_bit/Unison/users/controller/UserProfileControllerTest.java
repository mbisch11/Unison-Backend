package com._bit.Unison.users.controller;

import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    void createUser_whenServiceThrows_returns400() throws Exception {
        when(userService.registerUser(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("email is already in use"));

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
                .andExpect(status().isBadRequest());
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
    void getUser_whenNotFound_returns500() throws Exception {
        when(userService.getUser("missing"))
                .thenThrow(new RuntimeException("User not found: missing"));

        mvc.perform(get("/users/missing"))
                .andExpect(status().is5xxServerError());
    }
}