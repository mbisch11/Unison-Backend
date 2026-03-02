package com._bit.Unison.groups.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "group_membership")
public class GroupMembership {

    @Id
    private String membershipId;
    private String groupId;
    private String userId;
    private boolean isLeader;

    public GroupMembership(String groupId, String userId, boolean isLeader){
        this.groupId = groupId;
        this.userId = userId;
        this.isLeader = isLeader;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void changeLeader() {
        this.isLeader = !this.isLeader;
    }
}
