package com._bit.Unison.notifications.service;

import com._bit.Unison.auth.service.SessionResolver;
import com._bit.Unison.common.NotFoundException;
import com._bit.Unison.notifications.model.Notification;
import com._bit.Unison.notifications.repo.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    public static final String TYPE_GROUP_JOINED = "GROUP_JOINED";
    public static final String TYPE_GROUP_REMINDER = "GROUP_REMINDER";
    public static final String TYPE_ATTENDANCE_RSVP = "ATTENDANCE_RSVP";
    public static final String TYPE_ATTENDANCE_VERIFIED = "ATTENDANCE_VERIFIED";
    public static final String TYPE_ADMIN_INVITE_USED = "ADMIN_INVITE_USED";
    public static final String TYPE_REPORT_SUBMITTED = "REPORT_SUBMITTED";
    public static final String TYPE_REPORT_DISMISSED = "REPORT_DISMISSED";
    public static final String TYPE_REPORT_USER_SUSPENDED = "REPORT_USER_SUSPENDED";
    public static final String TYPE_REPORT_USER_BANNED = "REPORT_USER_BANNED";

    private final SessionResolver sessionResolver;
    private final NotificationRepository notificationRepo;

    public NotificationService(SessionResolver sessionResolver, NotificationRepository notificationRepo) {
        this.sessionResolver = sessionResolver;
        this.notificationRepo = notificationRepo;
    }

    public List<Notification> listNotifications(String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markRead(String sessionId, String notificationId) {
        String userId = sessionResolver.requireUserId(sessionId);
        Notification notification = notificationRepo.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + notificationId));

        notification.markRead();
        notificationRepo.save(notification);
    }

    public void markAllRead(String sessionId) {
        String userId = sessionResolver.requireUserId(sessionId);
        List<Notification> unreadNotifications = notificationRepo.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(Notification::markRead);
        if (!unreadNotifications.isEmpty()) {
            notificationRepo.saveAll(unreadNotifications);
        }
    }

    public Notification createNotification(String userId, String type, String title, String body, Map<String, String> metadata) {
        return createNotification(userId, type, title, body, metadata, null);
    }

    public Notification createNotification(
            String userId,
            String type,
            String title,
            String body,
            Map<String, String> metadata,
            String dedupeKey
    ) {
        validateCreateRequest(userId, type, title, body);

        if (dedupeKey != null && !dedupeKey.isBlank() && notificationRepo.existsByDedupeKey(dedupeKey)) {
            return null;
        }

        Notification notification = new Notification(userId, type, title, body, metadata, dedupeKey);
        return notificationRepo.save(notification);
    }

    private void validateCreateRequest(String userId, String type, String title, String body) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("body is required");
        }
    }
}
