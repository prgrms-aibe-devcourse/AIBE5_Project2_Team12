package com.generic4.itda.service.recommend.reason;

import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import java.math.BigDecimal;

public record RecommendationReasonContext(
        String proposalTitle,
        String positionName,
        ReasonFacts reasonFacts,
        BigDecimal finalScore
) {

}
