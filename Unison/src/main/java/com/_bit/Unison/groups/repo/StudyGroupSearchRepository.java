package com._bit.Unison.groups.repo;

import com._bit.Unison.groups.model.StudyGroup;

import java.util.List;
import java.time.LocalDateTime;

public interface StudyGroupSearchRepository {
    List<StudyGroup> findByCourse(String courseId);

    List<StudyGroup> findByCourseAndTimeRange(String courseId, LocalDateTime from, LocalDateTime to, boolean isVirtual);

    List<StudyGroup> findAvailableGroups(String courseId);

    List<StudyGroup> findByCreator(String userId);

    StudyGroup findByIdOrThrow(String groupId);
}
