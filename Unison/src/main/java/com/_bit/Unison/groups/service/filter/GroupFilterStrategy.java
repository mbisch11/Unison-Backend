package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;

import java.util.List;

public interface GroupFilterStrategy {
    List<StudyGroup> filter(List<StudyGroup> groups);
}
