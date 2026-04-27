package com._bit.Unison.moderation.controller;

import com._bit.Unison.moderation.service.ModerationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/invites")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationService.CreatedAdminInvite createInvite(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody CreateAdminInviteRequest req
    ) {
        return moderationService.createAdminInvite(sessionId, req.email, req.expiresInHours);
    }

    @GetMapping("/validate")
    public ModerationService.InviteValidationResult validateInvite(@RequestParam String token) {
        return moderationService.validateInviteToken(token);
    }

    @GetMapping
    public List<ModerationService.AdminInviteSummary> listInvites(@RequestHeader("X-Session-Id") String sessionId) {
        return moderationService.listInvites(sessionId);
    }

    @PostMapping("/{inviteId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeInvite(@RequestHeader("X-Session-Id") String sessionId, @PathVariable String inviteId) {
        moderationService.revokeInvite(sessionId, inviteId);
    }

    public static class CreateAdminInviteRequest {
        public String email;
        public Integer expiresInHours;
    }
}
