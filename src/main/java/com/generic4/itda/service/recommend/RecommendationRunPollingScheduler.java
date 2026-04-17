package com.generic4.itda.service.recommend;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationRunPollingScheduler {

    private final RecommendationRunExecutor recommendationRunExecutor;
    private final RecommendationRunProcessor recommendationRunProcessor;

    @Scheduled(fixedDelay = 3000)
    public void poll() {
        recommendationRunExecutor.claimNextPendingRun()
                .ifPresent(recommendationRunProcessor::process);
    }
}
