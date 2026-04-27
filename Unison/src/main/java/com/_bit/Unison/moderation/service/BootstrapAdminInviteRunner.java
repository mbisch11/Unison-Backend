package com._bit.Unison.moderation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInviteRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInviteRunner.class);

    private final ModerationService moderationService;
    private final boolean bootstrapEnabled;
    private final String bootstrapEmail;
    private final long bootstrapExpiresHours;

    public BootstrapAdminInviteRunner(
            ModerationService moderationService,
            @Value("${unison.bootstrap-admin.enabled:false}") boolean bootstrapEnabled,
            @Value("${unison.bootstrap-admin.email:}") String bootstrapEmail,
            @Value("${unison.bootstrap-admin.expires-hours:24}") long bootstrapExpiresHours
    ) {
        this.moderationService = moderationService;
        this.bootstrapEnabled = bootstrapEnabled;
        this.bootstrapEmail = bootstrapEmail;
        this.bootstrapExpiresHours = bootstrapExpiresHours;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!bootstrapEnabled || moderationService.hasAdminUsers()) {
            return;
        }

        ModerationService.CreatedAdminInvite invite = moderationService.resetBootstrapInvite(
                bootstrapEmail,
                bootstrapExpiresHours
        );

        log.warn("Bootstrap admin invite created. Save this one-time link now: {}", invite.inviteLink());
    }
}
