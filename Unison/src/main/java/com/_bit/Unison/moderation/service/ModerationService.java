package com._bit.Unison.moderation.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.moderation.model.AdminInvite;
import com._bit.Unison.moderation.model.ModerationReport;
import com._bit.Unison.moderation.model.ReportDecisionType;
import com._bit.Unison.moderation.model.ReportReasonCode;
import com._bit.Unison.moderation.model.ReportStatus;
import com._bit.Unison.moderation.model.ReportTargetType;
import com._bit.Unison.moderation.repo.AdminInviteRepository;
import com._bit.Unison.moderation.repo.ModerationReportRepository;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.notifications.service.NotificationService;
import com._bit.Unison.users.model.AccountStatus;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ModerationService {

    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();
    private static final String PENDING_REDEMPTION_USER_ID = "__PENDING_REDEMPTION__";
    private static final int DEFAULT_INVITE_EXPIRY_HOURS = 72;
    private static final Set<String> ADMIN_ROLES = Set.of(UserProfileService.ROLE_USER, UserProfileService.ROLE_ADMIN);

    private final SessionResolver sessionResolver;
    private final AuthSessionRepository sessionRepo;
    private final AdminInviteRepository inviteRepo;
    private final ModerationReportRepository reportRepo;
    private final StudyGroupRepository groupRepo;
    private final UserProfileRepository userRepo;
    private final UserProfileService userService;
    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final String frontendBaseUrl;

    public ModerationService(
            SessionResolver sessionResolver,
            AuthSessionRepository sessionRepo,
            AdminInviteRepository inviteRepo,
            ModerationReportRepository reportRepo,
            StudyGroupRepository groupRepo,
            UserProfileRepository userRepo,
            UserProfileService userService,
            NotificationService notificationService,
            MongoTemplate mongoTemplate,
            @Value("${unison.frontend-base-url:http://localhost:5173}") String frontendBaseUrl
    ) {
        this.sessionResolver = sessionResolver;
        this.sessionRepo = sessionRepo;
        this.inviteRepo = inviteRepo;
        this.reportRepo = reportRepo;
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.notificationService = notificationService;
        this.mongoTemplate = mongoTemplate;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public CreatedAdminInvite createAdminInvite(String sessionId, String email, Integer expiresInHours) {
        String createdByUserId = sessionResolver.requireUserId(sessionId);
        sessionResolver.requireAdmin(sessionId);

        return createInvite(createdByUserId, email, resolveExpiry(expiresInHours, DEFAULT_INVITE_EXPIRY_HOURS), false);
    }

    public CreatedAdminInvite resetBootstrapInvite(String email, long expiresInHours) {
        if (hasAdminUsers()) {
            throw new ConflictException("Bootstrap admin invite is disabled once an admin exists");
        }

        Instant now = Instant.now();
        inviteRepo.findByBootstrapTrueAndUsedAtIsNullAndRevokedAtIsNull()
                .forEach(invite -> {
                    invite.revoke(now);
                    inviteRepo.save(invite);
                });

        return createInvite("BOOTSTRAP", email, Instant.now().plus(expiresInHours, ChronoUnit.HOURS), true);
    }

    public InviteValidationResult validateInviteToken(String token) {
        if (token == null || token.isBlank()) {
            return new InviteValidationResult(false, "INVALID", null, null);
        }

        AdminInvite invite = inviteRepo.findByTokenHash(hashToken(token)).orElse(null);
        if (invite == null) {
            return new InviteValidationResult(false, "INVALID", null, null);
        }

        String status = resolveStatus(invite);
        return new InviteValidationResult("PENDING".equals(status), status, invite.getEmail(), invite.getExpiresAt());
    }

    public UserProfile redeemAdminInvite(
            String token,
            String displayName,
            String email,
            String username,
            String password,
            Set<String> courseIds
    ) {
        AdminInvite invite = requirePendingInvite(token);
        validateInviteEmail(invite, email);

        Instant claimedAt = Instant.now();
        if (!claimInvite(invite.getInviteId(), claimedAt)) {
            throw new ConflictException("This invite has already been used");
        }

        try {
            UserProfile createdUser = userService.registerUser(
                    displayName,
                    email,
                    username,
                    password,
                    courseIds,
                    invite.getRolesToGrant()
            );
            invite.markRedeemedBy(createdUser.getUserId(), claimedAt);
            inviteRepo.save(invite);
            notifyInviteCreator(invite, createdUser);
            return createdUser;
        } catch (RuntimeException ex) {
            releaseInviteClaim(invite.getInviteId(), claimedAt);
            throw ex;
        }
    }

    public void revokeInvite(String sessionId, String inviteId) {
        sessionResolver.requireAdmin(sessionId);
        AdminInvite invite = inviteRepo.findById(inviteId)
                .orElseThrow(() -> new NotFoundException("Invite not found: " + inviteId));

        if (invite.isUsed()) {
            throw new ConflictException("Used invites cannot be revoked");
        }

        if (invite.isRevoked()) {
            throw new ConflictException("Invite is already revoked");
        }

        invite.revoke(Instant.now());
        inviteRepo.save(invite);
    }

    public List<AdminInviteSummary> listInvites(String sessionId) {
        sessionResolver.requireAdmin(sessionId);
        return inviteRepo.findAllByOrderByCreatedAtDesc().stream()
                .sorted(Comparator.comparing(AdminInvite::getCreatedAt).reversed())
                .map(this::toSummary)
                .toList();
    }

    public boolean hasAdminUsers() {
        return userRepo.existsByRolesContaining(UserProfileService.ROLE_ADMIN);
    }

    public ModerationReportResponse submitReport(
            String sessionId,
            ReportTargetType targetType,
            String targetId,
            ReportReasonCode reasonCode,
            String justification
    ) {
        String reporterUserId = sessionResolver.requireUserId(sessionId);

        if (targetType == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        if (targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("targetId is required");
        }
        if (reasonCode == null) {
            throw new IllegalArgumentException("reasonCode is required");
        }
        if (justification == null || justification.trim().length() < 15) {
            throw new IllegalArgumentException("justification must be at least 15 characters");
        }

        TargetResolution resolution = resolveTarget(targetType, targetId.trim(), reporterUserId);

        if (reportRepo.existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(
                reporterUserId,
                targetType,
                resolution.targetId(),
                List.of(ReportStatus.OPEN, ReportStatus.UNDER_REVIEW)
        )) {
            throw new ConflictException("You already have an active report for this target");
        }

        ModerationReport report = new ModerationReport(
                targetType,
                resolution.targetId(),
                resolution.reportedUserId(),
                reporterUserId,
                reasonCode,
                justification.trim(),
                resolution.targetSnapshot()
        );

        ModerationReport savedReport = reportRepo.save(report);
        notifyAdminsOfSubmittedReport(savedReport);
        return toResponse(savedReport);
    }

    public List<ModerationReportResponse> listReportsForReporter(String sessionId) {
        String reporterUserId = sessionResolver.requireUserId(sessionId);
        return reportRepo.findByReporterUserIdOrderByCreatedAtDesc(reporterUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ModerationReportResponse> listReportsForAdmin(String sessionId) {
        sessionResolver.requireAdmin(sessionId);
        return reportRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public ModerationReportResponse getReportForAdmin(String sessionId, String reportId) {
        sessionResolver.requireAdmin(sessionId);
        return toResponse(requireReport(reportId));
    }

    public ModerationReportResponse dismissReport(String sessionId, String reportId, String decisionNotes) {
        String adminUserId = sessionResolver.requireUserId(sessionId);
        sessionResolver.requireAdmin(sessionId);

        ModerationReport report = requireOpenReport(reportId);
        String sanitizedNotes = sanitizeDecisionNotes(decisionNotes);
        report.dismiss(adminUserId, sanitizedNotes);
        ModerationReport saved = reportRepo.save(report);
        notifyReportOutcome(saved, NotificationService.TYPE_REPORT_DISMISSED, "Report dismissed");
        return toResponse(saved);
    }

    public ModerationReportResponse suspendReportedUser(
            String sessionId,
            String reportId,
            Instant suspensionEndsAt,
            String decisionNotes
    ) {
        String adminUserId = sessionResolver.requireUserId(sessionId);
        sessionResolver.requireAdmin(sessionId);

        if (suspensionEndsAt == null || !suspensionEndsAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("suspensionEndsAt must be in the future");
        }

        ModerationReport report = requireOpenReport(reportId);
        UserProfile reportedUser = requireReportedUser(report);
        String sanitizedNotes = sanitizeDecisionNotes(decisionNotes);

        report.suspend(adminUserId, sanitizedNotes, suspensionEndsAt);
        reportedUser.suspendUntil(suspensionEndsAt, sanitizedNotes);

        userRepo.save(reportedUser);
        sessionRepo.deleteByUserId(reportedUser.getUserId());

        ModerationReport saved = reportRepo.save(report);
        notifyReportOutcome(saved, NotificationService.TYPE_REPORT_USER_SUSPENDED, "Report resolved with suspension");
        return toResponse(saved);
    }

    public ModerationReportResponse banReportedUser(String sessionId, String reportId, String decisionNotes) {
        String adminUserId = sessionResolver.requireUserId(sessionId);
        sessionResolver.requireAdmin(sessionId);

        ModerationReport report = requireOpenReport(reportId);
        UserProfile reportedUser = requireReportedUser(report);
        String sanitizedNotes = sanitizeDecisionNotes(decisionNotes);

        report.ban(adminUserId, sanitizedNotes);
        reportedUser.ban(sanitizedNotes);

        userRepo.save(reportedUser);
        sessionRepo.deleteByUserId(reportedUser.getUserId());

        ModerationReport saved = reportRepo.save(report);
        notifyReportOutcome(saved, NotificationService.TYPE_REPORT_USER_BANNED, "Report resolved with ban");
        return toResponse(saved);
    }

    private CreatedAdminInvite createInvite(String createdByUserId, String email, Instant expiresAt, boolean bootstrap) {
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        String rawToken = generateToken();
        AdminInvite invite = new AdminInvite(
                hashToken(rawToken),
                normalizeBoundEmail(email),
                ADMIN_ROLES,
                expiresAt,
                createdByUserId,
                bootstrap
        );

        AdminInvite savedInvite = inviteRepo.save(invite);
        return new CreatedAdminInvite(
                savedInvite.getInviteId(),
                buildInviteLink(rawToken),
                savedInvite.getEmail(),
                savedInvite.getRolesToGrant(),
                savedInvite.getExpiresAt(),
                resolveStatus(savedInvite)
        );
    }

    private AdminInvite requirePendingInvite(String token) {
        if (token == null || token.isBlank()) {
            throw new NotFoundException("Invite token is invalid");
        }

        AdminInvite invite = inviteRepo.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new NotFoundException("Invite token is invalid"));

        String status = resolveStatus(invite);
        if ("REVOKED".equals(status)) {
            throw new ConflictException("This invite has been revoked");
        }
        if ("USED".equals(status)) {
            throw new ConflictException("This invite has already been used");
        }
        if ("EXPIRED".equals(status)) {
            throw new ConflictException("This invite has expired");
        }

        return invite;
    }

    private void validateInviteEmail(AdminInvite invite, String email) {
        String boundEmail = invite.getEmail();
        if (boundEmail == null || boundEmail.isBlank()) {
            return;
        }

        if (email == null || !boundEmail.equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("This invite is only valid for " + boundEmail);
        }
    }

    private boolean claimInvite(String inviteId, Instant claimedAt) {
        Query query = new Query(Criteria.where("inviteId").is(inviteId)
                .and("usedAt").is(null)
                .and("revokedAt").is(null)
                .and("expiresAt").gt(Instant.now()));

        Update update = new Update()
                .set("usedAt", claimedAt)
                .set("usedByUserId", PENDING_REDEMPTION_USER_ID);

        return mongoTemplate.updateFirst(query, update, AdminInvite.class).getModifiedCount() == 1;
    }

    private void releaseInviteClaim(String inviteId, Instant claimedAt) {
        Query query = new Query(Criteria.where("inviteId").is(inviteId)
                .and("usedAt").is(claimedAt)
                .and("usedByUserId").is(PENDING_REDEMPTION_USER_ID));

        Update update = new Update()
                .unset("usedAt")
                .unset("usedByUserId");

        mongoTemplate.updateFirst(query, update, AdminInvite.class);
    }

    private Instant resolveExpiry(Integer expiresInHours, int fallbackHours) {
        int hours = expiresInHours == null ? fallbackHours : expiresInHours;
        if (hours < 1) {
            throw new IllegalArgumentException("expiresInHours must be at least 1");
        }

        return Instant.now().plus(hours, ChronoUnit.HOURS);
    }

    private String resolveStatus(AdminInvite invite) {
        if (invite == null) {
            return "INVALID";
        }
        if (invite.isRevoked()) {
            return "REVOKED";
        }
        if (invite.isUsed()) {
            return "USED";
        }
        if (invite.isExpired()) {
            return "EXPIRED";
        }
        return "PENDING";
    }

    private AdminInviteSummary toSummary(AdminInvite invite) {
        return new AdminInviteSummary(
                invite.getInviteId(),
                invite.getEmail(),
                invite.getRolesToGrant(),
                invite.getExpiresAt(),
                invite.getCreatedAt(),
                invite.getCreatedByUserId(),
                invite.getUsedAt(),
                invite.getUsedByUserId(),
                invite.getRevokedAt(),
                invite.isBootstrap(),
                resolveStatus(invite)
        );
    }

    private String buildInviteLink(String token) {
        String normalizedBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        return normalizedBaseUrl + "/admin-signup?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private String normalizeBoundEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void notifyInviteCreator(AdminInvite invite, UserProfile createdUser) {
        if (invite.isBootstrap() || invite.getCreatedByUserId() == null || invite.getCreatedByUserId().isBlank()) {
            return;
        }

        notificationService.createNotification(
                invite.getCreatedByUserId(),
                NotificationService.TYPE_ADMIN_INVITE_USED,
                "Admin invite redeemed",
                createdUser.getDisplayName() + " redeemed an admin invite.",
                java.util.Map.of("inviteId", invite.getInviteId(), "userId", createdUser.getUserId())
        );
    }

    private ModerationReport requireReport(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            throw new IllegalArgumentException("reportId is required");
        }

        return reportRepo.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));
    }

    private ModerationReport requireOpenReport(String reportId) {
        ModerationReport report = requireReport(reportId);
        if (report.isResolved()) {
            throw new ConflictException("This report has already been resolved");
        }
        return report;
    }

    private UserProfile requireReportedUser(ModerationReport report) {
        return userRepo.findById(report.getReportedUserId())
                .orElseThrow(() -> new NotFoundException("Reported user not found: " + report.getReportedUserId()));
    }

    private TargetResolution resolveTarget(ReportTargetType targetType, String targetId, String reporterUserId) {
        return switch (targetType) {
            case USER -> resolveUserTarget(targetId, reporterUserId);
            case GROUP -> resolveGroupTarget(targetId);
            case POST -> throw new IllegalArgumentException("Post reporting is not available yet");
        };
    }

    private TargetResolution resolveUserTarget(String targetId, String reporterUserId) {
        UserProfile user = userRepo.findById(targetId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetId));

        if (reporterUserId.equals(user.getUserId())) {
            throw new ConflictException("You cannot report your own account");
        }

        return new TargetResolution(
                user.getUserId(),
                user.getUserId(),
                Map.of(
                        "displayName", user.getDisplayName(),
                        "username", user.getUsername(),
                        "accountStatus", user.getAccountStatus().name()
                )
        );
    }

    private TargetResolution resolveGroupTarget(String targetId) {
        StudyGroup group = groupRepo.findById(targetId)
                .orElseThrow(() -> new NotFoundException("Study group not found: " + targetId));

        return new TargetResolution(
                group.getGroupId(),
                group.getCreatedByUserId(),
                Map.of(
                        "title", group.getTitle(),
                        "courseId", group.getCourseId(),
                        "createdByUserId", group.getCreatedByUserId()
                )
        );
    }

    private String sanitizeDecisionNotes(String decisionNotes) {
        if (decisionNotes == null || decisionNotes.isBlank()) {
            throw new IllegalArgumentException("decisionNotes are required");
        }
        return decisionNotes.trim();
    }

    private void notifyAdminsOfSubmittedReport(ModerationReport report) {
        userRepo.findByRolesContaining(UserProfileService.ROLE_ADMIN).stream()
                .filter(admin -> admin.getAccountStatus() == AccountStatus.ACTIVE)
                .forEach(admin -> notificationService.createNotification(
                        admin.getUserId(),
                        NotificationService.TYPE_REPORT_SUBMITTED,
                        "New moderation report submitted",
                        "A new moderation report is waiting for review.",
                        Map.of(
                                "reportId", report.getReportId(),
                                "targetType", report.getTargetType().name(),
                                "targetId", report.getTargetId()
                        )
                ));
    }

    private void notifyReportOutcome(ModerationReport report, String notificationType, String title) {
        notificationService.createNotification(
                report.getReporterUserId(),
                notificationType,
                title,
                buildReporterOutcomeBody(report),
                Map.of(
                        "reportId", report.getReportId(),
                        "decisionType", report.getDecisionType().name(),
                        "targetType", report.getTargetType().name(),
                        "targetId", report.getTargetId()
                )
        );

        if (report.getReportedUserId() != null && !report.getReportedUserId().isBlank()) {
            notificationService.createNotification(
                    report.getReportedUserId(),
                    notificationType,
                    title,
                    buildReportedUserOutcomeBody(report),
                    Map.of(
                            "reportId", report.getReportId(),
                            "decisionType", report.getDecisionType().name()
                    )
            );
        }
    }

    private String buildReporterOutcomeBody(ModerationReport report) {
        if (report.getDecisionType() == null) {
            return "Your report was reviewed.";
        }

        return switch (report.getDecisionType()) {
            case DISMISS -> "Your report was reviewed and dismissed.";
            case SUSPEND -> "Your report was reviewed and the reported user was suspended.";
            case BAN -> "Your report was reviewed and the reported user was banned.";
        };
    }

    private String buildReportedUserOutcomeBody(ModerationReport report) {
        if (report.getDecisionType() == null) {
            return "A moderation report involving your account was reviewed.";
        }

        return switch (report.getDecisionType()) {
            case DISMISS -> "A report involving your account was reviewed and dismissed.";
            case SUSPEND -> "Your account has been suspended following a moderation review.";
            case BAN -> "Your account has been banned following a moderation review.";
        };
    }

    private ModerationReportResponse toResponse(ModerationReport report) {
        return new ModerationReportResponse(
                report.getReportId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReportedUserId(),
                report.getReporterUserId(),
                report.getReasonCode(),
                report.getJustification(),
                report.getStatus(),
                report.getDecisionType(),
                report.getDecisionNotes(),
                report.getSuspensionEndsAt(),
                report.getReviewedByUserId(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                report.getReviewedAt(),
                report.getTargetSnapshot()
        );
    }

    public record CreatedAdminInvite(
            String inviteId,
            String inviteLink,
            String email,
            Set<String> rolesToGrant,
            Instant expiresAt,
            String status
    ) {
    }

    public record InviteValidationResult(
            boolean valid,
            String status,
            String email,
            Instant expiresAt
    ) {
    }

    public record AdminInviteSummary(
            String inviteId,
            String email,
            Set<String> rolesToGrant,
            Instant expiresAt,
            Instant createdAt,
            String createdByUserId,
            Instant usedAt,
            String usedByUserId,
            Instant revokedAt,
            boolean bootstrap,
            String status
    ) {
    }

    private record TargetResolution(
            String targetId,
            String reportedUserId,
            Map<String, String> targetSnapshot
    ) {
    }

    public record ModerationReportResponse(
            String reportId,
            ReportTargetType targetType,
            String targetId,
            String reportedUserId,
            String reporterUserId,
            ReportReasonCode reasonCode,
            String justification,
            ReportStatus status,
            ReportDecisionType decisionType,
            String decisionNotes,
            Instant suspensionEndsAt,
            String reviewedByUserId,
            Instant createdAt,
            Instant updatedAt,
            Instant reviewedAt,
            Map<String, String> targetSnapshot
    ) {
    }
}
