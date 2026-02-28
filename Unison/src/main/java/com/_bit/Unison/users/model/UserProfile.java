package com._bit.Unison.users.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class UserProfile {

    @Id
    private String userId;
    private String displayName;
    private Set<String> courseIds;

    public UserProfile(){}

    public UserProfile(String dn, Set<String> cId){
        this.displayName = dn;
        this.courseIds = cId;
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

    public Set<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(Set<String> courseIds){
        this.courseIds = (courseIds == null) ? new HashSet<>() : courseIds;
    }
}
