package com.generic4.itda.service.recommend.reason;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.mysema.commons.lang.Assert;
import java.math.BigDecimal;

public record RecommendationReasonContext(
        String proposalTitle,
        String positionName,
        ReasonFacts reasonFacts,
        BigDecimal finalScore
) {

    public static RecommendationReasonContext from(RecommendationResult result) {
        Assert.notNull(result, "추천 결과는 필수입니다.");

        RecommendationRun run = result.getRecommendationRun();
        ProposalPosition proposalPosition = run.getProposalPosition();

        return new RecommendationReasonContext(
                proposalPosition.getProposal().getTitle(),
                resolvePositionTitle(proposalPosition),
                result.getReasonFacts(),
                result.getFinalScore()
        );
    }

    private static String resolvePositionTitle(ProposalPosition proposalPosition) {
        if (proposalPosition.getTitle() != null && !proposalPosition.getTitle().isBlank()) {
            return proposalPosition.getTitle();
        }
        return proposalPosition.getPosition().getName();
    }
}
