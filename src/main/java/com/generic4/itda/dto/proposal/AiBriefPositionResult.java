package com.generic4.itda.dto.proposal;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiBriefPositionResult {

    private final String positionName;
    private final Long headCount;
    private final Long unitBudgetMin;
    private final Long unitBudgetMax;
    private final List<AiBriefSkillResult> skills;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefPositionResult(String positionName, Long headCount, Long unitBudgetMin, Long unitBudgetMax,
            List<AiBriefSkillResult> skills) {
        Assert.hasText(positionName, "AI 브리프 직무명은 필수값입니다.");
        this.positionName = positionName.trim();
        this.headCount = headCount;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
        this.skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public static AiBriefPositionResult of(String positionName, Long headCount, Long unitBudgetMin, Long unitBudgetMax,
            List<AiBriefSkillResult> skills) {
        return AiBriefPositionResult.builder()
                .positionName(positionName)
                .headCount(headCount)
                .unitBudgetMin(unitBudgetMin)
                .unitBudgetMax(unitBudgetMax)
                .skills(skills)
                .build();
    }
}
