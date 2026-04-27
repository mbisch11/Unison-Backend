package com._bit.Unison.auth.controller;

import com._bit.Unison.auth.service.AuthService;
import com._bit.Unison.common.AuthenticationFailedException;
import com._bit.Unison.users.model.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void signup_returns201_andSessionPayload() throws Exception {
        AuthService.LoginResult result = new AuthService.LoginResult("s1", "u1", Set.of("USER"), Instant.parse("2026-04-21T12:00:00Z"));
        when(authService.signup(eq("Michael"), eq("michael@example.com"), eq("michael"), eq("password123"), eq(Set.of("HCDD440"))))
                .thenReturn(result);

        mvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Michael",
                                  "email": "michael@example.com",
                                  "username": "michael",
                                  "password": "password123",
                                  "courseIds": ["HCDD440"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        verify(authService).signup("Michael", "michael@example.com", "michael", "password123", Set.of("HCDD440"));
    }

    @Test
    void adminSignup_returns201_andAdminSessionPayload() throws Exception {
        AuthService.LoginResult result = new AuthService.LoginResult("s2", "admin-1", Set.of("USER", "ADMIN"), Instant.parse("2026-04-21T12:00:00Z"));
        when(authService.adminSignup(
                eq("invite-token"),
                eq("Admin User"),
                eq("admin@example.com"),
                eq("adminuser"),
                eq("password123"),
                eq(Set.of("HCDD440"))
        )).thenReturn(result);

        mvc.perform(post("/auth/admin/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "invite-token",
                                  "displayName": "Admin User",
                                  "email": "admin@example.com",
                                  "username": "adminuser",
                                  "password": "password123",
                                  "courseIds": ["HCDD440"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("s2"))
                .andExpect(jsonPath("$.userId").value("admin-1"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("USER", "ADMIN")));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authService.login("michael", "wrong-password"))
                .thenThrow(new AuthenticationFailedException("Invalid username/email or password"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emailOrUsername": "michael",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password"));
    }

    @Test
    void logout_returns204() throws Exception {
        mvc.perform(post("/auth/logout")
                        .header("X-Session-Id", "s1"))
                .andExpect(status().isNoContent());

        verify(authService).logout("s1");
    }

    @Test
    void me_returns200_withoutSensitiveFields() throws Exception {
        UserProfile user = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of("HCDD440")
        );
        ReflectionTestUtils.setField(user, "userId", "u1");

        when(authService.me("s1")).thenReturn(user);

        mvc.perform(get("/auth/me").header("X-Session-Id", "s1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.displayName").value("Michael"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.normalizedEmail").doesNotExist())
                .andExpect(jsonPath("$.normalizedUsername").doesNotExist());
    }
}
