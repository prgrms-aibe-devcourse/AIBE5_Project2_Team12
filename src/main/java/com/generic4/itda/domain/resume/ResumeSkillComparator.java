package com.generic4.itda.domain.resume;

import java.util.Comparator;

public class ResumeSkillComparator implements Comparator<ResumeSkill> {

    @Override
    public int compare(ResumeSkill o1, ResumeSkill o2) {
        // priority 내림차순 (ADVANCED=3 > INTERMEDIATE=2 > BEGINNER=1)
        int priorityCompare = Integer.compare(
                o2.getProficiency().getPriority(),
                o1.getProficiency().getPriority()
        );
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // 동일 priority면 스킬 이름 오름차순
        return o1.getSkill().getName().compareTo(o2.getSkill().getName());
    }
}
