package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TimeWindowFilterStrategy implements GroupFilterStrategy {
    private final LocalDateTime from;
    private final LocalDateTime to;

    public TimeWindowFilterStrategy(LocalDateTime from, LocalDateTime to){
        this.from = from;
        this.to = to;
    }

    @Override
    public List<StudyGroup> filter(List<StudyGroup> groups) {
        return groups.stream().filter(g -> {
            LocalDateTime t = g.getStartTime();
            return t != null && !t.isBefore(from) && !t.isAfter(to);
        }).collect(Collectors.toList());
    }
}
