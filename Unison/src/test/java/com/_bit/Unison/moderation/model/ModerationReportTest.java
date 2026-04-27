package com._bit.Unison.moderation.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationReportTest {

    @Test
    void newReport_defaultsToOpenStatus() {
        ModerationReport report = new ModerationReport(
                ReportTargetType.USER,
                "user-2",
                "user-2",
                "user-1",
                ReportReasonCode.HARASSMENT,
                "This user sent repeated abusive messages.",
                Map.of("displayName", "Reported User")
        );

        assertEquals(ReportStatus.OPEN, report.getStatus());
        assertTrue(report.isActive());
        assertEquals("user-1", report.getReporterUserId());
        assertEquals("Reported User", report.getTargetSnapshot().get("displayName"));
        assertNotNull(report.getCreatedAt());
        assertNotNull(report.getUpdatedAt());
    }

    @Test
    void suspend_marksReportResolved() {
        ModerationReport report = new ModerationReport(
                ReportTargetType.GROUP,
                "group-1",
                "user-2",
                "user-1",
                ReportReasonCode.SPAM,
                "This listing is clearly spam.",
                Map.of()
        );

        Instant suspensionEnd = Instant.parse("2026-05-01T12:00:00Z");
        report.suspend("admin-1", "Repeated spam reports confirmed.", suspensionEnd);

        assertEquals(ReportStatus.RESOLVED, report.getStatus());
        assertEquals(ReportDecisionType.SUSPEND, report.getDecisionType());
        assertEquals("admin-1", report.getReviewedByUserId());
        assertEquals(suspensionEnd, report.getSuspensionEndsAt());
        assertTrue(report.isResolved());
        assertNotNull(report.getReviewedAt());
    }
}
