package com.generic4.itda.service.recommend;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationRunPollingSchedulerTest {

    @Mock
    private RecommendationRunExecutor recommendationRunExecutor;

    @Mock
    private RecommendationRunProcessor recommendationRunProcessor;

    @InjectMocks
    private RecommendationRunPollingScheduler recommendationRunPollingScheduler;

    @Nested
    @DisplayName("poll")
    class Poll {

        @Test
        @DisplayName("점유 가능한 run이 없으면 processor를 호출하지 않는다")
        void 점유_가능한_run이_없으면_processor를_호출하지_않는다() {
            given(recommendationRunExecutor.claimNextPendingRun())
                    .willReturn(Optional.empty());

            recommendationRunPollingScheduler.poll();

            then(recommendationRunExecutor).should().claimNextPendingRun();
            then(recommendationRunProcessor).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("점유한 run이 있으면 processor를 호출한다")
        void 점유한_run이_있으면_processor를_호출한다() {
            given(recommendationRunExecutor.claimNextPendingRun())
                    .willReturn(Optional.of(1L));

            recommendationRunPollingScheduler.poll();

            then(recommendationRunExecutor).should().claimNextPendingRun();
            then(recommendationRunProcessor).should().process(1L);
        }
    }
}