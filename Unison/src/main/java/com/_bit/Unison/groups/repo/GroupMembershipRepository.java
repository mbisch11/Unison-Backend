package com._bit.Unison.groups.repo;

import com._bit.Unison.groups.model.GroupMembership;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends MongoRepository<GroupMembership, String> {
    boolean existsByGroupIdAndUserId(String groupId, String userId);

    long countByGroupId(String groupId);

    Optional<GroupMembership> findByGroupIdAndUserId(String groupId, String userId);

    List<GroupMembership> findByGroupId(String groupId);

    List<GroupMembership> findByUserId(String userId);
}