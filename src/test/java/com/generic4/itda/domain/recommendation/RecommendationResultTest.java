package com.generic4.itda.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecommendationResultTest {

    private RecommendationRun mockRun;
    private Resume mockResume;
    private ReasonFacts reasonFacts;

    @BeforeEach
    void setUp() {
        mockRun = mock(RecommendationRun.class);
        mockResume = mock(Resume.class);
        reasonFacts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of("핀테크"),
                3,
                List.of("대용량 트래픽 처리 경험")
        );
    }

    private RecommendationResult createResult() {
        return RecommendationResult.create(
                mockRun,
                mockResume,
                1,
                new BigDecimal("0.9500"),
                new BigDecimal("0.8800"),
                reasonFacts
        );
    }

    // -------------------------------------------------------------------------
    // 1. 생성 성공
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create() 정적 팩토리 메서드 - 성공")
    class CreateSuccess {

        @Test
        @DisplayName("생성 시 전달한 필드들이 올바르게 저장된다")
        void 생성_시_필드_정상_저장() {
            // when
            RecommendationResult result = createResult();

            // then
            assertThat(result.getRecommendationRun()).isSameAs(mockRun);
            assertThat(result.getResume()).isSameAs(mockResume);
            assertThat(result.getRank()).isEqualTo(1);
            assertThat(result.getFinalScore()).isEqualByComparingTo(new BigDecimal("0.9500"));
            assertThat(result.getEmbeddingScore()).isEqualByComparingTo(new BigDecimal("0.8800"));
            assertThat(result.getReasonFacts()).isEqualTo(reasonFacts);
        }

        @Test
        @DisplayName("최초 생성 시 llmStatus는 PENDING이다")
        void 생성_직후_llmStatus는_PENDING() {
            // when
            RecommendationResult result = createResult();

            // then
            assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.PENDING);
        }

        @Test
        @DisplayName("최초 생성 시 llmReason은 null이다")
        void 생성_직후_llmReason은_null() {
            // when
            RecommendationResult result = createResult();

            // then
            assertThat(result.getLlmReason()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // 2. 생성 실패
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create() 정적 팩토리 메서드 - 실패")
    class CreateFailure {

        @Test
        @DisplayName("recommendationRun이 null이면 예외가 발생한다")
        void recommendationRun_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(null, mockResume, 1, new BigDecimal("0.5"), new BigDecimal("0.5"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("resume이 null이면 예외가 발생한다")
        void resume_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, null, 1, new BigDecimal("0.5"), new BigDecimal("0.5"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rank가 0이면 예외가 발생한다")
        void rank_0이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 0, new BigDecimal("0.5"), new BigDecimal("0.5"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rank가 음수이면 예외가 발생한다")
        void rank_음수이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, -1, new BigDecimal("0.5"), new BigDecimal("0.5"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("finalScore가 null이면 예외가 발생한다")
        void finalScore_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, null, new BigDecimal("0.5"), reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingScore가 null이면 예외가 발생한다")
        void embeddingScore_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("0.5"), null, reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("reasonFacts가 null이면 예외가 발생한다")
        void reasonFacts_null이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("0.5"), new BigDecimal("0.5"),
                            null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("finalScore가 0 미만이면 예외가 발생한다")
        void finalScore_0미만이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("-0.0001"),
                            new BigDecimal("0.5"), reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("finalScore가 1 초과이면 예외가 발생한다")
        void finalScore_1초과이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("1.0001"), new BigDecimal("0.5"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingScore가 0 미만이면 예외가 발생한다")
        void embeddingScore_0미만이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("0.5"),
                            new BigDecimal("-0.0001"), reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingScore가 1 초과이면 예외가 발생한다")
        void embeddingScore_1초과이면_예외() {
            assertThatThrownBy(() ->
                    RecommendationResult.create(mockRun, mockResume, 1, new BigDecimal("0.5"), new BigDecimal("1.0001"),
                            reasonFacts))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 3. markLlmReady() 성공
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markLlmReady() - 성공")
    class MarkLlmReadySuccess {

        @Test
        @DisplayName("markLlmReady() 호출 후 llmStatus가 READY로 변경된다")
        void markLlmReady_후_상태는_READY() {
            // given
            RecommendationResult result = createResult();

            // when
            result.markLlmReady("  추천 설명입니다.  ");

            // then
            assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.READY);
        }

        @Test
        @DisplayName("markLlmReady() 호출 시 llmReason은 trim 처리되어 저장된다")
        void markLlmReady_후_llmReason_trim_처리() {
            // given
            RecommendationResult result = createResult();

            // when
            result.markLlmReady("  추천 설명입니다.  ");

            // then
            assertThat(result.getLlmReason()).isEqualTo("추천 설명입니다.");
        }
    }

    // -------------------------------------------------------------------------
    // 4. markLlmReady() 실패
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markLlmReady() - 실패")
    class MarkLlmReadyFailure {

        @Test
        @DisplayName("llmReason이 null이면 예외가 발생한다")
        void llmReason_null이면_예외() {
            // given
            RecommendationResult result = createResult();

            // then
            assertThatThrownBy(() -> result.markLlmReady(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("llmReason이 빈 문자열이면 예외가 발생한다")
        void llmReason_빈문자열이면_예외() {
            // given
            RecommendationResult result = createResult();

            // then
            assertThatThrownBy(() -> result.markLlmReady(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("llmReason이 공백만으로 구성되면 예외가 발생한다")
        void llmReason_공백만이면_예외() {
            // given
            RecommendationResult result = createResult();

            // then
            assertThatThrownBy(() -> result.markLlmReady("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 5. markLlmFailed()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("markLlmFailed()")
    class MarkLlmFailed {

        @Test
        @DisplayName("PENDING 상태에서 호출하면 llmStatus가 FAILED가 된다")
        void PENDING에서_markLlmFailed_후_상태는_FAILED() {
            // given
            RecommendationResult result = createResult();

            // when
            result.markLlmFailed();

            // then
            assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
        }

        @Test
        @DisplayName("PENDING 상태에서 호출하면 llmReason은 null이다")
        void PENDING에서_markLlmFailed_후_llmReason은_null() {
            // given
            RecommendationResult result = createResult();

            // when
            result.markLlmFailed();

            // then
            assertThat(result.getLlmReason()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // 6. score 경계값
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("score 경계값 허용")
    class ScoreBoundary {

        @Test
        @DisplayName("finalScore와 embeddingScore가 모두 0.0이면 정상 생성된다")
        void score가_0이면_정상_생성() {
            // when
            RecommendationResult result = RecommendationResult.create(
                    mockRun, mockResume, 1,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    reasonFacts
            );

            // then
            assertThat(result.getFinalScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getEmbeddingScore()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.PENDING);
            assertThat(result.getLlmReason()).isNull();
        }

        @Test
        @DisplayName("finalScore와 embeddingScore가 모두 1.0이면 정상 생성된다")
        void score가_1이면_정상_생성() {
            // when
            RecommendationResult result = RecommendationResult.create(
                    mockRun, mockResume, 1,
                    BigDecimal.ONE, BigDecimal.ONE,
                    reasonFacts
            );

            // then
            assertThat(result.getFinalScore()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.getEmbeddingScore()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.PENDING);
            assertThat(result.getLlmReason()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // 7. 잘못된 상태 전이
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("잘못된 상태 전이 시 예외 발생")
    class InvalidTransitions {

        @Test
        @DisplayName("READY 상태에서 markLlmReady()를 다시 호출하면 예외가 발생한다")
        void READY에서_markLlmReady_재호출_예외() {
            // given
            RecommendationResult result = createResult();
            result.markLlmReady("첫 설명");

            // when / then
            assertThatThrownBy(() -> result.markLlmReady("두번째 설명"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("FAILED 상태에서 markLlmReady()를 호출하면 예외가 발생한다")
        void FAILED에서_markLlmReady_호출_예외() {
            // given
            RecommendationResult result = createResult();
            result.markLlmFailed();

            // when / then
            assertThatThrownBy(() -> result.markLlmReady("설명"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("READY 상태에서 markLlmFailed()를 호출하면 예외가 발생한다")
        void READY에서_markLlmFailed_호출_예외() {
            // given
            RecommendationResult result = createResult();
            result.markLlmReady("설명");

            // when / then
            assertThatThrownBy(result::markLlmFailed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("FAILED 상태에서 markLlmFailed()를 다시 호출하면 예외가 발생한다")
        void FAILED에서_markLlmFailed_재호출_예외() {
            // given
            RecommendationResult result = createResult();
            result.markLlmFailed();

            // when / then
            assertThatThrownBy(result::markLlmFailed)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING");
        }
    }
}
