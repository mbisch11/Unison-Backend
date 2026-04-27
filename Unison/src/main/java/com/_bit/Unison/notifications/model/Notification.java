package com._bit.Unison.notifications.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String body;
    private Map<String, String> metadata;
    private Instant createdAt;
    private Instant readAt;
    @Indexed(unique = true, sparse = true)
    private String dedupeKey;

    public Notification() {
    }

    public Notification(String userId, String type, String title, String body, Map<String, String> metadata, String dedupeKey) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        this.createdAt = Instant.now();
        this.dedupeKey = dedupeKey;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getMetadata() {
        return metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
