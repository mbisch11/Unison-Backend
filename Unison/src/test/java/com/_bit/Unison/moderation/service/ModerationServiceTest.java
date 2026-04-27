package com._bit.Unison.moderation.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.auth.repo.AuthSessionRepository;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.moderation.model.AdminInvite;
import com._bit.Unison.moderation.model.ReportDecisionType;
import com._bit.Unison.moderation.model.ReportReasonCode;
import com._bit.Unison.moderation.model.ReportStatus;
import com._bit.Unison.moderation.model.ReportTargetType;
import com._bit.Unison.moderation.repo.AdminInviteRepository;
import com._bit.Unison.moderation.repo.ModerationReportRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.notifications.service.NotificationService;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import com._bit.Unison.users.service.UserProfileService;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationServiceTest {

    @Test
    void createAdminInvite_returnsOneTimeLink() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        doNothing().when(sessionResolver).requireAdmin("admin-session");
        when(sessionResolver.requireUserId("admin-session")).thenReturn("admin-1");
        when(inviteRepo.save(any(AdminInvite.class))).thenAnswer(invocation -> {
            AdminInvite invite = invocation.getArgument(0);
            ReflectionTestUtils.setField(invite, "inviteId", "invite-1");
            return invite;
        });

        ModerationService.CreatedAdminInvite createdInvite = service.createAdminInvite(
                "admin-session",
                "newadmin@example.com",
                24
        );

        assertEquals("invite-1", createdInvite.inviteId());
        assertEquals("newadmin@example.com", createdInvite.email());
        assertEquals("PENDING", createdInvite.status());
        assertNotNull(createdInvite.inviteLink());
    }

    @Test
    void validateInviteToken_whenMissing_returnsInvalid() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        ModerationService.InviteValidationResult result = service.validateInviteToken("missing-token");

        assertFalse(result.valid());
        assertEquals("INVALID", result.status());
    }

    @Test
    void redeemAdminInvite_claimsInviteAndCreatesAdminUser() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );
        when(inviteRepo.save(any(AdminInvite.class))).thenAnswer(invocation -> {
            AdminInvite invite = invocation.getArgument(0);
            if (invite.getInviteId() == null) {
                ReflectionTestUtils.setField(invite, "inviteId", "invite-1");
            }
            return invite;
        });

        AdminInvite invite = new AdminInvite(
                "hashed-token",
                "admin@example.com",
                Set.of("USER", "ADMIN"),
                Instant.now().plusSeconds(3600),
                "BOOTSTRAP",
                true
        );
        ReflectionTestUtils.setField(invite, "inviteId", "invite-1");
        when(inviteRepo.findByTokenHash(any())).thenReturn(Optional.of(invite));
        when(mongoTemplate.updateFirst(any(), any(), eq(AdminInvite.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));

        UserProfile adminUser = new UserProfile(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "admin@example.com",
                "adminuser",
                "hash",
                Set.of("HCDD440"),
                Set.of("USER", "ADMIN")
        );
        ReflectionTestUtils.setField(adminUser, "userId", "admin-1");
        when(userService.registerUser(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "password123",
                Set.of("HCDD440"),
                Set.of("USER", "ADMIN")
        )).thenReturn(adminUser);

        UserProfile result = service.redeemAdminInvite(
                "bootstrap-token",
                "Admin User",
                "admin@example.com",
                "adminuser",
                "password123",
                Set.of("HCDD440")
        );

        assertEquals("admin-1", result.getUserId());
        verify(inviteRepo).save(any(AdminInvite.class));
        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
    }

    @Test
    void revokeInvite_whenUsed_throwsConflict() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        doNothing().when(sessionResolver).requireAdmin("admin-session");

        AdminInvite invite = new AdminInvite(
                "hash",
                "admin@example.com",
                Set.of("USER", "ADMIN"),
                Instant.now().plusSeconds(3600),
                "admin-1",
                false
        );
        ReflectionTestUtils.setField(invite, "inviteId", "invite-1");
        invite.markRedeemedBy("admin-2", Instant.now());
        when(inviteRepo.findById("invite-1")).thenReturn(Optional.of(invite));

        assertThrows(ConflictException.class, () -> service.revokeInvite("admin-session", "invite-1"));
        verify(inviteRepo, never()).save(any(AdminInvite.class));
    }

    @Test
    void redeemAdminInvite_notifiesInviteCreatorForNonBootstrapInvites() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        AdminInvite invite = new AdminInvite(
                "hashed-token",
                "admin@example.com",
                Set.of("USER", "ADMIN"),
                Instant.now().plusSeconds(3600),
                "admin-owner",
                false
        );
        ReflectionTestUtils.setField(invite, "inviteId", "invite-2");
        when(inviteRepo.findByTokenHash(any())).thenReturn(Optional.of(invite));
        when(inviteRepo.save(any(AdminInvite.class))).thenReturn(invite);
        when(mongoTemplate.updateFirst(any(), any(), eq(AdminInvite.class)))
                .thenReturn(UpdateResult.acknowledged(1L, 1L, null));

        UserProfile adminUser = new UserProfile(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "admin@example.com",
                "adminuser",
                "hash",
                Set.of(),
                Set.of("USER", "ADMIN")
        );
        ReflectionTestUtils.setField(adminUser, "userId", "admin-1");
        when(userService.registerUser(any(), any(), any(), any(), any(), any())).thenReturn(adminUser);

        service.redeemAdminInvite("invite-token", "Admin User", "admin@example.com", "adminuser", "password123", Set.of());

        verify(notificationService).createNotification(eq("admin-owner"), eq(NotificationService.TYPE_ADMIN_INVITE_USED), eq("Admin invite redeemed"), eq("Admin User redeemed an admin invite."), any());
    }

    @Test
    void submitReport_forUserTarget_savesReportAndNotifiesAdmins() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        UserProfile reportedUser = new UserProfile(
                "Reported User",
                "reported@example.com",
                "reporteduser",
                "reported@example.com",
                "reporteduser",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(reportedUser, "userId", "user-2");

        UserProfile adminUser = new UserProfile(
                "Admin User",
                "admin@example.com",
                "adminuser",
                "admin@example.com",
                "adminuser",
                "hash",
                Set.of(),
                Set.of("USER", "ADMIN")
        );
        ReflectionTestUtils.setField(adminUser, "userId", "admin-1");

        when(sessionResolver.requireUserId("user-session")).thenReturn("user-1");
        when(userRepo.findById("user-2")).thenReturn(Optional.of(reportedUser));
        when(reportRepo.existsByReporterUserIdAndTargetTypeAndTargetIdAndStatusIn(eq("user-1"), eq(ReportTargetType.USER), eq("user-2"), any()))
                .thenReturn(false);
        when(reportRepo.save(any())).thenAnswer(invocation -> {
            Object report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "reportId", "report-1");
            return report;
        });
        when(userRepo.findByRolesContaining(UserProfileService.ROLE_ADMIN)).thenReturn(List.of(adminUser));

        ModerationService.ModerationReportResponse response = service.submitReport(
                "user-session",
                ReportTargetType.USER,
                "user-2",
                ReportReasonCode.HARASSMENT,
                "This user sent repeated abusive messages."
        );

        assertEquals("report-1", response.reportId());
        assertEquals(ReportStatus.OPEN, response.status());
        verify(notificationService).createNotification(eq("admin-1"), eq(NotificationService.TYPE_REPORT_SUBMITTED), eq("New moderation report submitted"), eq("A new moderation report is waiting for review."), any());
    }

    @Test
    void suspendReportedUser_updatesUserAndRevokesSessions() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        AuthSessionRepository sessionRepo = mock(AuthSessionRepository.class);
        AdminInviteRepository inviteRepo = mock(AdminInviteRepository.class);
        ModerationReportRepository reportRepo = mock(ModerationReportRepository.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        UserProfileService userService = mock(UserProfileService.class);
        NotificationService notificationService = mock(NotificationService.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        ModerationService service = new ModerationService(
                sessionResolver,
                sessionRepo,
                inviteRepo,
                reportRepo,
                groupRepo,
                userRepo,
                userService,
                notificationService,
                mongoTemplate,
                "http://localhost:5173"
        );

        when(sessionResolver.requireUserId("admin-session")).thenReturn("admin-1");
        doNothing().when(sessionResolver).requireAdmin("admin-session");

        com._bit.Unison.moderation.model.ModerationReport report = new com._bit.Unison.moderation.model.ModerationReport(
                ReportTargetType.USER,
                "user-2",
                "user-2",
                "user-1",
                ReportReasonCode.HARASSMENT,
                "This user sent repeated abusive messages.",
                Map.of("displayName", "Reported User")
        );
        ReflectionTestUtils.setField(report, "reportId", "report-2");

        UserProfile reportedUser = new UserProfile(
                "Reported User",
                "reported@example.com",
                "reporteduser",
                "reported@example.com",
                "reporteduser",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(reportedUser, "userId", "user-2");

        when(reportRepo.findById("report-2")).thenReturn(Optional.of(report));
        when(userRepo.findById("user-2")).thenReturn(Optional.of(reportedUser));
        when(reportRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant suspensionEndsAt = Instant.now().plusSeconds(7200);
        ModerationService.ModerationReportResponse response = service.suspendReportedUser(
                "admin-session",
                "report-2",
                suspensionEndsAt,
                "Repeated harassment confirmed."
        );

        assertEquals(ReportDecisionType.SUSPEND, response.decisionType());
        verify(userRepo).save(reportedUser);
        verify(sessionRepo).deleteByUserId("user-2");
        verify(notificationService).createNotification(eq("user-1"), eq(NotificationService.TYPE_REPORT_USER_SUSPENDED), eq("Report resolved with suspension"), any(), any());
    }
}
