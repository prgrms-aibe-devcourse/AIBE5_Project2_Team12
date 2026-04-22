package com.generic4.itda.service.recommend.reason;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generic4.itda.config.ai.RecommendationReasonProperties;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationReasonProcessorTest {

    @Mock
    private RecommendationReasonProperties properties;

    @Mock
    private RecommendationReasonGenerator recommendationReasonGenerator;

    @InjectMocks
    private RecommendationReasonProcessor recommendationReasonProcessor;

    @Captor
    private ArgumentCaptor<RecommendationReasonContext> contextCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(properties.isEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("results가 null이면 예외 없이 종료되고 generator는 호출되지 않는다")
    void process_whenResultsIsNull_doesNothing() {
        // when & then
        assertThatCode(() -> recommendationReasonProcessor.process(null))
                .doesNotThrowAnyException();

        verify(recommendationReasonGenerator, times(0)).generate(any());
    }

    @Test
    @DisplayName("results가 빈 리스트면 예외 없이 종료되고 generator는 호출되지 않는다")
    void process_whenResultsIsEmpty_doesNothing() {
        // when & then
        assertThatCode(() -> recommendationReasonProcessor.process(List.of()))
                .doesNotThrowAnyException();

        verify(recommendationReasonGenerator, times(0)).generate(any());
    }

    @Test
    @DisplayName("추천 이유 생성 옵션이 꺼져 있으면 PENDING result를 skip 하고 generator를 호출하지 않는다")
    void process_whenRecommendationReasonDisabled_skipsPendingResults() {
        // given
        RecommendationResult result = createPendingResult();
        when(properties.isEnabled()).thenReturn(false);

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.PENDING);
        assertThat(result.getLlmReason()).isNull();
        verify(recommendationReasonGenerator, times(0)).generate(any());
    }

    @Test
    @DisplayName("PENDING 상태 result에 대해 generator가 정상 문자열을 반환하면 READY 상태가 되고 이유가 저장된다")
    void process_whenSuccess_marksReady() {
        // given
        RecommendationResult result = createPendingResult();
        String expectedReason = "적합한 후보자입니다.";
        when(recommendationReasonGenerator.generate(any())).thenReturn(expectedReason);

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.READY);
        assertThat(result.getLlmReason()).isEqualTo(expectedReason);
    }

    @Test
    @DisplayName("generator가 앞뒤 공백이 있는 문자열을 반환하면 최종 저장된 이유는 trim 처리된다")
    void process_whenReasonHasSpaces_trimsReason() {
        // given
        RecommendationResult result = createPendingResult();
        String reasonWithSpaces = "  공백이 있는 이유  ";
        when(recommendationReasonGenerator.generate(any())).thenReturn(reasonWithSpaces);

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.READY);
        assertThat(result.getLlmReason()).isEqualTo("공백이 있는 이유");
    }

    @Test
    @DisplayName("generator가 null을 반환하면 FAILED 처리된다")
    void process_whenGeneratorReturnsNull_marksFailed() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any())).thenReturn(null);

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
        assertThat(result.getLlmReason()).isNull();
    }

    @Test
    @DisplayName("generator가 빈 문자열을 반환하면 FAILED 처리된다")
    void process_whenGeneratorReturnsEmpty_marksFailed() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any())).thenReturn("");

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
    }

    @Test
    @DisplayName("generator가 공백 문자열을 반환하면 FAILED 처리된다")
    void process_whenGeneratorReturnsBlank_marksFailed() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any())).thenReturn("   ");

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
    }

    @Test
    @DisplayName("generator 호출 중 예외 발생 시 FAILED 처리된다")
    void process_whenGeneratorThrowsException_marksFailed() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any())).thenThrow(new RuntimeException("LLM Error"));

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        assertThat(result.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
    }

    @Test
    @DisplayName("여러 result 중 하나가 실패해도 나머지는 계속 처리된다")
    void process_handlesMultipleResultsIndependently() {
        // given
        RecommendationResult successResult = createPendingResult();
        RecommendationResult failResult = createPendingResult();

        when(recommendationReasonGenerator.generate(any()))
                .thenReturn("성공 이유") // 첫 번째 호출
                .thenThrow(new RuntimeException("실패")); // 두 번째 호출

        // when
        recommendationReasonProcessor.process(List.of(successResult, failResult));

        // then
        assertThat(successResult.getLlmStatus()).isEqualTo(LlmStatus.READY);
        assertThat(failResult.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
    }

    @Test
    @DisplayName("이미 READY 상태인 result는 skip 되어 generator가 호출되지 않는다")
    void process_skipsReadyResults() {
        // given
        RecommendationResult readyResult = createPendingResult();
        readyResult.markLlmReady("이미 완료됨");

        // when
        recommendationReasonProcessor.process(List.of(readyResult));

        // then
        verify(recommendationReasonGenerator, times(0)).generate(any());
    }

    @Test
    @DisplayName("이미 FAILED 상태인 result는 skip 되어 generator가 호출되지 않는다")
    void process_skipsFailedResults() {
        // given
        RecommendationResult failedResult = createPendingResult();
        failedResult.markLlmFailed();

        // when
        recommendationReasonProcessor.process(List.of(failedResult));

        // then
        verify(recommendationReasonGenerator, times(0)).generate(any());
    }

    @Test
    @DisplayName("processor는 generator 내부 예외를 바깥으로 전파하지 않는다")
    void process_doesNotPropagateGeneratorExceptions() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any()))
                .thenThrow(new RuntimeException("LLM Error"));

        // when & then
        assertThatCode(() -> recommendationReasonProcessor.process(List.of(result)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("generator가 받은 RecommendationReasonContext의 내용이 기대대로 매핑되는지 검증한다")
    void process_verifiesContextMapping() {
        // given
        RecommendationResult result = createPendingResult();
        when(recommendationReasonGenerator.generate(any())).thenReturn("이유");

        // when
        recommendationReasonProcessor.process(List.of(result));

        // then
        verify(recommendationReasonGenerator).generate(contextCaptor.capture());
        RecommendationReasonContext capturedContext = contextCaptor.getValue();

        assertThat(capturedContext.proposalTitle()).isEqualTo("AI 매칭 플랫폼 구축");
        assertThat(capturedContext.positionName()).isEqualTo("백엔드 개발자");
        assertThat(capturedContext.reasonFacts().matchedSkills()).containsExactly("Java", "Spring");
        assertThat(capturedContext.reasonFacts().careerYears()).isEqualTo(3);
        assertThat(capturedContext.finalScore()).isEqualByComparingTo("0.8500");
        assertThat(capturedContext.reasonFacts().highlights()).containsExactly("공통 스킬 2개 보유", "관련 경력 3년");
    }

    @Test
    @DisplayName("PENDING 상태 개수만큼 generator가 호출된다")
    void process_callsGeneratorOnlyForPending() {
        // given
        RecommendationResult r1 = createPendingResult();
        RecommendationResult r2 = createPendingResult();
        RecommendationResult r3 = createPendingResult();

        r2.markLlmReady("이미 완료");

        when(recommendationReasonGenerator.generate(any()))
                .thenReturn("이유");

        // when
        recommendationReasonProcessor.process(List.of(r1, r2, r3));

        // then
        verify(recommendationReasonGenerator, times(2)).generate(any());
    }

    private RecommendationResult createPendingResult() {
        RecommendationRun run = mock(RecommendationRun.class);
        ProposalPosition position = mock(ProposalPosition.class);
        Proposal proposal = mock(Proposal.class);
        Resume resume = mock(Resume.class);

        lenient().when(run.getProposalPosition()).thenReturn(position);
        lenient().when(position.getProposal()).thenReturn(proposal);
        lenient().when(proposal.getTitle()).thenReturn("AI 매칭 플랫폼 구축");
        lenient().when(position.getTitle()).thenReturn("백엔드 개발자");

        ReasonFacts reasonFacts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of(),
                3,
                List.of("공통 스킬 2개 보유", "관련 경력 3년")
        );

        return RecommendationResult.create(
                run,
                resume,
                1,
                new BigDecimal("0.8500"),
                new BigDecimal("0.8000"),
                reasonFacts
        );
    }
}
