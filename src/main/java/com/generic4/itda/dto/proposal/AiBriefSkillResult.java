package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiBriefSkillResult {

    private final String skillName;
    private final ProposalPositionSkillImportance importance;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefSkillResult(String skillName, ProposalPositionSkillImportance importance) {
        Assert.hasText(skillName, "AI 브리프 스킬명은 필수값입니다.");
        this.skillName = skillName.trim();
        this.importance = importance;
    }

    public static AiBriefSkillResult of(String skillName, ProposalPositionSkillImportance importance) {
        return AiBriefSkillResult.builder()
                .skillName(skillName)
                .importance(importance)
                .build();
    }
}
