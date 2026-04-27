package com._bit.Unison.groups.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.ConflictException;
import com._bit.Unison.groups.dto.AttendanceStatus;
import com._bit.Unison.groups.dto.ManagedStudyGroupDto;
import com._bit.Unison.groups.model.AttendanceEvent;
import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.AttendanceEventRepository;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import com._bit.Unison.groups.repo.StudyGroupSearchRepository;
import com._bit.Unison.notifications.service.NotificationService;
import com._bit.Unison.users.model.UserProfile;
import com._bit.Unison.users.repo.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudyGroupServiceTest {

    @Test
    void createGroup_addsCreatorMembership_andJoinEvent() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        StudyGroup savedGroup = new StudyGroup("CMPSC 131", "Midterm Prep", "Review", "Library", false, startTime, 5, "u1");
        ReflectionTestUtils.setField(savedGroup, "groupId", "g1");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(groupRepo.save(any(StudyGroup.class))).thenReturn(savedGroup);

        StudyGroup result = service.createGroup("s1", "CMPSC 131", "Midterm Prep", "Review", "Library", false, startTime, 5);

        ArgumentCaptor<GroupMembership> membershipCaptor = ArgumentCaptor.forClass(GroupMembership.class);
        ArgumentCaptor<AttendanceEvent> attendanceCaptor = ArgumentCaptor.forClass(AttendanceEvent.class);

        verify(membershipRepo).save(membershipCaptor.capture());
        verify(attendanceRepo).save(attendanceCaptor.capture());

        assertEquals("g1", result.getGroupId());
        assertEquals("g1", membershipCaptor.getValue().getGroupId());
        assertTrue(membershipCaptor.getValue().isLeader());
        assertEquals("JOINED", attendanceCaptor.getValue().getEventType());
    }

    @Test
    void createGroup_requiresLocationForInPersonGroups() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");

        assertThrows(IllegalArgumentException.class, () ->
                service.createGroup("s1", "CMPSC 131", "Midterm Prep", "Review", "   ", false, LocalDateTime.now().plusDays(1), 5)
        );

        verify(groupRepo, never()).save(any());
    }

    @Test
    void joinGroup_whenAlreadyJoined_throwsConflict() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().plusDays(1), 5)));
        when(membershipRepo.existsByGroupIdAndUserId("g1", "u1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.joinGroup("s1", "g1"));
        verify(membershipRepo, never()).save(any());
    }

    @Test
    void rsvpAttendance_whenAlreadySubmitted_throwsConflict() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().plusHours(2), 5)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "u1")).thenReturn(Optional.of(new GroupMembership("g1", "u1", false)));
        when(attendanceRepo.findByGroupIdAndUserId("g1", "u1")).thenReturn(List.of(
                new AttendanceEvent("g1", "u1", "JOINED"),
                new AttendanceEvent("g1", "u1", "RSVPED")
        ));

        assertThrows(ConflictException.class, () -> service.rsvpAttendance("s1", "g1"));
        verify(attendanceRepo, never()).save(any());
    }

    @Test
    void getAvailableGroups_filtersOutJoinedPastAndFullGroups() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        StudyGroup openGroup = studyGroup("g1", LocalDateTime.now().plusDays(1), 3);
        StudyGroup fullGroup = studyGroup("g2", LocalDateTime.now().plusDays(1), 1);
        StudyGroup joinedGroup = studyGroup("g3", LocalDateTime.now().plusDays(1), 3);
        StudyGroup pastGroup = studyGroup("g4", LocalDateTime.now().minusDays(1), 3);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(groupSearchRepo.findByCourseIdIgnoreCase("CMPSC 131")).thenReturn(List.of(openGroup, fullGroup, joinedGroup, pastGroup));
        when(membershipRepo.countByGroupId("g1")).thenReturn(1L);
        when(membershipRepo.countByGroupId("g2")).thenReturn(1L);
        when(membershipRepo.countByGroupId("g3")).thenReturn(0L);
        when(membershipRepo.countByGroupId("g4")).thenReturn(0L);
        when(membershipRepo.existsByGroupIdAndUserId(eq("g3"), eq("u1"))).thenReturn(true);
        when(membershipRepo.existsByGroupIdAndUserId(eq("g1"), eq("u1"))).thenReturn(false);
        when(membershipRepo.existsByGroupIdAndUserId(eq("g2"), eq("u1"))).thenReturn(false);

        List<StudyGroup> result = service.getAvailableGroups("s1", "CMPSC 131");

        assertEquals(1, result.size());
        assertEquals("g1", result.get(0).getGroupId());
    }

    @Test
    void getMyJoinedGroups_returnsSortedResolvedGroups_withAttendanceStatus() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        GroupMembership firstMembership = new GroupMembership("g2", "u1", false);
        GroupMembership secondMembership = new GroupMembership("g1", "u1", false);

        StudyGroup earlyGroup = studyGroup("g1", LocalDateTime.now().plusDays(1), 5);
        StudyGroup laterGroup = studyGroup("g2", LocalDateTime.now().plusDays(2), 5);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(membershipRepo.findByUserId("u1")).thenReturn(List.of(firstMembership, secondMembership));
        when(groupRepo.findById("g1")).thenReturn(Optional.of(earlyGroup));
        when(groupRepo.findById("g2")).thenReturn(Optional.of(laterGroup));
        when(attendanceRepo.findByGroupIdAndUserId("g1", "u1")).thenReturn(List.of(
                new AttendanceEvent("g1", "u1", "JOINED"),
                new AttendanceEvent("g1", "u1", "RSVPED")
        ));
        when(attendanceRepo.findByGroupIdAndUserId("g2", "u1")).thenReturn(List.of(
                new AttendanceEvent("g2", "u1", "JOINED")
        ));

        List<ManagedStudyGroupDto> result = service.getMyJoinedGroups("s1");

        assertEquals(2, result.size());
        assertEquals("g1", result.get(0).groupId());
        assertEquals(AttendanceStatus.RSVPED, result.get(0).currentUserAttendanceStatus());
        assertEquals("g2", result.get(1).groupId());
        assertEquals(AttendanceStatus.NOT_RESPONDED, result.get(1).currentUserAttendanceStatus());
    }

    @Test
    void joinGroup_notifiesCreatorWhenMemberJoins() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        UserProfile joiningUser = new UserProfile(
                "Michael",
                "michael@example.com",
                "michael",
                "michael@example.com",
                "michael",
                "hash",
                Set.of()
        );
        ReflectionTestUtils.setField(joiningUser, "userId", "u2");

        when(sessionResolver.requireUserId("s1")).thenReturn("u2");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().plusDays(1), 5)));
        when(membershipRepo.existsByGroupIdAndUserId("g1", "u2")).thenReturn(false);
        when(membershipRepo.countByGroupId("g1")).thenReturn(1L);
        when(userRepo.findById("u2")).thenReturn(Optional.of(joiningUser));

        service.joinGroup("s1", "g1");

        verify(notificationService).createNotification(eq("u1"), eq(NotificationService.TYPE_GROUP_JOINED), eq("New member joined your group"), eq("Michael joined Title."), any());
    }

    @Test
    void rsvpAttendance_createsRsvpNotification() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().plusHours(3), 5)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "u1")).thenReturn(Optional.of(new GroupMembership("g1", "u1", false)));
        when(attendanceRepo.findByGroupIdAndUserId("g1", "u1")).thenReturn(List.of(new AttendanceEvent("g1", "u1", "JOINED")));

        service.rsvpAttendance("s1", "g1");

        verify(notificationService).createNotification(eq("u1"), eq(NotificationService.TYPE_ATTENDANCE_RSVP), eq("Attendance RSVP saved"), eq("You're marked as attending Title."), any());
    }

    @Test
    void verifyAttendance_requiresRsvpBeforeReview() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("leader-1");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().minusHours(1), 5)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "leader-1")).thenReturn(Optional.of(new GroupMembership("g1", "leader-1", true)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "member-1")).thenReturn(Optional.of(new GroupMembership("g1", "member-1", false)));
        when(attendanceRepo.findByGroupIdAndUserId("g1", "member-1")).thenReturn(List.of(new AttendanceEvent("g1", "member-1", "JOINED")));

        assertThrows(ConflictException.class, () -> service.verifyAttendance("s1", "g1", "member-1", true));
        verify(attendanceRepo, never()).save(any());
    }

    @Test
    void verifyAttendance_createsVerificationNotification_andRecordsLeader() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        StudyGroupSearchRepository groupSearchRepo = mock(StudyGroupSearchRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        AttendanceEventRepository attendanceRepo = mock(AttendanceEventRepository.class);
        UserProfileRepository userRepo = mock(UserProfileRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        StudyGroupService service = new StudyGroupService(sessionResolver, groupRepo, groupSearchRepo, membershipRepo, attendanceRepo, userRepo, notificationService);

        when(sessionResolver.requireUserId("s1")).thenReturn("leader-1");
        when(groupRepo.findById("g1")).thenReturn(Optional.of(studyGroup("g1", LocalDateTime.now().minusHours(1), 5)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "leader-1")).thenReturn(Optional.of(new GroupMembership("g1", "leader-1", true)));
        when(membershipRepo.findByGroupIdAndUserId("g1", "member-1")).thenReturn(Optional.of(new GroupMembership("g1", "member-1", false)));
        when(attendanceRepo.findByGroupIdAndUserId("g1", "member-1")).thenReturn(List.of(
                new AttendanceEvent("g1", "member-1", "JOINED"),
                new AttendanceEvent("g1", "member-1", "RSVPED")
        ));

        service.verifyAttendance("s1", "g1", "member-1", true);

        ArgumentCaptor<AttendanceEvent> attendanceCaptor = ArgumentCaptor.forClass(AttendanceEvent.class);
        verify(attendanceRepo).save(attendanceCaptor.capture());
        assertEquals("leader-1", attendanceCaptor.getValue().getRecordedByUserId());
        assertEquals("VERIFIED_ATTENDED", attendanceCaptor.getValue().getEventType());
        verify(notificationService).createNotification(eq("member-1"), eq(NotificationService.TYPE_ATTENDANCE_VERIFIED), eq("Attendance verified"), eq("Your leader marked you present for Title."), any());
    }

    private StudyGroup studyGroup(String groupId, LocalDateTime startTime, int maxCapacity) {
        StudyGroup group = new StudyGroup("CMPSC 131", "Title", "Desc", "Library", false, startTime, maxCapacity, "u1");
        ReflectionTestUtils.setField(group, "groupId", groupId);
        return group;
    }
}
