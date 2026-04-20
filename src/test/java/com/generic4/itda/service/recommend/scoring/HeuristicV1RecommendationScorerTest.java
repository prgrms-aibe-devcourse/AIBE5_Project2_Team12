package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
    }

    // -------------------------------------------------------------------------
    // 시나리오 3: finalScore 내림차순 정렬
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
    // 시나리오 4: raw cosine 정규화 → embeddingScore
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
    // 시나리오 5: finalScore 상한 clamp → 1.0
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("embeddingScore와 보정치의 합이 1.0을 초과하면 finalScore는 1.0으로 clamp된다")
    void score_ClampsFinalScore_ToOneWhenSumExceedsOne() {
        // given — rawCosine(0.8) + skill(0.15) + career(0.08) = 1.03 → clamp 1.0
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
        assertThat(result.get(0).scoreBreakdown().finalScore()).isEqualTo(1.0);
    }

    // -------------------------------------------------------------------------
    // 시나리오 6: finalScore 하한 clamp → 0.0
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("embeddingScore와 보정치의 합이 0.0 미만이면 finalScore는 0.0으로 clamp된다")
    void score_ClampsFinalScore_ToZeroWhenSumBelowZero() {
        // given — rawCosine(-0.8) + skill(-0.15) + career(-0.12) = -1.07 → clamp 0.0
        RecommendationScorableCandidate candidate = candidateWith(100L);
        List<Double> queryEmbedding = dummyQueryEmbedding();
        List<Double> resumeEmbedding = List.of(-0.8, 0.2);

        given(recommendationQueryTextGenerator.generate(any(), any(), any(), any()))
                .willReturn("query text");
        given(queryEmbeddingGenerator.generate(anyString())).willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(any(), anyString()))
                .willReturn(Map.of(100L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding)).willReturn(-0.8);
        given(skillAdjustmentCalculator.calculate(any(), any(), any())).willReturn(-0.15);
        given(careerAdjustmentCalculator.calculate(any(int.class), any(), any())).willReturn(-0.12);

        // when
        List<ScoredCandidate> result = scorer.score(
                proposal, proposalPosition, Set.of(), Set.of(), List.of(candidate), EMBEDDING_MODEL
        );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).scoreBreakdown().finalScore()).isEqualTo(0.0);
    }
}
