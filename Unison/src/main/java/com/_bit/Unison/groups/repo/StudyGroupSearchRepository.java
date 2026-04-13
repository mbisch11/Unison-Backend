package com._bit.Unison.groups.repo;

import com._bit.Unison.groups.model.StudyGroup;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StudyGroupSearchRepository extends MongoRepository<StudyGroup, String> {

    List<StudyGroup> findByCourseId(String courseId);

    List<StudyGroup> findByCreatedByUserId(String userId);
}