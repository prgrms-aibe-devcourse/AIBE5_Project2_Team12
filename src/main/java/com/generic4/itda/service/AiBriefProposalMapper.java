package com.generic4.itda.service;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkill;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiBriefProposalMapper {

    private static final Long DEFAULT_HEAD_COUNT = 1L;
    private static final ProposalWorkType DEFAULT_WORK_TYPE = ProposalWorkType.REMOTE;
    private static final String DEFAULT_WORK_PLACE = "협의";

    private final PositionResolver positionResolver;
    private final SkillResolver skillResolver;

    public void apply(Proposal proposal, AiBriefResult aiBriefResult) {
        Assert.notNull(proposal, "제안서는 필수값입니다.");
        Assert.notNull(aiBriefResult, "AI 브리프 결과는 필수값입니다.");

        applyProposalFields(proposal, aiBriefResult);
        mergePositions(proposal, aiBriefResult.getPositions(), true, Set.of());
    }

    public void applyForInterview(Proposal proposal, AiBriefResult aiBriefResult, String userMessage) {
        Assert.notNull(proposal, "제안서는 필수값입니다.");
        Assert.notNull(aiBriefResult, "AI 브리프 결과는 필수값입니다.");

        Set<String> deletedPositionKeys = removeExplicitlyDeletedPositions(proposal, userMessage);
        applyProposalFields(proposal, aiBriefResult);
        mergePositions(proposal, aiBriefResult.getPositions(), false, deletedPositionKeys);
    }

    private void applyProposalFields(Proposal proposal, AiBriefResult aiBriefResult) {
        proposal.update(
                resolveTitle(proposal, aiBriefResult),
                proposal.getRawInputText(),
                resolveDescription(proposal, aiBriefResult),
                resolveTotalBudgetMin(proposal, aiBriefResult),
                resolveTotalBudgetMax(proposal, aiBriefResult),
                resolveExpectedPeriod(proposal, aiBriefResult)
        );
    }

    private void mergePositions(
            Proposal proposal,
            List<AiBriefPositionResult> positionResults,
            boolean removePositionsNotInAiResult,
            Set<String> ignoredPositionKeys
    ) {
        if (positionResults == null || positionResults.isEmpty()) {
            return;
        }

        Map<String, ProposalPosition> existingByPositionName = existingPositionsByPositionName(proposal);
        List<PositionApplication> applications = mergePositionApplicationsByPositionName(positionResults);
        if (applications.isEmpty()) {
            return;
        }
        Set<ProposalPosition> appliedPositions = new HashSet<>();

        for (PositionApplication application : applications) {
            String positionKey = normalizeKey(application.position().getName());
            if (ignoredPositionKeys.contains(positionKey)) {
                continue;
            }

            ProposalPosition proposalPosition = existingByPositionName.get(positionKey);
            AiBriefPositionResult result = application.result();
            ProposalWorkType workType = resolveWorkType(result);
            String workPlace = resolveWorkPlace(workType, result.getWorkPlace());

            if (proposalPosition == null) {
                proposalPosition = proposal.addPosition(
                        application.position(),
                        result.getTitle(),
                        workType,
                        resolveHeadCount(result),
                        result.getUnitBudgetMin(),
                        result.getUnitBudgetMax(),
                        result.getExpectedPeriod(),
                        result.getCareerMinYears(),
                        result.getCareerMaxYears(),
                        workPlace
                );
            } else {
                proposalPosition.update(
                        application.position(),
                        result.getTitle(),
                        workType,
                        resolveHeadCount(result),
                        result.getUnitBudgetMin(),
                        result.getUnitBudgetMax(),
                        result.getExpectedPeriod(),
                        result.getCareerMinYears(),
                        result.getCareerMaxYears(),
                        workPlace
                );
            }

            replaceSkills(proposalPosition, result.getSkills());
            appliedPositions.add(proposalPosition);
        }

        if (removePositionsNotInAiResult) {
            removePositionsNotInAiResult(proposal, appliedPositions);
        }
    }

    private Set<String> removeExplicitlyDeletedPositions(Proposal proposal, String userMessage) {
        Set<String> deletedPositionKeys = new HashSet<>();
        if (!StringUtils.hasText(userMessage)) {
            return deletedPositionKeys;
        }

        List<String> messageParts = splitMessageParts(userMessage);
        List<ProposalPosition> existingPositions = new ArrayList<>(proposal.getPositions());

        for (ProposalPosition existingPosition : existingPositions) {
            if (isExplicitlyDeleted(existingPosition, messageParts)) {
                if (existingPosition.getPosition() != null) {
                    deletedPositionKeys.add(normalizeKey(existingPosition.getPosition().getName()));
                }
                proposal.removePosition(existingPosition);
            }
        }

        return deletedPositionKeys;
    }

    private boolean isExplicitlyDeleted(ProposalPosition existingPosition, List<String> messageParts) {
        String positionName = existingPosition.getPosition() == null ? "" : existingPosition.getPosition().getName();
        String title = existingPosition.getTitle();

        for (String messagePart : messageParts) {
            if (!hasDeleteIntent(messagePart)) {
                continue;
            }

            String normalizedMessagePart = normalizeForContains(messagePart);
            boolean positionNameMatched = containsNormalized(normalizedMessagePart, positionName);
            boolean titleMatched = containsNormalized(normalizedMessagePart, title);

            if (positionNameMatched || titleMatched) {
                return true;
            }
        }

        return false;
    }

    private List<String> splitMessageParts(String userMessage) {
        return List.of(userMessage.split("[,，.。!?！？\\n]|그리고|또|및|\\s+하고\\s+|\\s+가고\\s+|\\s+으로\\s+가고\\s+"))
                .stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private boolean hasDeleteIntent(String messagePart) {
        if (!StringUtils.hasText(messagePart)) {
            return false;
        }

        String normalized = normalizeForContains(messagePart);
        return normalized.contains("빼")
                || normalized.contains("제외")
                || normalized.contains("삭제")
                || normalized.contains("제거")
                || normalized.contains("필요없")
                || normalized.contains("안뽑")
                || normalized.contains("안구")
                || normalized.contains("없애");
    }

    private boolean containsNormalized(String normalizedSource, String target) {
        if (!StringUtils.hasText(normalizedSource) || !StringUtils.hasText(target)) {
            return false;
        }
        return normalizedSource.contains(normalizeForContains(target));
    }

    private String normalizeForContains(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("\\s+", "")
                .trim();
    }

    private void removePositionsNotInAiResult(Proposal proposal, Set<ProposalPosition> appliedPositions) {
        List<ProposalPosition> existingPositions = new ArrayList<>(proposal.getPositions());
        for (ProposalPosition existingPosition : existingPositions) {
            if (!appliedPositions.contains(existingPosition)) {
                proposal.removePosition(existingPosition);
            }
        }
    }

    private Map<String, ProposalPosition> existingPositionsByPositionName(Proposal proposal) {
        Map<String, ProposalPosition> existingPositions = new LinkedHashMap<>();
        for (ProposalPosition proposalPosition : proposal.getPositions()) {
            if (proposalPosition.getPosition() != null && StringUtils.hasText(proposalPosition.getPosition().getName())) {
                existingPositions.put(normalizeKey(proposalPosition.getPosition().getName()), proposalPosition);
            }
        }
        return existingPositions;
    }

    private List<PositionApplication> mergePositionApplicationsByPositionName(List<AiBriefPositionResult> positionResults) {
        Map<String, PositionApplication> merged = new LinkedHashMap<>();

        for (AiBriefPositionResult positionResult : positionResults) {
            Optional<Position> resolvedPosition = positionResolver.resolve(positionResult.getPositionCategoryName());
            if (resolvedPosition.isEmpty()) {
                continue;
            }

            Position position = resolvedPosition.get();
            merged.put(normalizeKey(position.getName()), new PositionApplication(position, positionResult));
        }

        return new ArrayList<>(merged.values());
    }

    private Long resolveHeadCount(AiBriefPositionResult result) {
        if (result.getHeadCount() == null || result.getHeadCount() < 1) {
            return DEFAULT_HEAD_COUNT;
        }
        return result.getHeadCount();
    }

    private ProposalWorkType resolveWorkType(AiBriefPositionResult result) {
        if (result.getWorkType() == null) {
            return DEFAULT_WORK_TYPE;
        }
        return result.getWorkType();
    }

    private String resolveWorkPlace(ProposalWorkType workType, String workPlace) {
        if (workType == ProposalWorkType.REMOTE) {
            return null;
        }

        if ((workType == ProposalWorkType.SITE || workType == ProposalWorkType.HYBRID)
                && !StringUtils.hasText(workPlace)) {
            return DEFAULT_WORK_PLACE;
        }

        return StringUtils.hasText(workPlace) ? workPlace.trim() : null;
    }

    private void replaceSkills(ProposalPosition proposalPosition, List<AiBriefSkillResult> skills) {
        Map<String, SkillApplication> desiredSkills = buildDesiredSkills(skills);

        List<ProposalPositionSkill> existingSkills = new ArrayList<>(proposalPosition.getSkills());
        for (ProposalPositionSkill existingSkill : existingSkills) {
            String key = normalizeKey(existingSkill.getSkill().getName());
            SkillApplication desiredSkill = desiredSkills.remove(key);

            if (desiredSkill == null) {
                proposalPosition.removeSkill(existingSkill.getSkill());
                continue;
            }

            existingSkill.changeImportance(desiredSkill.importance());
        }

        for (SkillApplication desiredSkill : desiredSkills.values()) {
            proposalPosition.addSkill(desiredSkill.skill(), desiredSkill.importance());
        }
    }

    private Map<String, SkillApplication> buildDesiredSkills(List<AiBriefSkillResult> skills) {
        Map<String, SkillApplication> desiredSkills = new LinkedHashMap<>();
        if (skills == null || skills.isEmpty()) {
            return desiredSkills;
        }

        for (AiBriefSkillResult skillResult : skills) {
            if (skillResult == null || !StringUtils.hasText(skillResult.getSkillName())) {
                continue;
            }

            Optional<Skill> resolvedSkill = skillResolver.resolve(skillResult.getSkillName());
            if (resolvedSkill.isEmpty()) {
                continue;
            }

            Skill skill = resolvedSkill.get();
            desiredSkills.putIfAbsent(
                    normalizeKey(skill.getName()),
                    new SkillApplication(skill, skillResult.getImportance())
            );
        }

        return desiredSkills;
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

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim();
    }

    private record PositionApplication(Position position, AiBriefPositionResult result) {
    }

    private record SkillApplication(Skill skill, ProposalPositionSkillImportance importance) {
    }
}
