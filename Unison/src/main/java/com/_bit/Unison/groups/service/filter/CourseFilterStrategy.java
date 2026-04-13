package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;

import java.util.List;
import java.util.stream.Collectors;

public class CourseFilterStrategy implements GroupFilterStrategy {
    private final String courseId;

    public CourseFilterStrategy(String courseId){
        this.courseId = courseId;
    }

    @Override
    public List<StudyGroup> filter(List<StudyGroup> groups) {
        return groups.stream().filter(g -> courseId.equalsIgnoreCase(g.getCourseId())).collect(Collectors.toList());
    }
}
