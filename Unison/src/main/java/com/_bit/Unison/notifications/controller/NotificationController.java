package com._bit.Unison.notifications.controller;

import com._bit.Unison.notifications.model.Notification;
import com._bit.Unison.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> listNotifications(@RequestHeader("X-Session-Id") String sessionId) {
        return notificationService.listNotifications(sessionId);
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable String notificationId
    ) {
        notificationService.markRead(sessionId, notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@RequestHeader("X-Session-Id") String sessionId) {
        notificationService.markAllRead(sessionId);
    }
}
