package com.generic4.itda.service;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.SkillRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiBriefProposalMapper {

    private final PositionRepository positionRepository;
    private final SkillRepository skillRepository;

    public void apply(Proposal proposal, AiBriefResult aiBriefResult) {
        Assert.notNull(proposal, "제안서는 필수값입니다.");
        Assert.notNull(aiBriefResult, "AI 브리프 결과는 필수값입니다.");

        proposal.update(
                resolveTitle(proposal, aiBriefResult),
                proposal.getRawInputText(),
                resolveDescription(proposal, aiBriefResult),
                resolveTotalBudgetMin(proposal, aiBriefResult),
                resolveTotalBudgetMax(proposal, aiBriefResult),
                resolveExpectedPeriod(proposal, aiBriefResult)
        );

        replacePositions(proposal, aiBriefResult.getPositions());
    }

    private void replacePositions(Proposal proposal, List<AiBriefPositionResult> positions) {
        if (positions == null || positions.isEmpty()) {
            return;
        }

        List<ProposalPosition> existingPositions = new ArrayList<>(proposal.getPositions());
        for (ProposalPosition existingPosition : existingPositions) {
            proposal.removePosition(existingPosition);
        }

        for (AiBriefPositionResult positionResult : positions) {
            Position position = findOrCreatePosition(positionResult.getPositionName());
            ProposalPosition proposalPosition = proposal.addPosition(
                    position,
                    positionResult.getPositionName(),
                    null,
                    positionResult.getHeadCount(),
                    positionResult.getUnitBudgetMin(),
                    positionResult.getUnitBudgetMax(),
                    null,
                    null,
                    null,
                    null
            );
            addSkills(proposalPosition, positionResult.getSkills());
        }
    }

    private void addSkills(ProposalPosition proposalPosition, List<AiBriefSkillResult> skills) {
        for (AiBriefSkillResult skillResult : skills) {
            Skill skill = findOrCreateSkill(skillResult.getSkillName());
            proposalPosition.addSkill(skill, skillResult.getImportance());
        }
    }

    private Position findOrCreatePosition(String positionName) {
        return positionRepository.findByName(positionName)
                .orElseGet(() -> positionRepository.save(Position.create(positionName)));
    }

    private Skill findOrCreateSkill(String skillName) {
        return skillRepository.findByName(skillName)
                .orElseGet(() -> skillRepository.save(Skill.create(skillName, null)));
    }

    private String resolveTitle(Proposal proposal, AiBriefResult aiBriefResult) {
        if (StringUtils.hasText(aiBriefResult.getTitle())) {
            return aiBriefResult.getTitle();
        }
        return proposal.getTitle();
    }

    private String resolveDescription(Proposal proposal, AiBriefResult aiBriefResult) {
        if (StringUtils.hasText(aiBriefResult.getDescription())) {
            return aiBriefResult.getDescription();
        }
        return proposal.getDescription();
    }

    private Long resolveTotalBudgetMin(Proposal proposal, AiBriefResult aiBriefResult) {
        if (aiBriefResult.getTotalBudgetMin() != null) {
            return aiBriefResult.getTotalBudgetMin();
        }
        return proposal.getTotalBudgetMin();
    }

    private Long resolveTotalBudgetMax(Proposal proposal, AiBriefResult aiBriefResult) {
        if (aiBriefResult.getTotalBudgetMax() != null) {
            return aiBriefResult.getTotalBudgetMax();
        }
        return proposal.getTotalBudgetMax();
    }

    private Long resolveExpectedPeriod(Proposal proposal, AiBriefResult aiBriefResult) {
        if (aiBriefResult.getExpectedPeriod() != null) {
            return aiBriefResult.getExpectedPeriod();
        }
        return proposal.getExpectedPeriod();
    }
}
