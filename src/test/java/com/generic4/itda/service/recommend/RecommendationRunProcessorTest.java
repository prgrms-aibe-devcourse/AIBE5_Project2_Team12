package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.spy;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationRunProcessorTest {

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @InjectMocks
    private RecommendationRunProcessor recommendationRunProcessor;

    @Nested
    @DisplayName("process")
    class Process {

        @Test
        @DisplayName("존재하지 않는 run이면 예외가 발생한다")
        void 존재하지_않는_run이면_예외가_발생한다() {
            given(recommendationRunRepository.findById(1L))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> recommendationRunProcessor.process(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 추천 실행입니다.");
        }

        @Test
        @DisplayName("RUNNING 상태가 아니면 예외가 발생한다")
        void RUNNING_상태가_아니면_예외가_발생한다() {
            RecommendationRun run = createRun(false);

            given(recommendationRunRepository.findById(1L))
                    .willReturn(Optional.of(run));

            assertThatThrownBy(() -> recommendationRunProcessor.process(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("RUNNING 상태의 추천 실행만 처리할 수 있습니다.");
        }

        @Test
        @DisplayName("RUNNING 상태의 run이면 예외없이 처리한다")
        void RUNNING_상태의_run이면_예외_없이_처리한다() {
            RecommendationRun run = createRun(true);

            given(recommendationRunRepository.findById(1L))
                    .willReturn(Optional.of(run));

            assertThatCode(() -> recommendationRunProcessor.process(1L))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("처리 중 예외가 발생하면 FAILED 상태로 전이하고 예외 메시지를 저장한다")
    void 처리_중_예외가_발생하면_FAILED_상태로_전이하고_예외_메시지를_저장한다() {
        RecommendationRun run = createRun(true);
        RecommendationRunProcessor spyProcessor = spy(
                new RecommendationRunProcessor(recommendationRunRepository)
        );

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        willThrow(new RuntimeException("하드 필터 계산 실패"))
                .given(spyProcessor).doProcess(run);

        assertThatCode(() -> spyProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("하드 필터 계산 실패");
    }

    @Test
    @DisplayName("예외 메시지가 없으면 기본 실패 메시지로 FAILED 처리한다")
    void 예외_메시지가_없으면_기본_실패_메시지로_FAILED_처리한다() {
        RecommendationRun run = createRun(true);
        RecommendationRunProcessor spyProcessor = org.mockito.Mockito.spy(
                new RecommendationRunProcessor(recommendationRunRepository)
        );

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        org.mockito.BDDMockito.willThrow(new RuntimeException())
                .given(spyProcessor).doProcess(run);

        assertThatCode(() -> spyProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(
                com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("추천 실행 중 오류가 발생했습니다.");
    }

    private RecommendationRun createRun(boolean running) {
        RecommendationRun run = RecommendationRun.create(
                createProposalPosition(),
                running ? "fp-running" : "fp-pending",
                RecommendationAlgorithm.HEURISTIC_V1,
                5
        );

        if (running) {
            run.markRunning();
        }

        return run;
    }

    private ProposalPosition createProposalPosition() {
        Member member = createMember();

        Position position = Position.create("백엔드 개발자");

        Proposal proposal = Proposal.create(
                member,
                "AI 매칭 플랫폼 구축",
                "원문 내용",
                null,
                null,
                null,
                null
        );

        return proposal.addPosition(
                position,
                "백엔드 개발자",
                null,
                2L,
                500_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        );
    }
}
