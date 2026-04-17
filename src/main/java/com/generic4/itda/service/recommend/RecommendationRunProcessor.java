package com.generic4.itda.service.recommend;

import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.repository.RecommendationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationRunProcessor {

    private final RecommendationRunRepository recommendationRunRepository;

    public void process(Long runId) {
        RecommendationRun run = recommendationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 추천 실행입니다."));

        if (!run.isRunning()) {
            throw new IllegalStateException("RUNNING 상태의 추천 실행만 처리할 수 있습니다.");
        }

        try {
            doProcess(run);
        } catch (Exception e) {
            run.markFailed(resolveErrorMessage(e));
        }
    }

    void doProcess(RecommendationRun run) {
        // TODO 실제 추천 계산 / 결과 저장 / 완료 처리
    }

    private String resolveErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "추천 실행 중 오류가 발생했습니다.";
        }
        return message;
    }
}
