package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalWorkType;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiBriefPositionResult {

    private final Long proposalPositionId;
    private final String positionCategoryName;
    private final String title;
    private final ProposalWorkType workType;
    private final Long headCount;
    private final Long unitBudgetMin;
    private final Long unitBudgetMax;
    private final Long expectedPeriod;
    private final Integer careerMinYears;
    private final Integer careerMaxYears;
    private final String workPlace;
    private final List<AiBriefSkillResult> skills;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefPositionResult(Long proposalPositionId, String positionCategoryName, String title,
                                  ProposalWorkType workType, Long headCount, Long unitBudgetMin, Long unitBudgetMax, Long expectedPeriod,
                                  Integer careerMinYears, Integer careerMaxYears, String workPlace, List<AiBriefSkillResult> skills) {
        Assert.hasText(positionCategoryName, "AI 브리프 포지션 카테고리는 필수값입니다.");
        Assert.hasText(title, "AI 브리프 포지션 제목은 필수값입니다.");
        this.proposalPositionId = proposalPositionId;
        this.positionCategoryName = positionCategoryName.trim();
        this.title = title.trim();
        this.workType = workType;
        this.headCount = headCount;
        this.unitBudgetMin = unitBudgetMin;
        this.unitBudgetMax = unitBudgetMax;
        this.expectedPeriod = expectedPeriod;
        this.careerMinYears = careerMinYears;
        this.careerMaxYears = careerMaxYears;
        this.workPlace = workPlace;
        this.skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public static AiBriefPositionResult of(String positionCategoryName, String title, ProposalWorkType workType,
                                           Long headCount, Long unitBudgetMin, Long unitBudgetMax, Long expectedPeriod, Integer careerMinYears,
                                           Integer careerMaxYears, String workPlace, List<AiBriefSkillResult> skills) {
        return of(
                null,
                positionCategoryName,
                title,
                workType,
                headCount,
                unitBudgetMin,
                unitBudgetMax,
                expectedPeriod,
                careerMinYears,
                careerMaxYears,
                workPlace,
                skills
        );
    }

    public static AiBriefPositionResult of(Long proposalPositionId, String positionCategoryName, String title,
                                           ProposalWorkType workType, Long headCount, Long unitBudgetMin, Long unitBudgetMax, Long expectedPeriod,
                                           Integer careerMinYears, Integer careerMaxYears, String workPlace, List<AiBriefSkillResult> skills) {
        return AiBriefPositionResult.builder()
                .proposalPositionId(proposalPositionId)
                .positionCategoryName(positionCategoryName)
                .title(title)
                .workType(workType)
                .headCount(headCount)
                .unitBudgetMin(unitBudgetMin)
                .unitBudgetMax(unitBudgetMax)
                .expectedPeriod(expectedPeriod)
                .careerMinYears(careerMinYears)
                .careerMaxYears(careerMaxYears)
                .workPlace(workPlace)
                .skills(skills)
                .build();
    }
}