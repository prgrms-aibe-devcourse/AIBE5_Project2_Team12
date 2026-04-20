package com.generic4.itda.service.recommend.scoring;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 임베딩용 문자열 생성기
 */
@Component
public class RecommendationQueryTextGenerator {

    public String generate(
            Proposal proposal,
            ProposalPosition proposalPosition,
            Set<String> requiredSkillNames,
            Set<String> preferredSkillNames
    ) {
        Set<String> required = safeSet(requiredSkillNames);
        Set<String> preferred = normalizePreferredSkills(required, preferredSkillNames);

        String positionName = extractPositionName(proposalPosition.getPosition());
        String workType = proposalPosition.getWorkType() == null ? "" : proposalPosition.getWorkType().name();
        String careerRange = buildCareerRange(
                proposalPosition.getCareerMinYears(),
                proposalPosition.getCareerMaxYears()
        );

        String requiredSkillsText = joinSkills(required);
        String preferredSkillsText = joinSkills(preferred);

        String title = normalizeText(proposal.getTitle());
        String description = normalizeText(proposal.getDescription());

        return """
                position: %s
                workType: %s
                career: %s
                required skills: %s
                preferred skills: %s
                title: %s
                description: %s
                """.formatted(
                positionName,
                workType,
                careerRange,
                requiredSkillsText,
                preferredSkillsText,
                title,
                description
        ).trim();
    }

    private Set<String> safeSet(Set<String> skills) {
        return skills == null ? Collections.emptySet() : skills;
    }

    private Set<String> normalizePreferredSkills(Set<String> required, Set<String> preferredSkillNames) {
        Set<String> preferred = safeSet(preferredSkillNames);

        return preferred.stream()
                .filter(Predicate.not(required::contains))
                .collect(Collectors.toUnmodifiableSet());
    }

    private String extractPositionName(Position position) {
        if (position == null || position.getName() == null) {
            return "";
        }
        return position.getName();
    }

    private String buildCareerRange(Integer minYears, Integer maxYears) {
        if (minYears == null && maxYears == null) {
            return "";
        }
        if (minYears != null && maxYears != null) {
            return minYears + "~" + maxYears + " years";
        }
        if (minYears != null) {
            return minYears + "+ years";
        }
        return "up to " + maxYears + " years";
    }

    private String joinSkills(Set<String> skills) {
        if (skills == null) {
            return "";
        }

        return skills.stream()
                .map(this::normalizeText)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
