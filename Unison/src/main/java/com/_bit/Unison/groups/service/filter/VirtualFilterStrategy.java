package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;

import java.util.List;
import java.util.stream.Collectors;

public class VirtualFilterStrategy implements GroupFilterStrategy{
    private final boolean virtual;

    public VirtualFilterStrategy(boolean virtual) {
        this.virtual = virtual;
    }

    @Override
    public List<StudyGroup> filter(List<StudyGroup> groups) {
        return groups.stream().filter(g -> g.isVirtual() == virtual).collect(Collectors.toList());
    }
}
