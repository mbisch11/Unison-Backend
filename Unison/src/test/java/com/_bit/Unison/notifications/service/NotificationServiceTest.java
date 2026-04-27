package com._bit.Unison.notifications.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.notifications.model.Notification;
import com._bit.Unison.notifications.repo.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void createNotification_savesNotification() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(sessionResolver, notificationRepo);

        when(notificationRepo.existsByDedupeKey("join:u1:g1")).thenReturn(false);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = service.createNotification(
                "u1",
                NotificationService.TYPE_GROUP_JOINED,
                "Someone joined your group",
                "Michael joined Midterm Prep.",
                Map.of("groupId", "g1"),
                "join:u1:g1"
        );

        assertNotNull(notification);
        assertEquals("u1", notification.getUserId());
        assertEquals(NotificationService.TYPE_GROUP_JOINED, notification.getType());
    }

    @Test
    void moderationNotificationType_isAccepted() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(sessionResolver, notificationRepo);

        when(notificationRepo.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = service.createNotification(
                "u1",
                NotificationService.TYPE_REPORT_USER_BANNED,
                "Report resolved with ban",
                "Your report was reviewed and the reported user was banned.",
                Map.of("reportId", "report-1")
        );

        assertEquals(NotificationService.TYPE_REPORT_USER_BANNED, notification.getType());
    }

    @Test
    void createNotification_withDuplicateDedupeKey_skipsSave() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(sessionResolver, notificationRepo);

        when(notificationRepo.existsByDedupeKey("reminder:g1:u1:30m")).thenReturn(true);

        Notification notification = service.createNotification(
                "u1",
                NotificationService.TYPE_GROUP_REMINDER,
                "Group reminder",
                "Your study group starts soon.",
                Map.of("groupId", "g1"),
                "reminder:g1:u1:30m"
        );

        assertEquals(null, notification);
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void markRead_whenNotificationMissing_throwsNotFound() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(sessionResolver, notificationRepo);

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(notificationRepo.findByNotificationIdAndUserId("n1", "u1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.markRead("s1", "n1"));
    }

    @Test
    void markAllRead_marksUnreadNotifications() {
        SessionResolver sessionResolver = mock(SessionResolver.class);
        NotificationRepository notificationRepo = mock(NotificationRepository.class);
        NotificationService service = new NotificationService(sessionResolver, notificationRepo);

        Notification first = new Notification("u1", "TYPE", "Title", "Body", Map.of(), null);
        Notification second = new Notification("u1", "TYPE", "Title", "Body", Map.of(), null);
        ReflectionTestUtils.setField(first, "notificationId", "n1");
        ReflectionTestUtils.setField(second, "notificationId", "n2");

        when(sessionResolver.requireUserId("s1")).thenReturn("u1");
        when(notificationRepo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc("u1")).thenReturn(List.of(first, second));

        service.markAllRead("s1");

        verify(notificationRepo).saveAll(List.of(first, second));
    }
}
