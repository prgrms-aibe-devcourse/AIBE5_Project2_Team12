package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeRepository;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoreBreakdown;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationResultCreatorTest {

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private RecommendationResultCreator creator;

    @Nested
    @DisplayName("finalScore 기준 내림차순 정렬")
    class SortByFinalScore {

        @Test
        @DisplayName("여러 후보가 finalScore 내림차순으로 정렬되고 rank는 1부터 순서대로 부여된다")
        void finalScore_내림차순_정렬_및_rank_순서_검증() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate low = scoredCandidate(1L, 10L, 101L, 0.5, 3);
            ScoredCandidate mid = scoredCandidate(2L, 20L, 102L, 0.7, 3);
            ScoredCandidate high = scoredCandidate(3L, 30L, 103L, 0.9, 3);

            Resume resume101 = resumeWithId(101L);
            Resume resume102 = resumeWithId(102L);
            Resume resume103 = resumeWithId(103L);

            given(resumeRepository.findAllById(anyList()))
                    .willReturn(List.of(resume101, resume102, resume103));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(low, mid, high), 3, Set.of()
            );

            // then
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getRank()).isEqualTo(1);
            assertThat(results.get(0).getResume()).isEqualTo(resume103); // 0.9
            assertThat(results.get(1).getRank()).isEqualTo(2);
            assertThat(results.get(1).getResume()).isEqualTo(resume102); // 0.7
            assertThat(results.get(2).getRank()).isEqualTo(3);
            assertThat(results.get(2).getResume()).isEqualTo(resume101); // 0.5
        }
    }

    @Nested
    @DisplayName("resumeId 기준 중복 제거")
    class DeduplicateByResumeId {

        @Test
        @DisplayName("같은 resumeId를 가진 후보 중 finalScore가 높은 것만 결과에 포함된다")
        void 같은_resumeId_중_높은_점수만_결과에_포함() {
            // given
            // 낮은 점수가 입력 리스트에 먼저 위치 → 정렬 후 dedup 시 올바르게 제거되는지 검증
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate lowerScore = scoredCandidate(1L, 10L, 100L, 0.4, 2);
            ScoredCandidate higherScore = scoredCandidate(2L, 20L, 100L, 0.8, 2);

            Resume resume = resumeWithId(100L);
            given(resumeRepository.findAllById(anyList()))
                    .willReturn(List.of(resume));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(lowerScore, higherScore), 3, Set.of()
            );

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getFinalScore().doubleValue()).isEqualTo(0.8);
        }
    }

    @Nested
    @DisplayName("topK 제한")
    class TopKLimit {

        @Test
        @DisplayName("후보가 topK보다 많으면 결과는 정확히 topK개다")
        void 후보가_topK보다_많으면_결과는_topK개() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate c1 = scoredCandidate(1L, 10L, 101L, 0.9, 3);
            ScoredCandidate c2 = scoredCandidate(2L, 20L, 102L, 0.7, 3);
            ScoredCandidate c3 = scoredCandidate(3L, 30L, 103L, 0.5, 3);

            Resume resume101 = resumeWithId(101L);
            Resume resume102 = resumeWithId(102L);
            given(resumeRepository.findAllById(anyList()))
                    .willReturn(List.of(resume101, resume102));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(c1, c2, c3), 2, Set.of()
            );

            // then
            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Resume 일괄 조회")
    class ResumeLoading {

        @Test
        @DisplayName("resumeRepository.findAllById() 결과가 RecommendationResult에 정확히 매핑된다")
        void resume_일괄_조회_결과가_결과에_매핑된다() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate c1 = scoredCandidate(1L, 10L, 201L, 0.9, 2);
            ScoredCandidate c2 = scoredCandidate(2L, 20L, 202L, 0.7, 2);

            Resume resume201 = resumeWithId(201L);
            Resume resume202 = resumeWithId(202L);

            given(resumeRepository.findAllById(anyList()))
                    .willReturn(List.of(resume201, resume202));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(c1, c2), 2, Set.of()
            );

            // then
            assertThat(results.get(0).getResume()).isEqualTo(resume201);
            assertThat(results.get(1).getResume()).isEqualTo(resume202);
        }

        @Test
        @DisplayName("일부 resumeId에 해당하는 Resume이 조회되지 않으면 IllegalStateException이 발생한다")
        void 일부_resume_누락_시_예외_발생() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate c1 = scoredCandidate(1L, 10L, 301L, 0.9, 2);
            ScoredCandidate c2 = scoredCandidate(2L, 20L, 302L, 0.7, 2);

            // 302L에 해당하는 Resume 누락
            Resume resume301 = resumeWithId(301L);
            given(resumeRepository.findAllById(anyList()))
                    .willReturn(List.of(resume301));

            // when & then
            assertThatThrownBy(() -> creator.create(run, List.of(c1, c2), 2, Set.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("추천 결과 생성 대상 이력서 일부를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("ReasonFacts 생성")
    class ReasonFactsCreation {

        @Test
        @DisplayName("후보 스킬과 requiredSkillNames의 교집합이 matchedSkills로 정렬되어 설정된다")
        void matchedSkills가_교집합으로_정확히_계산된다() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate candidate = scoredCandidateWithSkills(
                    1L, 10L, 401L, 0.9, 3,
                    Set.of("Java", "Spring", "Redis")
            );

            Resume resume = resumeWithId(401L);
            given(resumeRepository.findAllById(anyList())).willReturn(List.of(resume));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(candidate), 1, Set.of("Spring", "Redis", "Docker")
            );

            // then
            ReasonFacts facts = results.get(0).getReasonFacts();
            assertThat(facts.matchedSkills())
                    .containsExactly("Redis", "Spring"); // 알파벳 오름차순 정렬
        }

        @Test
        @DisplayName("matchedSkills가 있으면 highlights에 스킬 개수가, careerYears > 0이면 경력이, similarityScore >= 0.8이면 유사도 메시지가 포함된다")
        void highlights가_규칙대로_생성된다() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            // careerYears=5, similarityScore=0.9, matchedSkills=["Java","Spring"]
            ScoredCandidate candidate = scoredCandidateWithSkills(
                    1L, 10L, 501L, 0.9, 5,
                    Set.of("Java", "Spring")
            );

            Resume resume = resumeWithId(501L);
            given(resumeRepository.findAllById(anyList())).willReturn(List.of(resume));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(candidate), 1, Set.of("Java", "Spring")
            );

            // then
            List<String> highlights = results.get(0).getReasonFacts().highlights();
            assertThat(highlights).contains("공토 스킬 2개 보유");
            assertThat(highlights).contains("관련 경력 5년");
            assertThat(highlights).contains("요구 조건과 높은 유사도");
        }

        @Test
        @DisplayName("requiredSkillNames가 비어있으면 matchedSkills는 빈 리스트이고 스킬 highlight는 없다")
        void requiredSkillNames가_비어있으면_matchedSkills_비어있음() {
            // given
            RecommendationRun run = mock(RecommendationRun.class);

            ScoredCandidate candidate = scoredCandidateWithSkills(
                    1L, 10L, 601L, 0.5, 0,
                    Set.of("Java", "Spring")
            );

            Resume resume = resumeWithId(601L);
            given(resumeRepository.findAllById(anyList())).willReturn(List.of(resume));

            // when
            List<RecommendationResult> results = creator.create(
                    run, List.of(candidate), 1, Set.of()
            );

            // then
            ReasonFacts facts = results.get(0).getReasonFacts();
            assertThat(facts.matchedSkills()).isEmpty();
            assertThat(facts.highlights()).doesNotContainAnyElementsOf(
                    List.of("공토 스킬 2개 보유")
            );
        }
    }

    // --- fixture helpers ---

    private ScoredCandidate scoredCandidate(
            Long candidateId, Long memberId, Long resumeId, double finalScore, int careerYears
    ) {
        return scoredCandidateWithSkills(candidateId, memberId, resumeId, finalScore, careerYears, Set.of());
    }

    private ScoredCandidate scoredCandidateWithSkills(
            Long candidateId, Long memberId, Long resumeId,
            double finalScore, int careerYears, Set<String> skills
    ) {
        RecommendationScorableCandidate candidate = new RecommendationScorableCandidate(
                candidateId, memberId, resumeId, careerYears, skills
        );
        ScoreBreakdown breakdown = new ScoreBreakdown(finalScore, 0.0, 0.0, finalScore);
        return new ScoredCandidate(candidate, breakdown);
    }

    private Resume resumeWithId(Long id) {
        Resume resume = mock(Resume.class);
        given(resume.getId()).willReturn(id);
        return resume;
    }
}
