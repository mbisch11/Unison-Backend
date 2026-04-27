package com._bit.Unison.moderation.controller;

import com._bit.Unison.moderation.model.ReportReasonCode;
import com._bit.Unison.moderation.model.ReportTargetType;
import com._bit.Unison.moderation.service.ModerationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ModerationService.ModerationReportResponse submitReport(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody SubmitReportRequest request
    ) {
        return moderationService.submitReport(
                sessionId,
                request.targetType,
                request.targetId,
                request.reasonCode,
                request.justification
        );
    }

    @GetMapping("/reports/me")
    public List<ModerationService.ModerationReportResponse> listMyReports(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        return moderationService.listReportsForReporter(sessionId);
    }

    @GetMapping("/admin/reports")
    public List<ModerationService.ModerationReportResponse> listReportsForAdmin(
            @RequestHeader("X-Session-Id") String sessionId
    ) {
        return moderationService.listReportsForAdmin(sessionId);
    }

    @GetMapping("/admin/reports/{reportId}")
    public ModerationService.ModerationReportResponse getReportForAdmin(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String reportId
    ) {
        return moderationService.getReportForAdmin(sessionId, reportId);
    }

    @PostMapping("/admin/reports/{reportId}/dismiss")
    public ModerationService.ModerationReportResponse dismissReport(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String reportId,
            @RequestBody ReviewDecisionRequest request
    ) {
        return moderationService.dismissReport(sessionId, reportId, request.decisionNotes);
    }

    @PostMapping("/admin/reports/{reportId}/suspend")
    public ModerationService.ModerationReportResponse suspendReportedUser(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String reportId,
            @RequestBody SuspendDecisionRequest request
    ) {
        Instant suspensionEndsAt = request.suspensionEndsAt == null || request.suspensionEndsAt.isBlank()
                ? null
                : Instant.parse(request.suspensionEndsAt);

        return moderationService.suspendReportedUser(sessionId, reportId, suspensionEndsAt, request.decisionNotes);
    }

    @PostMapping("/admin/reports/{reportId}/ban")
    public ModerationService.ModerationReportResponse banReportedUser(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String reportId,
            @RequestBody ReviewDecisionRequest request
    ) {
        return moderationService.banReportedUser(sessionId, reportId, request.decisionNotes);
    }

    public static class SubmitReportRequest {
        public ReportTargetType targetType;
        public String targetId;
        public ReportReasonCode reasonCode;
        public String justification;
    }

    public static class ReviewDecisionRequest {
        public String decisionNotes;
    }

    public static class SuspendDecisionRequest extends ReviewDecisionRequest {
        public String suspensionEndsAt;
    }
}
