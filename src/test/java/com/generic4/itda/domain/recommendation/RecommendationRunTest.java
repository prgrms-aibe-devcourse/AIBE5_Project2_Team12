package com.generic4.itda.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecommendationRunTest {

    private ProposalPosition mockPosition;
    private HardFilterStat stat;

    @BeforeEach
    void setUp() {
        mockPosition = mock(ProposalPosition.class);
        stat = new HardFilterStat(10, 8, 5, 3); // finalCount() == 3
    }

    private RecommendationRun createPendingRun() {
        return RecommendationRun.create(mockPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5);
    }

    // -------------------------------------------------------------------------
    // 1. 생성
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create() 정적 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("생성 직후 status는 PENDING이다")
        void 생성_직후_상태는_PENDING() {
            RecommendationRun run = createPendingRun();

            assertThat(run.getStatus()).isEqualTo(RecommendationAlgorithm.RecommendationRunStatus.PENDING);
        }

        @Test
        @DisplayName("생성 직후 candidateCount, hardFilterStats, errorMessage는 null이다")
        void 생성_직후_선택_필드는_null() {
            RecommendationRun run = createPendingRun();

            assertThat(run.getCandidateCount()).isNull();
            assertThat(run.getHardFilterStats()).isNull();
            assertThat(run.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("생성 시 전달한 필드들이 올바르게 설정된다")
        void 생성_시_필수_필드_설정() {
            RecommendationRun run = createPendingRun();

            assertThat(run.getProposalPosition()).isSameAs(mockPosition);
            assertThat(run.getRequestFingerprint()).isEqualTo("fp-abc123");
            assertThat(run.getAlgorithm()).isEqualTo(RecommendationAlgorithm.HEURISTIC_V1);
            assertThat(run.getTopK()).isEqualTo(5);
        }

        @Test
        @DisplayName("proposalPosition이 null이면 예외가 발생한다")
        void proposalPosition_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationRun.create(null, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requestFingerprint가 빈 문자열이면 예외가 발생한다")
        void fingerprint_공백이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationRun.create(mockPosition, " ", RecommendationAlgorithm.HEURISTIC_V1, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("topK가 0 이하이면 예외가 발생한다")
        void topK_0이하이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationRun.create(mockPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 2. 유효한 상태 전이
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("유효한 상태 전이")
    class ValidTransitions {

        @Test
        @DisplayName("PENDING → RUNNING: markRunning() 호출 후 status가 RUNNING이 된다")
        void PENDING에서_RUNNING으로_전이() {
            RecommendationRun run = createPendingRun();

            run.markRunning();

            assertThat(run.getStatus()).isEqualTo(RecommendationAlgorithm.RecommendationRunStatus.RUNNING);
        }

        @Test
        @DisplayName("RUNNING → COMPUTED: markCompleted() 호출 후 status가 COMPUTED가 된다")
        void RUNNING에서_COMPUTED로_전이() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markCompleted(stat);

            assertThat(run.getStatus()).isEqualTo(RecommendationAlgorithm.RecommendationRunStatus.COMPUTED);
        }

        @Test
        @DisplayName("RUNNING → FAILED: markFailed() 호출 후 status가 FAILED가 된다")
        void RUNNING에서_FAILED로_전이() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markFailed("AI 서버 연결 실패");

            assertThat(run.getStatus()).isEqualTo(RecommendationAlgorithm.RecommendationRunStatus.FAILED);
        }
    }

    // -------------------------------------------------------------------------
    // 3. 완료 상태 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markCompleted() 후 상태 검증")
    class CompletedState {

        @Test
        @DisplayName("candidateCount는 stat.finalCount()와 같다")
        void candidateCount_정상_설정() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markCompleted(stat);

            assertThat(run.getCandidateCount()).isEqualTo(stat.finalCount());
        }

        @Test
        @DisplayName("hardFilterStats는 전달한 stat 객체와 같다")
        void hardFilterStats_정상_설정() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markCompleted(stat);

            assertThat(run.getHardFilterStats()).isEqualTo(stat);
        }

        @Test
        @DisplayName("완료 후 errorMessage는 null이다")
        void 완료_후_errorMessage는_null() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markCompleted(stat);

            assertThat(run.getErrorMessage()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // 4. 실패 상태 검증
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markFailed() 후 상태 검증")
    class FailedState {

        @Test
        @DisplayName("errorMessage가 전달한 사유로 설정된다")
        void errorMessage_정상_설정() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            run.markFailed("AI 서버 타임아웃");

            assertThat(run.getErrorMessage()).isEqualTo("AI 서버 타임아웃");
        }
    }

    // -------------------------------------------------------------------------
    // 5. 잘못된 상태 전이
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("잘못된 상태 전이 시 IllegalStateException 발생")
    class InvalidTransitions {

        @Test
        @DisplayName("PENDING 상태에서 markCompleted()를 호출하면 예외가 발생한다")
        void PENDING에서_markCompleted_예외() {
            RecommendationRun run = createPendingRun();

            assertThatThrownBy(() -> run.markCompleted(stat))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("PENDING 상태에서 markFailed()를 호출하면 예외가 발생한다")
        void PENDING에서_markFailed_예외() {
            RecommendationRun run = createPendingRun();

            assertThatThrownBy(() -> run.markFailed("실패 사유"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("RUNNING 상태에서 markRunning()을 다시 호출하면 예외가 발생한다")
        void RUNNING에서_markRunning_예외() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            assertThatThrownBy(run::markRunning)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPUTED 상태에서 markRunning()을 호출하면 예외가 발생한다")
        void COMPUTED에서_markRunning_예외() {
            RecommendationRun run = createPendingRun();
            run.markRunning();
            run.markCompleted(stat);

            assertThatThrownBy(run::markRunning)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPUTED 상태에서 markCompleted()를 다시 호출하면 예외가 발생한다")
        void COMPUTED에서_markCompleted_예외() {
            RecommendationRun run = createPendingRun();
            run.markRunning();
            run.markCompleted(stat);

            assertThatThrownBy(() -> run.markCompleted(stat))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("COMPUTED 상태에서 markFailed()를 호출하면 예외가 발생한다")
        void COMPUTED에서_markFailed_예외() {
            RecommendationRun run = createPendingRun();
            run.markRunning();
            run.markCompleted(stat);

            assertThatThrownBy(() -> run.markFailed("실패 사유"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 6. 헬퍼 메서드
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("상태 확인 헬퍼 메서드")
    class StatusHelpers {

        @Test
        @DisplayName("생성 직후 isPending()은 true, 나머지는 false이다")
        void PENDING_상태_헬퍼() {
            RecommendationRun run = createPendingRun();

            assertThat(run.isPending()).isTrue();
            assertThat(run.isRunning()).isFalse();
            assertThat(run.isComputed()).isFalse();
            assertThat(run.isFailed()).isFalse();
            assertThat(run.isTerminalStatus()).isFalse();
        }

        @Test
        @DisplayName("markRunning() 후 isRunning()은 true, 나머지는 false이다")
        void RUNNING_상태_헬퍼() {
            RecommendationRun run = createPendingRun();
            run.markRunning();

            assertThat(run.isRunning()).isTrue();
            assertThat(run.isPending()).isFalse();
            assertThat(run.isComputed()).isFalse();
            assertThat(run.isFailed()).isFalse();
            assertThat(run.isTerminalStatus()).isFalse();
        }

        @Test
        @DisplayName("markCompleted() 후 isComputed()와 isTerminalStatus()는 true이다")
        void COMPUTED_상태_헬퍼() {
            RecommendationRun run = createPendingRun();
            run.markRunning();
            run.markCompleted(stat);

            assertThat(run.isComputed()).isTrue();
            assertThat(run.isTerminalStatus()).isTrue();
            assertThat(run.isPending()).isFalse();
            assertThat(run.isRunning()).isFalse();
            assertThat(run.isFailed()).isFalse();
        }

        @Test
        @DisplayName("markFailed() 후 isFailed()와 isTerminalStatus()는 true이다")
        void FAILED_상태_헬퍼() {
            RecommendationRun run = createPendingRun();
            run.markRunning();
            run.markFailed("오류 발생");

            assertThat(run.isFailed()).isTrue();
            assertThat(run.isTerminalStatus()).isTrue();
            assertThat(run.isPending()).isFalse();
            assertThat(run.isRunning()).isFalse();
            assertThat(run.isComputed()).isFalse();
        }
    }
}
