package com.generic4.itda.service.recommend.reason;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationReasonProcessor {

    private final RecommendationReasonGenerator recommendationReasonGenerator;

    public void process(List<RecommendationResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (RecommendationResult result : results) {
            if (result.getLlmStatus() != LlmStatus.PENDING) {
                continue;
            }

            try {
                RecommendationReasonContext context = RecommendationReasonContext.from(result);
                String llmReason = recommendationReasonGenerator.generate(context);

                if (!StringUtils.hasText(llmReason)) {
                    result.markLlmFailed();
                    log.warn("추천 이유 생성 결과가 비어있습니다. resultId={}", result.getId());
                    continue;
                }

                result.markLlmReady(llmReason);
            } catch (Exception e) {
                result.markLlmFailed();
                log.warn("추천 이유 생성에 실패했습니다. resultId={}", result.getId(), e);
            }
        }
    }
}
