package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RecommendationRunExecutorTest {

    private static final PageRequest NEXT_PENDING_RUN = PageRequest.of(0, 10);

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @InjectMocks
    private RecommendationRunExecutor recommendationRunExecutor;

    @Test
    @DisplayName("대기 중인 run이 없으면 빈 값을 반환한다")
    void 대기_중인_run이_없으면_빈_값을_반환한다() {
        given(recommendationRunRepository.findPendingRunIds(NEXT_PENDING_RUN))
                .willReturn(List.of());

        Optional<Long> result = recommendationRunExecutor.claimNextPendingRun();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("대기 중인 run 점유에 성공하면 runId를 반환한다")
    void 대기_중인_run_점유에_성공하면_runId를_반환한다() {
        given(recommendationRunRepository.findPendingRunIds(NEXT_PENDING_RUN))
                .willReturn(List.of(1L));
        given(recommendationRunRepository.claimAsRunning(1L))
                .willReturn(1);

        Optional<Long> result = recommendationRunExecutor.claimNextPendingRun();

        assertThat(result).contains(1L);
    }

    @Test
    @DisplayName("대기 중인 run이 있어도 이미 다른 실행기가 점유했으면 빈 값을 반환한다")
    void claim_실패하면_빈_값을_반환한다() {
        given(recommendationRunRepository.findPendingRunIds(NEXT_PENDING_RUN))
                .willReturn(List.of(1L));
        given(recommendationRunRepository.claimAsRunning(1L))
                .willReturn(0);

        Optional<Long> result = recommendationRunExecutor.claimNextPendingRun();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("가장 앞의 run 점유에 성공하면 다음 run은 점유 시도하지 않는다")
    void 가장_앞의_run_점유에_성공하면_다음_run은_점유_시도하지_않는다() {
        given(recommendationRunRepository.findPendingRunIds(NEXT_PENDING_RUN))
                .willReturn(List.of(1L, 2L));

        given(recommendationRunRepository.claimAsRunning(1L))
                .willReturn(1);

        Optional<Long> result = recommendationRunExecutor.claimNextPendingRun();

        assertThat(result).contains(1L);
        then(recommendationRunRepository).should().claimAsRunning(1L);
        then(recommendationRunRepository).should(never()).claimAsRunning(2L);
    }
}
