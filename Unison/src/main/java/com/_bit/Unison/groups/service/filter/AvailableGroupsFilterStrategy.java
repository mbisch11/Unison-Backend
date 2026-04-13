package com._bit.Unison.groups.service.filter;

import com._bit.Unison.groups.model.StudyGroup;
import com._bit.Unison.groups.repo.GroupMembershipRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AvailableGroupsFilterStrategy implements GroupFilterStrategy {

    private final GroupMembershipRepository membershipRepo;

    public AvailableGroupsFilterStrategy(GroupMembershipRepository membershipRepo) {
        this.membershipRepo = membershipRepo;
    }

    @Override
    public List<StudyGroup> filter(List<StudyGroup> groups) {
        LocalDateTime now = LocalDateTime.now();
        return groups.stream().filter(g -> g.getStartTime() != null && g.getStartTime().isAfter(now)).filter(g -> membershipRepo.countByGroupId(g.getGroupId()) < g.getMaxCapacity()).collect(Collectors.toList());
    }
}
