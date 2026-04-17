package com.generic4.itda.service.recommend;

import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendationRunExecutor {

    private static final PageRequest NEXT_PENDING_RUN = PageRequest.of(0, 1);
    private final RecommendationRunRepository recommendationRunRepository;

    public Optional<Long> claimNextPendingRun() {
        List<Long> pendingRunIds = recommendationRunRepository.findPendingRunIds(NEXT_PENDING_RUN);
        if (pendingRunIds.isEmpty()) {
            return Optional.empty();
        }

        Long runId = pendingRunIds.get(0);
        int updated = recommendationRunRepository.claimAsRunning(runId);

        if (updated == 0) {
            return Optional.empty();
        }

        return Optional.of(runId);
    }
}
