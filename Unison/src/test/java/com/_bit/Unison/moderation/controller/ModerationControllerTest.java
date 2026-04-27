package com._bit.Unison.moderation.controller;

import com._bit.Unison.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModerationController.class)
class ModerationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ModerationService moderationService;

    @Test
    void createInvite_returns201_andInviteLink() throws Exception {
        when(moderationService.createAdminInvite("admin-session", "newadmin@example.com", 48))
                .thenReturn(new ModerationService.CreatedAdminInvite(
                        "invite-1",
                        "http://localhost:5173/admin-signup?token=abc",
                        "newadmin@example.com",
                        Set.of("USER", "ADMIN"),
                        Instant.parse("2026-04-25T12:00:00Z"),
                        "PENDING"
                ));

        mvc.perform(post("/admin/invites")
                        .header("X-Session-Id", "admin-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newadmin@example.com",
                                  "expiresInHours": 48
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteId").value("invite-1"))
                .andExpect(jsonPath("$.inviteLink").value("http://localhost:5173/admin-signup?token=abc"));
    }

    @Test
    void validateInvite_returnsStatusPayload() throws Exception {
        when(moderationService.validateInviteToken("abc"))
                .thenReturn(new ModerationService.InviteValidationResult(true, "PENDING", "admin@example.com", Instant.parse("2026-04-25T12:00:00Z")));

        mvc.perform(get("/admin/invites/validate").param("token", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listInvites_returns200() throws Exception {
        when(moderationService.listInvites("admin-session"))
                .thenReturn(List.of(new ModerationService.AdminInviteSummary(
                        "invite-1",
                        "admin@example.com",
                        Set.of("USER", "ADMIN"),
                        Instant.parse("2026-04-25T12:00:00Z"),
                        Instant.parse("2026-04-24T12:00:00Z"),
                        "admin-1",
                        null,
                        null,
                        null,
                        false,
                        "PENDING"
                )));

        mvc.perform(get("/admin/invites").header("X-Session-Id", "admin-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inviteId").value("invite-1"));
    }

    @Test
    void revokeInvite_returns204() throws Exception {
        mvc.perform(post("/admin/invites/invite-1/revoke").header("X-Session-Id", "admin-session"))
                .andExpect(status().isNoContent());

        verify(moderationService).revokeInvite("admin-session", "invite-1");
    }
}
