package com._bit.Unison.groups.repo;

import com._bit.Unison.groups.model.StudyGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StudyGroupRepository extends MongoRepository<StudyGroup, String> {

    List<StudyGroup> findByCourseId(String courseId);

    List<StudyGroup> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    List<StudyGroup> findByCourseIdAndStartTimeBetween(String courseId, LocalDateTime from, LocalDateTime to);

    List<StudyGroup> findByCourseIdAndIsVirtualAndStartTimeBetween(
            String courseId, boolean isVirtual, LocalDateTime from, LocalDateTime to
    );
}