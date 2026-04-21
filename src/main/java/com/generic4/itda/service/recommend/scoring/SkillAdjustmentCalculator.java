package com.generic4.itda.service.recommend.scoring;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SkillAdjustmentCalculator {

    private static final double REQUIRED_SKILL_NEUTRAL_MATCH_RATIO = 0.5;
    private static final double REQUIRED_SKILL_ADJUSTMENT_WEIGHT = 0.3;
    private static final double REQUIRED_MAX_BONUS = 0.15;
    private static final double REQUIRED_MIN_PENALTY = -0.15;
    private static final double PREFERRED_MAX_BONUS = 0.05;

    public double calculate(
            Set<String> requiredSkillNames,
            Set<String> preferredSkillNames,
            Set<String> ownedSkillNames
    ) {
        Set<String> required = safeSet(requiredSkillNames);
        Set<String> preferred = normalizePreferredSkills(required, preferredSkillNames);
        Set<String> owned = safeSet(ownedSkillNames);

        double requiredAdjustment = calculateRequiredAdjustment(required, owned);
        double preferredAdjustment = calculatePreferredAdjustment(preferred, owned);

        return requiredAdjustment + preferredAdjustment;
    }

    private double calculateRequiredAdjustment(Set<String> requiredSkillNames, Set<String> ownedSkillNames) {
        if (requiredSkillNames.isEmpty()) {
            return 0.0;
        }

        long matchedCount = requiredSkillNames.stream()
                .filter(ownedSkillNames::contains)
                .count();

        double matchRatio = (double) matchedCount / requiredSkillNames.size();
        double adjustment = (matchRatio - REQUIRED_SKILL_NEUTRAL_MATCH_RATIO) * REQUIRED_SKILL_ADJUSTMENT_WEIGHT;

        if (adjustment > REQUIRED_MAX_BONUS) {
            return REQUIRED_MAX_BONUS;
        }
        if (adjustment < REQUIRED_MIN_PENALTY) {
            return REQUIRED_MIN_PENALTY;
        }
        return adjustment;
    }

    private double calculatePreferredAdjustment(Set<String> preferredSkillNames, Set<String> ownedSkillNames) {
        if (preferredSkillNames.isEmpty()) {
            return 0.0;
        }

        long matchCount = preferredSkillNames.stream()
                .filter(ownedSkillNames::contains)
                .count();

        double matchRatio = (double) matchCount / preferredSkillNames.size();
        return matchRatio * PREFERRED_MAX_BONUS;
    }

    private Set<String> safeSet(Set<String> skills) {
        return skills == null ? Collections.emptySet() : skills;
    }

    private Set<String> normalizePreferredSkills(Set<String> requiredSkillNames, Set<String> preferredSkillNames) {
        Set<String> preferred = safeSet(preferredSkillNames);

        return preferred.stream()
                .filter(skillName -> !requiredSkillNames.contains(skillName))
                .collect(Collectors.toUnmodifiableSet());
    }
}
