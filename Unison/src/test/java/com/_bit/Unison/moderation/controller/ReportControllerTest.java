package com._bit.Unison.moderation.controller;

import com._bit.Unison.moderation.model.ReportDecisionType;
import com._bit.Unison.moderation.model.ReportReasonCode;
import com._bit.Unison.moderation.model.ReportStatus;
import com._bit.Unison.moderation.model.ReportTargetType;
import com._bit.Unison.moderation.service.ModerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ModerationService moderationService;

    @Test
    void submitReport_returnsCreatedResponse() throws Exception {
        when(moderationService.submitReport(
                "user-session",
                ReportTargetType.USER,
                "user-2",
                ReportReasonCode.HARASSMENT,
                "This user sent repeated abusive messages."
        )).thenReturn(reportResponse(ReportStatus.OPEN, null));

        mvc.perform(post("/reports")
                        .header("X-Session-Id", "user-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "USER",
                                  "targetId": "user-2",
                                  "reasonCode": "HARASSMENT",
                                  "justification": "This user sent repeated abusive messages."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reportId").value("report-1"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void listReportsForAdmin_returnsReports() throws Exception {
        when(moderationService.listReportsForAdmin("admin-session"))
                .thenReturn(List.of(reportResponse(ReportStatus.OPEN, null)));

        mvc.perform(get("/admin/reports").header("X-Session-Id", "admin-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportId").value("report-1"));
    }

    @Test
    void banReport_forwardsDecisionNotes() throws Exception {
        when(moderationService.banReportedUser("admin-session", "report-1", "Repeated harassment confirmed."))
                .thenReturn(reportResponse(ReportStatus.RESOLVED, ReportDecisionType.BAN));

        mvc.perform(post("/admin/reports/report-1/ban")
                        .header("X-Session-Id", "admin-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "decisionNotes": "Repeated harassment confirmed."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionType").value("BAN"));

        verify(moderationService).banReportedUser("admin-session", "report-1", "Repeated harassment confirmed.");
    }

    private ModerationService.ModerationReportResponse reportResponse(
            ReportStatus status,
            ReportDecisionType decisionType
    ) {
        return new ModerationService.ModerationReportResponse(
                "report-1",
                ReportTargetType.USER,
                "user-2",
                "user-2",
                "user-1",
                ReportReasonCode.HARASSMENT,
                "This user sent repeated abusive messages.",
                status,
                decisionType,
                decisionType == null ? null : "Repeated harassment confirmed.",
                decisionType == ReportDecisionType.SUSPEND ? Instant.parse("2026-05-01T12:00:00Z") : null,
                decisionType == null ? null : "admin-1",
                Instant.parse("2026-04-24T12:00:00Z"),
                Instant.parse("2026-04-24T12:00:00Z"),
                decisionType == null ? null : Instant.parse("2026-04-24T13:00:00Z"),
                Map.of("displayName", "Reported User")
        );
    }
}
