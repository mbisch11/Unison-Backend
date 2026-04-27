package com._bit.Unison.groups.repo;

import com._bit.Unison.groups.model.AttendanceEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AttendanceEventRepository extends MongoRepository<AttendanceEvent, String> {

    List<AttendanceEvent> findByUserId(String userId);

    List<AttendanceEvent> findByUserIdAndGroupId(String userId, String groupId);

    List<AttendanceEvent> findByGroupId(String groupId);

    List<AttendanceEvent> findByGroupIdAndUserId(String groupId, String userId);

    boolean existsByUserIdAndGroupIdAndEventType(String userId, String groupId, String eventType);

    boolean existsByGroupIdAndUserIdAndEventType(String groupId, String userId, String eventType);
}
