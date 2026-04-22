package com.generic4.itda.service.recommend.reason;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.recommend-reason", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledRecommendationReasonGenerator implements RecommendationReasonGenerator {

    @Override
    public String generate(RecommendationReasonContext context) {
        throw new UnsupportedOperationException("추천 이유 생성이 비활성화되어있습니다.");
    }
}
