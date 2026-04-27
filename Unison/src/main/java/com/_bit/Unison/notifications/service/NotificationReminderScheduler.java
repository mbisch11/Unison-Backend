package com._bit.Unison.notifications.service;

import com._bit.Unison.groups.model.GroupMembership;
import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.GroupMembershipRepository;
import com._bit.Unison.groups.repo.StudyGroupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class NotificationReminderScheduler {

    private final StudyGroupRepository groupRepo;
    private final GroupMembershipRepository membershipRepo;
    private final NotificationService notificationService;
    private final long reminderWindowMinutes;

    public NotificationReminderScheduler(
            StudyGroupRepository groupRepo,
            GroupMembershipRepository membershipRepo,
            NotificationService notificationService,
            @Value("${unison.notifications.reminder-window-minutes:30}") long reminderWindowMinutes
    ) {
        this.groupRepo = groupRepo;
        this.membershipRepo = membershipRepo;
        this.notificationService = notificationService;
        this.reminderWindowMinutes = reminderWindowMinutes;
    }

    @Scheduled(fixedDelayString = "${unison.notifications.reminder-rate-ms:300000}")
    public void sendUpcomingGroupReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(reminderWindowMinutes);

        for (StudyGroup group : groupRepo.findByStartTimeBetween(now, windowEnd)) {
            for (GroupMembership membership : membershipRepo.findByGroupId(group.getGroupId())) {
                String dedupeKey = "reminder:" + group.getGroupId() + ":" + membership.getUserId() + ":" + reminderWindowMinutes;
                notificationService.createNotification(
                        membership.getUserId(),
                        NotificationService.TYPE_GROUP_REMINDER,
                        "Study group starts soon",
                        group.getTitle() + " starts at " + group.getStartTime().format(DateTimeFormatter.ofPattern("MMM d, h:mm a")) + ".",
                        Map.of("groupId", group.getGroupId()),
                        dedupeKey
                );
            }
        }
    }
}
