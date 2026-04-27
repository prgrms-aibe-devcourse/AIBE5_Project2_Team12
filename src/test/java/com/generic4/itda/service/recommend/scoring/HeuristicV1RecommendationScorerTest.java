package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("HeuristicV1RecommendationScorer 단위 테스트")
class HeuristicV1RecommendationScorerTest {

    @Mock
    private AiEmbeddingProperties properties;

    @Mock
    private RecommendationQueryTextGenerator recommendationQueryTextGenerator;

    @Mock
    private QueryEmbeddingGenerator queryEmbeddingGenerator;

    @Mock
    private ResumeEmbeddingReader resumeEmbeddingReader;

    @Mock
    private CosineSimilarityCalculator cosineSimilarityCalculator;

    @Mock
    private SkillAdjustmentCalculator skillAdjustmentCalculator;

    @Mock
    private CareerAdjustmentCalculator careerAdjustmentCalculator;

    @Mock
    private Proposal proposal;

    @Mock
    private ProposalPosition proposalPosition;

    @InjectMocks
    private HeuristicV1RecommendationScorer scorer;

    private static final String EMBEDDING_MODEL = "text-embedding-3-small";

    // -------------------------------------------------------------------------
    // 공통 헬퍼
    // -------------------------------------------------------------------------

    private RecommendationScorableCandidate candidateWith(long resumeId) {
        return new RecommendationScorableCandidate(resumeId, 3, Set.of("Java"));
    }

    private List<Double> dummyQueryEmbedding() {
        return List.of(1.0, 0.0);
    }

    // -------------------------------------------------------------------------
    // 시나리오 0: 기본 모델 사용 및 후보 정보 전달
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("기본 score 호출은 기본 임베딩 모델과 후보 정보를 그대로 사용한다")
    void score_UsesDefaultEmbeddingModel_AndPassesCandidateDataToCollaborators() {
        // given
        RecommendationScorableCandidate candidate =
                new RecommendationScorableCandidate(100L, 7, Set.of("Java", "Spring"));
        Set<String> requiredSkills = Set.of("Java");
        Set<String> preferredSkills = Set.of("Spring", "Docker");
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.2, 0.8);

