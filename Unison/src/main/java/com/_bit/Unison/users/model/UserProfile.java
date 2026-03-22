package com._bit.Unison.users.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class UserProfile {

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

    public UserProfile(){}

    public UserProfile(String displayName, String email, String username, String normalizedEmail, String normalizedUsername, String passwordHash, Set<String> courseIds){
        this.displayName = displayName;
        this.courseIds = (courseIds == null) ? new HashSet<>() : new HashSet<>(courseIds);
        this.email = email;
        this.username = username;
        this.normalizedEmail = normalizedEmail;
        this.normalizedUsername = normalizedUsername;
        this.passwordHash = passwordHash;
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

    public String getUsername() {
        return username;
    }

    public String getNormalizedEmail() {
        return normalizedEmail;
    }

    public String getNormalizedUsername() {
        return normalizedUsername;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(Set<String> courseIds){
        this.courseIds = (courseIds == null) ? new HashSet<>() : courseIds;
    }
}
