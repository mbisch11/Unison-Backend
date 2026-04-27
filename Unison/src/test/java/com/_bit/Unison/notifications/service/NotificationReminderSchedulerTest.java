package com._bit.Unison.notifications.service;

import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationReminderSchedulerTest {

    @Test
    void sendUpcomingGroupReminders_createsDedupeProtectedReminders() {
        StudyGroupRepository groupRepo = mock(StudyGroupRepository.class);
        GroupMembershipRepository membershipRepo = mock(GroupMembershipRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationReminderScheduler scheduler = new NotificationReminderScheduler(groupRepo, membershipRepo, notificationService, 30);

        StudyGroup group = new StudyGroup("CMPSC 131", "Midterm Prep", "Review", "Library", false, LocalDateTime.now().plusMinutes(15), 5, "u1");
        ReflectionTestUtils.setField(group, "groupId", "g1");

        when(groupRepo.findByStartTimeBetween(any(), any())).thenReturn(List.of(group));
        when(membershipRepo.findByGroupId("g1")).thenReturn(List.of(new GroupMembership("g1", "u2", false)));

        scheduler.sendUpcomingGroupReminders();

        verify(notificationService).createNotification(
                eq("u2"),
                eq(NotificationService.TYPE_GROUP_REMINDER),
                eq("Study group starts soon"),
                any(),
                eq(java.util.Map.of("groupId", "g1")),
                eq("reminder:g1:u2:30")
        );
    }
}
