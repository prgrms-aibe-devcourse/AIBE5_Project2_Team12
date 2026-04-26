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
import java.util.Locale;
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
        mergePositions(proposal, aiBriefResult.getPositions(), PositionMergeMode.AI_BRIEF, DeletedPositionKeys.empty());
    }

    public void applyForInterview(Proposal proposal, AiBriefResult aiBriefResult, String userMessage) {
        Assert.notNull(proposal, "제안서는 필수값입니다.");
        Assert.notNull(aiBriefResult, "AI 브리프 결과는 필수값입니다.");

        DeletedPositionKeys deletedPositionKeys = removeExplicitlyDeletedPositions(proposal, userMessage);
        applyProposalFields(proposal, aiBriefResult);
        mergePositions(proposal, aiBriefResult.getPositions(), PositionMergeMode.AI_INTERVIEW, deletedPositionKeys);
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
            PositionMergeMode mergeMode,
            DeletedPositionKeys ignoredPositionKeys
    ) {
        if (positionResults == null || positionResults.isEmpty()) {
            return;
        }

        List<PositionApplication> applications = mergePositionApplicationsByPositionKey(positionResults);
        if (applications.isEmpty()) {
            return;
        }

        Map<String, ProposalPosition> existingByPositionKey = existingPositionsByPositionKey(proposal);
        Map<String, List<ProposalPosition>> existingByCategoryKey = existingPositionsByCategoryKey(proposal);
        Map<String, Long> applicationCountByCategoryKey = applicationCountByCategoryKey(applications);
        Set<ProposalPosition> appliedPositions = new HashSet<>();

        for (PositionApplication application : applications) {
            String positionKey = positionKey(application.position(), application.result().getTitle());
            String categoryKey = categoryKey(application.position());
            if (ignoredPositionKeys.positionKeys().contains(positionKey)
                    || ignoredPositionKeys.categoryKeys().contains(categoryKey)) {
                continue;
            }

            ProposalPosition proposalPosition = findExistingPosition(
                    application,
                    existingByPositionKey,
                    existingByCategoryKey,
                    applicationCountByCategoryKey
            );

            if (proposalPosition == null) {
                proposalPosition = addNewPosition(proposal, application);
                replaceSkills(proposalPosition, application.result().getSkills());
            } else if (mergeMode == PositionMergeMode.AI_INTERVIEW) {
                updateExistingPositionForInterview(proposalPosition, application);
                mergeSkillsForInterview(proposalPosition, application.result().getSkills());
            } else {
                updateExistingPositionForAiBrief(proposalPosition, application);
                replaceSkills(proposalPosition, application.result().getSkills());
            }

            appliedPositions.add(proposalPosition);
        }

        if (mergeMode == PositionMergeMode.AI_BRIEF) {
            removePositionsNotInAiResult(proposal, appliedPositions);
        }
    }

    private ProposalPosition addNewPosition(Proposal proposal, PositionApplication application) {
        AiBriefPositionResult result = application.result();
        ProposalWorkType workType = resolveWorkType(result);
        String workPlace = resolveWorkPlace(workType, result.getWorkPlace());

        return proposal.addPosition(
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

    private void updateExistingPositionForAiBrief(ProposalPosition proposalPosition, PositionApplication application) {
        AiBriefPositionResult result = application.result();
        ProposalWorkType workType = resolveWorkType(result);
        String workPlace = resolveWorkPlace(workType, result.getWorkPlace());

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

    private void updateExistingPositionForInterview(ProposalPosition proposalPosition, PositionApplication application) {
        AiBriefPositionResult result = application.result();
        ProposalWorkType workType = resolveInterviewWorkType(proposalPosition, result);
        String workPlace = resolveInterviewWorkPlace(proposalPosition, result, workType);

        proposalPosition.update(
                application.position(),
                resolveInterviewTitle(proposalPosition, result),
                workType,
                resolveInterviewHeadCount(proposalPosition, result),
                resolveInterviewUnitBudgetMin(proposalPosition, result),
                resolveInterviewUnitBudgetMax(proposalPosition, result),
                resolveInterviewExpectedPeriod(proposalPosition, result),
                resolveInterviewCareerMinYears(proposalPosition, result),
                resolveInterviewCareerMaxYears(proposalPosition, result),
                workPlace
        );
    }

    private String resolveInterviewTitle(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (StringUtils.hasText(result.getTitle())) {
            return result.getTitle();
        }
        return proposalPosition.getTitle();
    }

    private ProposalWorkType resolveInterviewWorkType(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getWorkType() != null) {
            return result.getWorkType();
        }
        return proposalPosition.getWorkType();
    }

    private String resolveInterviewWorkPlace(
            ProposalPosition proposalPosition,
            AiBriefPositionResult result,
            ProposalWorkType resolvedWorkType
    ) {
        if (resolvedWorkType == ProposalWorkType.REMOTE) {
            return null;
        }

        if (StringUtils.hasText(result.getWorkPlace())) {
            return result.getWorkPlace().trim();
        }

        if (StringUtils.hasText(proposalPosition.getWorkPlace())) {
            return proposalPosition.getWorkPlace();
        }

        if (result.getWorkType() == ProposalWorkType.SITE || result.getWorkType() == ProposalWorkType.HYBRID) {
            return DEFAULT_WORK_PLACE;
        }

        return null;
    }

    private Long resolveInterviewHeadCount(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getHeadCount() != null && result.getHeadCount() >= 1) {
            return result.getHeadCount();
        }
        return proposalPosition.getHeadCount();
    }

    private Long resolveInterviewUnitBudgetMin(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getUnitBudgetMin() != null) {
            return result.getUnitBudgetMin();
        }
        return proposalPosition.getUnitBudgetMin();
    }

    private Long resolveInterviewUnitBudgetMax(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getUnitBudgetMax() != null) {
            return result.getUnitBudgetMax();
        }
        return proposalPosition.getUnitBudgetMax();
    }

    private Long resolveInterviewExpectedPeriod(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getExpectedPeriod() != null) {
            return result.getExpectedPeriod();
        }
        return proposalPosition.getExpectedPeriod();
    }

    private Integer resolveInterviewCareerMinYears(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getCareerMinYears() != null) {
            return result.getCareerMinYears();
        }
        return proposalPosition.getCareerMinYears();
    }

    private Integer resolveInterviewCareerMaxYears(ProposalPosition proposalPosition, AiBriefPositionResult result) {
        if (result.getCareerMaxYears() != null) {
            return result.getCareerMaxYears();
        }
        return proposalPosition.getCareerMaxYears();
    }

    private DeletedPositionKeys removeExplicitlyDeletedPositions(Proposal proposal, String userMessage) {
        Set<String> deletedPositionKeys = new HashSet<>();
        Set<String> deletedCategoryKeys = new HashSet<>();
        if (!StringUtils.hasText(userMessage)) {
            return new DeletedPositionKeys(deletedPositionKeys, deletedCategoryKeys);
        }

        List<String> messageParts = splitMessageParts(userMessage);
        List<ProposalPosition> existingPositions = new ArrayList<>(proposal.getPositions());
        Map<String, List<ProposalPosition>> existingByCategoryKey = existingPositionsByCategoryKey(proposal);

        for (ProposalPosition existingPosition : existingPositions) {
            DeleteDecision deleteDecision = decideDeletion(existingPosition, existingByCategoryKey, messageParts);
            if (deleteDecision.ignoreCategory()) {
                deletedCategoryKeys.add(categoryKey(existingPosition));
            }
            if (deleteDecision.deletePosition()) {
                deletedPositionKeys.add(positionKey(existingPosition));
                proposal.removePosition(existingPosition);
            }
        }

        return new DeletedPositionKeys(deletedPositionKeys, deletedCategoryKeys);
    }

    private DeleteDecision decideDeletion(
            ProposalPosition existingPosition,
            Map<String, List<ProposalPosition>> existingByCategoryKey,
            List<String> messageParts
    ) {
        String categoryKey = categoryKey(existingPosition);
        String positionName = existingPosition.getPosition() == null ? "" : existingPosition.getPosition().getName();
        String title = existingPosition.getTitle();
        int sameCategoryCount = existingByCategoryKey.getOrDefault(categoryKey, List.of()).size();

        for (String messagePart : messageParts) {
            if (!hasDeleteIntent(messagePart)) {
                continue;
            }

            String normalizedMessagePart = normalizeForContains(messagePart);
            boolean titleMatched = containsNormalized(normalizedMessagePart, title);
            boolean positionNameMatched = containsNormalized(normalizedMessagePart, positionName);

            if (titleMatched) {
                return new DeleteDecision(true, false);
            }

            if (positionNameMatched) {
                if (sameCategoryCount == 1) {
                    return new DeleteDecision(true, true);
                }
                return new DeleteDecision(false, true);
            }
        }

        return new DeleteDecision(false, false);
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
        return value.toLowerCase(Locale.ROOT)
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

    private Map<String, ProposalPosition> existingPositionsByPositionKey(Proposal proposal) {
        Map<String, ProposalPosition> existingPositions = new LinkedHashMap<>();
        for (ProposalPosition proposalPosition : proposal.getPositions()) {
            if (proposalPosition.getPosition() != null && StringUtils.hasText(proposalPosition.getPosition().getName())) {
                existingPositions.put(positionKey(proposalPosition), proposalPosition);
            }
        }
        return existingPositions;
    }

    private Map<String, List<ProposalPosition>> existingPositionsByCategoryKey(Proposal proposal) {
        Map<String, List<ProposalPosition>> existingPositions = new LinkedHashMap<>();
        for (ProposalPosition proposalPosition : proposal.getPositions()) {
            if (proposalPosition.getPosition() == null || !StringUtils.hasText(proposalPosition.getPosition().getName())) {
                continue;
            }

            String categoryKey = normalizeKey(proposalPosition.getPosition().getName());
            existingPositions.computeIfAbsent(categoryKey, ignored -> new ArrayList<>()).add(proposalPosition);
        }
        return existingPositions;
    }

    private List<PositionApplication> mergePositionApplicationsByPositionKey(List<AiBriefPositionResult> positionResults) {
        Map<String, PositionApplication> merged = new LinkedHashMap<>();

        for (AiBriefPositionResult positionResult : positionResults) {
            Optional<Position> resolvedPosition = positionResolver.resolve(positionResult.getPositionCategoryName());
            if (resolvedPosition.isEmpty()) {
                continue;
            }

            Position position = resolvedPosition.get();
            merged.put(positionKey(position, positionResult.getTitle()), new PositionApplication(position, positionResult));
        }

        return new ArrayList<>(merged.values());
    }

    private Map<String, Long> applicationCountByCategoryKey(List<PositionApplication> applications) {
        Map<String, Long> countByCategory = new LinkedHashMap<>();

        for (PositionApplication application : applications) {
            String categoryKey = normalizeKey(application.position().getName());
            countByCategory.put(categoryKey, countByCategory.getOrDefault(categoryKey, 0L) + 1L);
        }

        return countByCategory;
    }

    private ProposalPosition findExistingPosition(
            PositionApplication application,
            Map<String, ProposalPosition> existingByPositionKey,
            Map<String, List<ProposalPosition>> existingByCategoryKey,
            Map<String, Long> applicationCountByCategoryKey
    ) {
        String positionKey = positionKey(application.position(), application.result().getTitle());
        ProposalPosition exactMatch = existingByPositionKey.get(positionKey);
        if (exactMatch != null) {
            return exactMatch;
        }

        String categoryKey = normalizeKey(application.position().getName());
        List<ProposalPosition> categoryMatches = existingByCategoryKey.getOrDefault(categoryKey, List.of());
        Long categoryApplicationCount = applicationCountByCategoryKey.getOrDefault(categoryKey, 0L);

        if (categoryMatches.size() == 1 && categoryApplicationCount == 1L) {
            return categoryMatches.get(0);
        }

        return null;
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

    private void mergeSkillsForInterview(ProposalPosition proposalPosition, List<AiBriefSkillResult> skills) {
        Map<String, SkillApplication> desiredSkills = buildDesiredSkills(skills);
        if (desiredSkills.isEmpty()) {
            return;
        }

        for (ProposalPositionSkill existingSkill : proposalPosition.getSkills()) {
            String key = normalizeKey(existingSkill.getSkill().getName());
            SkillApplication desiredSkill = desiredSkills.remove(key);

            if (desiredSkill != null) {
                existingSkill.changeImportance(desiredSkill.importance());
            }
        }

        for (SkillApplication desiredSkill : desiredSkills.values()) {
            proposalPosition.addSkill(desiredSkill.skill(), desiredSkill.importance());
        }
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

    private String positionKey(ProposalPosition proposalPosition) {
        if (proposalPosition == null || proposalPosition.getPosition() == null) {
            return "";
        }
        return positionKey(proposalPosition.getPosition(), proposalPosition.getTitle());
    }

    private String positionKey(Position position, String title) {
        return categoryKey(position) + "::" + normalizeKey(title);
    }

    private String categoryKey(ProposalPosition proposalPosition) {
        if (proposalPosition == null || proposalPosition.getPosition() == null) {
            return "";
        }
        return categoryKey(proposalPosition.getPosition());
    }

    private String categoryKey(Position position) {
        return position == null ? "" : normalizeKey(position.getName());
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .trim();
    }

    private enum PositionMergeMode {
        AI_BRIEF,
        AI_INTERVIEW
    }

    private record PositionApplication(Position position, AiBriefPositionResult result) {
    }

    private record SkillApplication(Skill skill, ProposalPositionSkillImportance importance) {
    }

    private record DeletedPositionKeys(Set<String> positionKeys, Set<String> categoryKeys) {

        private static DeletedPositionKeys empty() {
            return new DeletedPositionKeys(Set.of(), Set.of());
        }
    }

    private record DeleteDecision(boolean deletePosition, boolean ignoreCategory) {
    }
}