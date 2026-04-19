package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkill;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RecommendationFingerprintGenerator {

    public String generate(
            ProposalPosition proposalPosition,
            RecommendationAlgorithm algorithm,
            int topK
    ) {
        String skillPart = proposalPosition.getSkills().stream()
                .sorted(Comparator.comparing((ProposalPositionSkill skill) -> skill.getSkill().getName())
                        .thenComparing(skill -> skill.getImportance().name()))
                .map(this::toSkillToken)
                .collect(Collectors.joining("|"));

        String raw = String.join("::",
                "proposalPosition.id=" + number(proposalPosition.getId()),
                "proposalPosition.positionId=" + number(proposalPosition.getPosition().getId()),
                "proposalPosition.workType=" + enumName(proposalPosition.getWorkType()),
                "proposalPosition.careerMinYears=" + number(proposalPosition.getCareerMinYears()),
                "proposalPosition.careerMaxYears=" + number(proposalPosition.getCareerMaxYears()),

                "proposalPosition.unitBudgetMin=" + number(proposalPosition.getUnitBudgetMin()),
                "proposalPosition.unitBudgetMax=" + number(proposalPosition.getUnitBudgetMax()),

                "proposalPosition.skills=" + skillPart,

                "recommendation.algorithm=" + algorithm.name(),
                "recommendation.topK=" + topK
        );

        return sha256(raw);
    }

    private String toSkillToken(ProposalPositionSkill skill) {
        return (skill.getSkill().getId() != null ? skill.getSkill().getId() : skill.getSkill().getName())
                + ":" + skill.getImportance().name();
    }

    private String number(Number value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("request fingerprint 생성에 실패했습니다.", e);
        }
    }
}
