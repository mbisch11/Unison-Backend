package com._bit.Unison.notifications.repo;

import com._bit.Unison.notifications.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId);

    Optional<Notification> findByNotificationIdAndUserId(String notificationId, String userId);

    boolean existsByDedupeKey(String dedupeKey);
}
