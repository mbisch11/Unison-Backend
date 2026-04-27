package com._bit.Unison.moderation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "moderation_reports")
public class ModerationReport {

    @Id
    private String reportId;
    private ReportTargetType targetType;
    @Indexed
    private String targetId;
    @Indexed
    private String reportedUserId;
    @Indexed
    private String reporterUserId;
    private ReportReasonCode reasonCode;
    private String justification;
    @Indexed
    private ReportStatus status;
    private ReportDecisionType decisionType;
    private String decisionNotes;
    private Instant suspensionEndsAt;
    private String reviewedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant reviewedAt;
    private Map<String, String> targetSnapshot;

    public ModerationReport() {
    }

    public ModerationReport(
            ReportTargetType targetType,
            String targetId,
            String reportedUserId,
            String reporterUserId,
            ReportReasonCode reasonCode,
            String justification,
            Map<String, String> targetSnapshot
    ) {
        Instant now = Instant.now();
        this.targetType = targetType;
        this.targetId = targetId;
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.reasonCode = reasonCode;
        this.justification = justification;
        this.status = ReportStatus.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
        this.targetSnapshot = targetSnapshot == null ? new HashMap<>() : new HashMap<>(targetSnapshot);
    }

    public String getReportId() {
        return reportId;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getReportedUserId() {
        return reportedUserId;
    }

    public String getReporterUserId() {
        return reporterUserId;
    }

    public ReportReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getJustification() {
        return justification;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportDecisionType getDecisionType() {
        return decisionType;
    }

    public String getDecisionNotes() {
        return decisionNotes;
    }

    public Instant getSuspensionEndsAt() {
        return suspensionEndsAt;
    }

    public String getReviewedByUserId() {
        return reviewedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Map<String, String> getTargetSnapshot() {
        return targetSnapshot == null ? Map.of() : new HashMap<>(targetSnapshot);
    }

    public boolean isActive() {
        return status == ReportStatus.OPEN || status == ReportStatus.UNDER_REVIEW;
    }

    public boolean isResolved() {
        return status == ReportStatus.DISMISSED || status == ReportStatus.RESOLVED;
    }

    public void markUnderReview(String reviewedByUserId) {
        this.status = ReportStatus.UNDER_REVIEW;
        this.reviewedByUserId = reviewedByUserId;
        this.updatedAt = Instant.now();
    }

    public void dismiss(String reviewedByUserId, String decisionNotes) {
        Instant now = Instant.now();
        this.status = ReportStatus.DISMISSED;
        this.decisionType = ReportDecisionType.DISMISS;
        this.reviewedByUserId = reviewedByUserId;
        this.decisionNotes = decisionNotes;
        this.reviewedAt = now;
        this.updatedAt = now;
        this.suspensionEndsAt = null;
    }

    public void suspend(String reviewedByUserId, String decisionNotes, Instant suspensionEndsAt) {
        Instant now = Instant.now();
        this.status = ReportStatus.RESOLVED;
        this.decisionType = ReportDecisionType.SUSPEND;
        this.reviewedByUserId = reviewedByUserId;
        this.decisionNotes = decisionNotes;
        this.suspensionEndsAt = suspensionEndsAt;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    public void ban(String reviewedByUserId, String decisionNotes) {
        Instant now = Instant.now();
        this.status = ReportStatus.RESOLVED;
        this.decisionType = ReportDecisionType.BAN;
        this.reviewedByUserId = reviewedByUserId;
        this.decisionNotes = decisionNotes;
        this.reviewedAt = now;
        this.updatedAt = now;
        this.suspensionEndsAt = null;
    }
}
