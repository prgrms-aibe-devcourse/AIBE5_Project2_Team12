package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.proposal.Proposal;
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
            Proposal proposal,
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
                "proposal.id=" + proposal.getId(),
                "proposal.title=" + nullSafe(proposal.getTitle()),
                "proposal.description=" + nullSafe(proposal.getDescription()),
                "proposal.totalBudgetMin=" + proposal.getTotalBudgetMin(),
                "proposal.totalBudgetMax=" + proposal.getTotalBudgetMax(),
                "proposal.workType=" + enumName(proposal.getWorkType()),
                "proposal.workPlace=" + nullSafe(proposal.getWorkPlace()),
                "proposal.expectedPeriod=" + proposal.getExpectedPeriod(),
                "proposal.status=" + proposal.getStatus().name(),

                "proposalPosition.id=" + proposalPosition.getId(),
                "proposalPosition.positionId=" + proposalPosition.getPosition().getId(),
                "proposalPosition.positionName=" + proposalPosition.getPosition().getName(),
                "proposalPosition.headCount=" + proposalPosition.getHeadCount(),
                "proposalPosition.status=" + proposalPosition.getStatus().name(),
                "proposalPosition.unitBudgetMin=" + proposalPosition.getUnitBudgetMin(),
                "proposalPosition.unitBudgetMax=" + proposalPosition.getUnitBudgetMax(),

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

    private String nullSafe(String value) {
        return value == null ? "" : value;
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
