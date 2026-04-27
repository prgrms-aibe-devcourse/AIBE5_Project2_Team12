package com.generic4.itda.service.recommend.scoring;

import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoreBreakdown;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class HeuristicV1RecommendationScorer {

    private static final double SKILL_ADJUSTMENT_CONTRIBUTION_WEIGHT = 0.35;
    private static final double CAREER_ADJUSTMENT_CONTRIBUTION_WEIGHT = 0.25;

    private final AiEmbeddingProperties properties;
    private final RecommendationQueryTextGenerator recommendationQueryTextGenerator;
    private final QueryEmbeddingGenerator queryEmbeddingGenerator;
    private final ResumeEmbeddingReader resumeEmbeddingReader;
    private final CosineSimilarityCalculator cosineSimilarityCalculator;
    private final SkillAdjustmentCalculator skillAdjustmentCalculator;
    private final CareerAdjustmentCalculator careerAdjustmentCalculator;

    public List<ScoredCandidate> score(
            Proposal proposal,
            ProposalPosition proposalPosition,
            Set<String> requiredSkillNames,
            Set<String> preferredSkillNames,
            List<RecommendationScorableCandidate> candidates
    ) {
        return score(
                proposal,
                proposalPosition,
                requiredSkillNames,
                preferredSkillNames,
                candidates,
                properties.resolveEmbeddingModel()
        );
    }

    public List<ScoredCandidate> score(
            Proposal proposal,
            ProposalPosition proposalPosition,
            Set<String> requiredSkillNames,
            Set<String> preferredSkillNames,
            List<RecommendationScorableCandidate> candidates,
            String embeddingModel
    ) {
        Assert.notNull(proposal, "proposal은 필수입니다.");
        Assert.notNull(proposalPosition, "proposalPosition은 필수입니다.");
        Assert.notNull(candidates, "candidates는 null일 수 없습니다.");

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        String queryText = recommendationQueryTextGenerator.generate(
                proposal,
                proposalPosition,
                requiredSkillNames,
                preferredSkillNames
        );

        List<Double> queryEmbedding = queryEmbeddingGenerator.generate(queryText);

        List<Long> resumeIds = candidates.stream()
                .map(RecommendationScorableCandidate::resumeId)
                .toList();

        Map<Long, List<Double>> resumeEmbeddings =
                resumeEmbeddingReader.readEmbeddingsByResumeIds(resumeIds, embeddingModel);

        return candidates.stream()
                .filter(candidate -> resumeEmbeddings.containsKey(candidate.resumeId()))
                .map(candidate -> scoredCandidate(
                        candidate,
                        proposalPosition,
                        requiredSkillNames,
                        preferredSkillNames,
                        queryEmbedding,
                        resumeEmbeddings.get(candidate.resumeId())
                ))
                .sorted(Comparator.comparingDouble(
                        (ScoredCandidate scoredCandidate) -> scoredCandidate.scoreBreakdown().finalScore()
                ).reversed())
                .toList();
    }

    private ScoredCandidate scoredCandidate(
            RecommendationScorableCandidate candidate,
            ProposalPosition proposalPosition,
            Set<String> requiredSkillNames,
            Set<String> preferredSkillNames,
            List<Double> queryEmbedding,
            List<Double> resumeEmbedding
    ) {
        double rawCosineSimilarity = cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding);
        double embeddingScore = normalizeCosineSimilarity(rawCosineSimilarity);

        double skillAdjustmentScore = skillAdjustmentCalculator.calculate(
                requiredSkillNames,
                preferredSkillNames,
                candidate.ownedSkillNames()
        );

        double careerAdjustmentScore = careerAdjustmentCalculator.calculate(
                candidate.careerYears(),
                proposalPosition.getCareerMinYears(),
                proposalPosition.getCareerMaxYears()
        );

        double finalScore = clampScore(
                embeddingScore
                        + (skillAdjustmentScore * SKILL_ADJUSTMENT_CONTRIBUTION_WEIGHT)
                        + (careerAdjustmentScore * CAREER_ADJUSTMENT_CONTRIBUTION_WEIGHT)
        );

        return new ScoredCandidate(
                candidate,
                new ScoreBreakdown(
                        embeddingScore,
                        skillAdjustmentScore,
                        careerAdjustmentScore,
                        finalScore
                )
        );
    }

    private double normalizeCosineSimilarity(double rawCosineSimilarity) {
        return (rawCosineSimilarity + 1) / 2.0;
    }

    private double clampScore(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }
}
