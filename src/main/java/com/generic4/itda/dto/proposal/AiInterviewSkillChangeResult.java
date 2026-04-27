package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.Assert;

@Getter
public class AiInterviewSkillChangeResult {

    private final AiInterviewSkillChangeAction action;
    private final String skillName;
    private final ProposalPositionSkillImportance importance;

    @Builder(access = AccessLevel.PRIVATE)
    private AiInterviewSkillChangeResult(
            AiInterviewSkillChangeAction action,
            String skillName,
            ProposalPositionSkillImportance importance
    ) {
        Assert.notNull(action, "AI 인터뷰 스킬 변경 action은 필수값입니다.");
        Assert.hasText(skillName, "AI 인터뷰 스킬 변경 스킬명은 필수값입니다.");
        this.action = action;
        this.skillName = skillName.trim();
        this.importance = importance;
    }

    public static AiInterviewSkillChangeResult of(
            AiInterviewSkillChangeAction action,
            String skillName,
            ProposalPositionSkillImportance importance
    ) {
        return AiInterviewSkillChangeResult.builder()
                .action(action)
                .skillName(skillName)
                .importance(importance)
                .build();
    }
}