        given(properties.resolveEmbeddingModel()).willReturn(EMBEDDING_MODEL);
        given(proposalPosition.getCareerMinYears()).willReturn(5);
        given(proposalPosition.getCareerMaxYears()).willReturn(8);
        given(recommendationQueryTextGenerator.generate(
                proposal,
                proposalPosition,
                requiredSkills,
                preferredSkills
        )).willReturn("query text");
        given(queryEmbeddingGenerator.generate("query text")).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(100L), EMBEDDING_MODEL))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(0.2);
        given(skillAdjustmentCalculator.calculate(requiredSkills, preferredSkills, candidate.ownedSkillNames()))
                .willReturn(0.10);
        given(careerAdjustmentCalculator.calculate(7, 5, 8)).willReturn(0.08);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal,
                proposalPosition,
                requiredSkills,
                preferredSkills,
                List.of(candidate)
        );

        // then
        assertThat(result).singleElement().satisfies(scoredCandidate -> {
            assertThat(scoredCandidate.resumeId()).isEqualTo(100L);
            assertThat(scoredCandidate.careerYears()).isEqualTo(7);
            assertThat(scoredCandidate.ownedSkillNames()).containsExactlyInAnyOrder("Java", "Spring");
            assertThat(scoredCandidate.similarityScore()).isEqualTo(0.6);
            assertThat(scoredCandidate.skillAdjustmentScore()).isEqualTo(0.10);
            assertThat(scoredCandidate.careerAdjustmentScore()).isEqualTo(0.08);
            assertThat(scoredCandidate.finalScore()).isCloseTo(0.655, offset(1e-9));
        });

        verify(recommendationQueryTextGenerator).generate(
                proposal,
                proposalPosition,
                requiredSkills,
                preferredSkills
        );
        verify(queryEmbeddingGenerator).generate("query text");
        verify(resumeEmbeddingReader).readEmbeddingsByResumeIds(List.of(100L), EMBEDDING_MODEL);
        verify(skillAdjustmentCalculator).calculate(requiredSkills, preferredSkills, candidate.ownedSkillNames());
        verify(careerAdjustmentCalculator).calculate(7, 5, 8);
    }

    @Test
    @DisplayName("명시적 embeddingModel을 전달하면 properties가 아닌 전달된 모델로 reader를 호출한다")
    void score_UsesExplicitEmbeddingModel_NotProperties_WhenModelParamIsPassed() {
        // given
        String customModel = "custom-model-v1";
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.2, 0.8);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(100L), customModel))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(any(), any())).willReturn(0.0);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.0);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.0);

        // when
        scorer.score(proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), customModel);

        // then
        verify(resumeEmbeddingReader).readEmbeddingsByResumeIds(List.of(100L), customModel);
        verify(properties, never()).resolveEmbeddingModel();
    }

    // -------------------------------------------------------------------------
    // 시나리오 1: 빈 후보 리스트
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("후보가 비어 있으면 빈 리스트를 반환한다")
    void score_ReturnsEmptyList_WhenCandidatesIsEmpty() {
        // given
        List<RecommendationScorableCandidate> emptyCandidates = List.of();

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), emptyCandidates, EMBEDDING_MODEL
        );

        // then
        assertThat(result).isEmpty();

        verify(recommendationQueryTextGenerator, never()).generate(any(), any(), any(), any());
        verify(queryEmbeddingGenerator, never()).generate(anyString());
        verify(resumeEmbeddingReader, never()).readEmbeddingsByResumeIds(anyList(), anyString());
    }

    // -------------------------------------------------------------------------
    // 시나리오 2: 이력서 임베딩이 없는 후보는 결과에서 제외
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("이력서 임베딩이 없는 후보는 결과에서 제외된다")
    void score_ExcludesCandidate_WhenResumeEmbeddingIsMissing() {
        // given
        RecommendationScorableCandidate candidate = candidateWith(100L);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString()))
                .willReturn(dummyQueryEmbedding());
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of()); // 임베딩 없음

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).isEmpty();
        verify(cosineSimilarityCalculator, never()).calculate(any(), any());
        verify(skillAdjustmentCalculator, never()).calculate(any(), any(), any());
        verify(careerAdjustmentCalculator, never()).calculate(any(int.class), any(), any());
    }

    // -------------------------------------------------------------------------
    // 시나리오 3: 일부 후보 임베딩 누락 시 후속 계산 제외
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("임베딩이 없는 후보는 후속 점수 계산을 수행하지 않는다")
    void score_SkipsDownstreamCalculations_WhenResumeEmbeddingIsMissingForSomeCandidates() {
        // given
        RecommendationScorableCandidate missingEmbeddingCandidate =
                new RecommendationScorableCandidate(100L, 3, Set.of("Java"));
        RecommendationScorableCandidate scorableCandidate =
                new RecommendationScorableCandidate(200L, 4, Set.of("Spring"));

        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.4, 0.6);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate("query text")).willReturn(queryEmbedding);
        given(proposalPosition.getCareerMinYears()).willReturn(null);
        given(proposalPosition.getCareerMaxYears()).willReturn(null);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(100L, 200L), EMBEDDING_MODEL))
                .willReturn(Map.of(200L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(0.4);
        given(skillAdjustmentCalculator.calculate(Set.of(), Set.of(), scorableCandidate.ownedSkillNames()))
                .willReturn(0.0);
        given(careerAdjustmentCalculator.calculate(4, null, null)).willReturn(0.0);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal,
                proposalPosition,
                Set.of(),
                Set.of(),
                List.of(missingEmbeddingCandidate, scorableCandidate),
                EMBEDDING_MODEL
        );

        // then
        assertThat(result).singleElement().satisfies(candidate -> {
            assertThat(candidate.resumeId()).isEqualTo(200L);
            assertThat(candidate.finalScore()).isEqualTo(0.7);
        });

        verify(cosineSimilarityCalculator).calculate(queryEmbedding, resumeEmbedding);
        verify(skillAdjustmentCalculator).calculate(Set.of(), Set.of(), scorableCandidate.ownedSkillNames());
        verify(careerAdjustmentCalculator).calculate(4, null, null);
        verifyNoMoreInteractions(cosineSimilarityCalculator, skillAdjustmentCalculator, careerAdjustmentCalculator);
    }

    // -------------------------------------------------------------------------
    // 시나리오 4: finalScore 내림차순 정렬
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("최종 점수(finalScore) 내림차순으로 정렬한다")
    void score_ReturnsCandidatesSortedByFinalScoreDescending() {
        // given
        RecommendationScorableCandidate candidateA = candidateWith(100L);
        RecommendationScorableCandidate candidateB = candidateWith(200L);

        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> embeddingA = List.of(1.0, 0.0);
        List<Double> embeddingB = List.of(0.0, 1.0);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString()))
                .willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, embeddingA, 200L, embeddingB));

        // candidateA: rawCosine=0.6, skill=0.0, career=0.0 → finalScore=0.6
        given(cosineSimilarityCalculator.calculate(queryEmbedding, embeddingA)).willReturn(0.6);
        given(cosineSimilarityCalculator.calculate(queryEmbedding, embeddingB)).willReturn(0.3);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.0);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.0);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(),
                List.of(candidateA, candidateB), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).scoreBreakdown().finalScore())
                .isGreaterThan(result.get(1).scoreBreakdown().finalScore());
        assertThat(result.get(0).candidate().resumeId()).isEqualTo(100L);
        assertThat(result.get(1).candidate().resumeId()).isEqualTo(200L);
    }

    // -------------------------------------------------------------------------
    // 시나리오 5: raw cosine 정규화 → embeddingScore
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("raw cosine -1.0은 embeddingScore 0.0으로 정규화된다")
    void score_NormalizesRawCosineMinusOne_ToZero() {
        // given
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.0, 1.0);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(-1.0);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.0);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.0);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().similarityScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("raw cosine 1.0은 embeddingScore 1.0으로 정규화된다")
    void score_NormalizesRawCosinePlusOne_ToOne() {
        // given
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(1.0, 0.0);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(1.0);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.0);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.0);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().similarityScore()).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // 시나리오 6: finalScore 상한 clamp → 1.0
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("높은 임베딩과 일반적인 최대 보정치 조합에서도 finalScore가 바로 1.0으로 포화되지 않는다")
    void score_DoesNotImmediatelySaturateToOne_WithHighEmbeddingAndNormalAdjustments() {
        // given — normalized embedding(0.9) + skill(0.15*0.35) + career(0.08*0.25) = 0.9725
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.8, 0.2);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(0.8);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.15);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.08);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().finalScore()).isCloseTo(0.9725, offset(1e-9));
    }

    @Test
    @DisplayName("가중 반영 후 합이 1.0을 초과하면 finalScore는 1.0으로 clamp된다")
    void score_ClampsFinalScore_ToOneWhenSumExceedsOne() {
        // given — normalized embedding(0.9) + skill(0.50*0.35) + career(0.30*0.25) = 1.15 → clamp 1.0
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(0.8, 0.2);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(0.8);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(0.50);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(0.30);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().finalScore()).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // 시나리오 7: finalScore 하한 clamp → 0.0
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("가중 반영 후 합이 0.0 미만이면 finalScore는 0.0으로 clamp된다")
    void score_ClampsFinalScore_ToZeroWhenSumBelowZero() {
        // given — normalized embedding(0.1) + skill(-0.50*0.35) + career(-0.50*0.25) = -0.20 → clamp 0.0
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(-0.8, 0.2);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(-0.8);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(-0.50);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(-0.50);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().finalScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("proposal이 null이면 예외가 발생한다")
    void score_ThrowsException_WhenProposalIsNull() {
        assertThatThrownBy(() -> scorer.score(
                null,
                proposalPosition,
                Set.of(),
                Set.of(),
                List.of(candidateWith(100L)),
                EMBEDDING_MODEL
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("proposal은 필수입니다.");
    }

    @Test
    @DisplayName("proposalPosition이 null이면 예외가 발생한다")
    void score_ThrowsException_WhenProposalPositionIsNull() {
        assertThatThrownBy(() -> scorer.score(
                proposal,
                null,
                Set.of(),
                Set.of(),
                List.of(candidateWith(100L)),
                EMBEDDING_MODEL
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("proposalPosition은 필수입니다.");
    }

    @Test
    @DisplayName("candidates가 null이면 예외가 발생한다")
    void score_ThrowsException_WhenCandidatesIsNull() {
        assertThatThrownBy(() -> scorer.score(
                proposal,
                proposalPosition,
                Set.of(),
                Set.of(),
                null,
                EMBEDDING_MODEL
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("candidates는 null일 수 없습니다.");
    }
}
