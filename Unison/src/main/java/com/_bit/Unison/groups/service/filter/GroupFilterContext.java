package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;

import java.util.ArrayList;
import java.util.List;

public class GroupFilterContext {

    private final List<GroupFilterStrategy> strategies = new ArrayList<>();

    public GroupFilterContext addStrategy(GroupFilterStrategy strategy) {
        strategies.add(strategy);
        return this;
    }

    public List<StudyGroup> applyAll(List<StudyGroup> candidates){
        List<StudyGroup> result = new ArrayList<>(candidates);
        for (GroupFilterStrategy strategy : strategies){
            result = strategy.filter(result);
        }
        return result;
    }
}
