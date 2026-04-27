package com._bit.Unison.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Document(collection = "users")
public class UserProfile {
    private static final DateTimeFormatter RESTRICTION_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("MMMM d, yyyy 'at' h:mm a z", Locale.US)
            .withZone(ZoneId.systemDefault());

    @Id
    private String userId;
    private String displayName;
    private Set<String> courseIds;
    @Indexed(unique = true)
    private String email;
    @Indexed(unique = true)
    private String username;
    @Indexed(unique = true)
    private String normalizedEmail;
    @Indexed(unique = true)
    private String normalizedUsername;
    private String passwordHash;
    private Set<String> roles;
    private AccountStatus accountStatus;
    private Instant suspensionEndsAt;
    private String moderationReason;

    public UserProfile(){}

    public UserProfile(String displayName, String email, String username, String normalizedEmail, String normalizedUsername, String passwordHash, Set<String> courseIds){
        this(displayName, email, username, normalizedEmail, normalizedUsername, passwordHash, courseIds, Set.of("USER"));
    }

    public UserProfile(
            String displayName,
            String email,
            String username,
            String normalizedEmail,
            String normalizedUsername,
            String passwordHash,
            Set<String> courseIds,
            Set<String> roles
    ){
        this.displayName = displayName;
        this.courseIds = (courseIds == null) ? new HashSet<>() : new HashSet<>(courseIds);
        this.email = email;
        this.username = username;
        this.normalizedEmail = normalizedEmail;
        this.normalizedUsername = normalizedUsername;
        this.passwordHash = passwordHash;
        this.roles = (roles == null) ? new HashSet<>() : new HashSet<>(roles);
        this.accountStatus = AccountStatus.ACTIVE;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void updateDisplayName(String displayName){
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void updateEmail(String email, String normalizedEmail) {
        this.email = email;
        this.normalizedEmail = normalizedEmail;
    }

    public String getUsername() {
        return username;
    }

    public void updateUsername(String username, String normalizedUsername) {
        this.username = username;
        this.normalizedUsername = normalizedUsername;
    }

    @JsonIgnore
    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    @JsonIgnore
    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(Set<String> courseIds){
        this.courseIds = (courseIds == null) ? new HashSet<>() : new HashSet<>(courseIds);
    }

    public Set<String> getRoles() {
        return roles == null ? Set.of() : new HashSet<>(roles);
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public AccountStatus getAccountStatus() {
        return accountStatus == null ? AccountStatus.ACTIVE : accountStatus;
    }

    public Instant getSuspensionEndsAt() {
        return suspensionEndsAt;
    }

    public String getModerationReason() {
        return moderationReason;
    }

    public boolean isBanned() {
        return getAccountStatus() == AccountStatus.BANNED;
    }

    public boolean isSuspended() {
        return getAccountStatus() == AccountStatus.SUSPENDED
                && (suspensionEndsAt == null || suspensionEndsAt.isAfter(Instant.now()));
    }

    @JsonIgnore
    public boolean isSuspensionExpired() {
        return getAccountStatus() == AccountStatus.SUSPENDED
                && suspensionEndsAt != null
                && !suspensionEndsAt.isAfter(Instant.now());
    }

    public void suspendUntil(Instant suspensionEndsAt, String moderationReason) {
        this.accountStatus = AccountStatus.SUSPENDED;
        this.suspensionEndsAt = suspensionEndsAt;
        this.moderationReason = moderationReason;
    }

    public void ban(String moderationReason) {
        this.accountStatus = AccountStatus.BANNED;
        this.suspensionEndsAt = null;
        this.moderationReason = moderationReason;
    }

    public boolean clearExpiredSuspension() {
        if (!isSuspensionExpired()) {
            return false;
        }

        this.accountStatus = AccountStatus.ACTIVE;
        this.suspensionEndsAt = null;
        this.moderationReason = null;
        return true;
    }

    public String buildRestrictionMessage() {
        if (isBanned()) {
            return moderationReason == null || moderationReason.isBlank()
                    ? "Your account has been banned."
                    : "Your account has been banned. Reason: " + moderationReason;
        }

        if (isSuspended()) {
            String untilMessage = suspensionEndsAt == null
                    ? "Your account is suspended."
                    : "Your account is suspended until " + RESTRICTION_TIME_FORMATTER.format(suspensionEndsAt) + ".";

            if (moderationReason == null || moderationReason.isBlank()) {
                return untilMessage;
            }

            return untilMessage + " Reason: " + moderationReason;
        }

        return "Your account is active.";
    }
}
