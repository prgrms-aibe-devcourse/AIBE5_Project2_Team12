package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.ResumeStatus;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.exception.QueryEmbeddingGenerationException;
import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import com.generic4.itda.service.recommend.reason.RecommendationReasonProcessor;
import com.generic4.itda.service.recommend.scoring.CareerAdjustmentCalculator;
import com.generic4.itda.service.recommend.scoring.CosineSimilarityCalculator;
import com.generic4.itda.service.recommend.scoring.HeuristicV1RecommendationScorer;
import com.generic4.itda.service.recommend.scoring.QueryEmbeddingGenerator;
import com.generic4.itda.service.recommend.scoring.RecommendationQueryTextGenerator;
import com.generic4.itda.service.recommend.scoring.ResumeEmbeddingReader;
import com.generic4.itda.service.recommend.scoring.SkillAdjustmentCalculator;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationRunProcessorTest {

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private RecommendationCandidateFinder recommendationCandidateFinder;

    @Mock
    private HeuristicV1RecommendationScorer recommendationScorer;

    @Mock
    private RecommendationResultCreator recommendationResultCreator;

    @Mock
    private RecommendationReasonProcessor recommendationReasonProcessor;

    @Mock
    private RecommendationQueryTextGenerator recommendationQueryTextGenerator;

    @Mock
    private AiEmbeddingProperties aiEmbeddingProperties;

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
        @DisplayName("RUNNING 상태의 run이면 예외 없이 처리하고 COMPUTED 상태가 된다")
        void RUNNING_상태의_run이면_예외_없이_처리한다() {
            ProposalPosition proposalPosition = createProposalPositionWithSkills();
            RecommendationRun run = createRun(proposalPosition, true);

            List<RecommendationCandidate> candidates = List.of(
                    createCandidate(101L, 3,
                            createCandidateSkill(1L, "Java", Proficiency.ADVANCED),
                            createCandidateSkill(3L, "Kotlin", Proficiency.INTERMEDIATE)),
                    createCandidate(202L, 1,
                            createCandidateSkill(2L, "Spring", Proficiency.BEGINNER))
            );
            List<ScoredCandidate> scoredCandidates = List.of();
            List<RecommendationResult> results = List.of(org.mockito.Mockito.mock(RecommendationResult.class));

            given(recommendationRunRepository.findById(1L))
                    .willReturn(Optional.of(run));
            given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                    .willReturn(candidates);
            given(recommendationScorer.score(
                    any(),
                    any(),
                    anySet(),
                    anySet(),
                    anyList()
            )).willReturn(scoredCandidates);
            given(recommendationResultCreator.create(
                    run,
                    scoredCandidates,
                    run.getTopK(),
                    Set.of("Java")
            )).willReturn(results);

            assertThatCode(() -> recommendationRunProcessor.process(1L))
                    .doesNotThrowAnyException();

            assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
            assertThat(run.getCandidateCount()).isEqualTo(1);
            assertThat(run.getHardFilterStats()).isEqualTo(new HardFilterStat(2, 1));
            then(recommendationScorer).should().score(
                    proposalPosition.getProposal(),
                    proposalPosition,
                    Set.of("Java"),
                    Set.of("Spring"),
                    List.of(
                            new RecommendationScorableCandidate(101L, 3, Set.of("Java", "Kotlin")),
                            new RecommendationScorableCandidate(202L, 1, Set.of("Spring"))
                    )
            );
            then(recommendationResultCreator).should().create(
                    run,
                    scoredCandidates,
                    run.getTopK(),
                    Set.of("Java")
            );
            then(recommendationResultRepository).should().saveAll(results);
            then(recommendationReasonProcessor).should().process(results);
        }
    }

    @Test
    @DisplayName("finder 처리 중 예외가 발생하면 FAILED 상태로 전이하고 예외 메시지를 저장한다")
    void 처리_중_예외가_발생하면_FAILED_상태로_전이하고_예외_메시지를_저장한다() {
        RecommendationRun run = createRun(true);

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willThrow(new RuntimeException("하드 필터 계산 실패"));

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("하드 필터 계산 실패");
        then(recommendationScorer).shouldHaveNoInteractions();
        then(recommendationResultCreator).shouldHaveNoInteractions();
        then(recommendationResultRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("후보가 존재하고 query embedding 생성이 성공하면 scorer 실경로를 거쳐 결과를 저장하고 COMPUTED 상태가 된다")
    void query_embedding_생성이_성공하면_scorer_실경로를_거쳐_정상_완료된다() {
        ProposalPosition proposalPosition = createProposalPositionWithSkills();
        RecommendationRun run = createRun(proposalPosition, true);
        RecommendationRunProcessor processorWithRealScorer = createProcessorWithRealScorer();

        List<RecommendationCandidate> candidates = List.of(
                createCandidate(101L, 3,
                        createCandidateSkill(1L, "Java", Proficiency.ADVANCED),
                        createCandidateSkill(2L, "Spring", Proficiency.INTERMEDIATE))
        );
        List<Double> queryEmbedding = List.of(0.1, 0.9);
        List<Double> resumeEmbedding = List.of(0.8, 0.2);
        List<RecommendationResult> results = List.of(org.mockito.Mockito.mock(RecommendationResult.class));

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationQueryTextGenerator.generate(
                proposalPosition.getProposal(),
                proposalPosition,
                Set.of("Java"),
                Set.of("Spring")
        )).willReturn("query text");
        given(aiEmbeddingProperties.resolveEmbeddingModel()).willReturn("text-embedding-3-small:stub");
        given(queryEmbeddingGenerator.generate("query text"))
                .willReturn(queryEmbedding);
        given(resumeEmbeddingReader.readEmbeddingsByResumeIds(List.of(101L), "text-embedding-3-small:stub"))
                .willReturn(Map.of(101L, resumeEmbedding));
        given(cosineSimilarityCalculator.calculate(queryEmbedding, resumeEmbedding))
                .willReturn(0.4);
        given(skillAdjustmentCalculator.calculate(anySet(), anySet(), anySet()))
                .willReturn(0.1);
        given(careerAdjustmentCalculator.calculate(anyInt(), any(), any()))
                .willReturn(0.08);
        given(recommendationResultCreator.create(
                any(),
                anyList(),
                anyInt(),
                anySet()
        )).willReturn(results);

        assertThatCode(() -> processorWithRealScorer.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(run.getErrorMessage()).isNull();
        assertThat(run.getHardFilterStats()).isEqualTo(new HardFilterStat(1, 1));
        assertThat(run.getCandidateCount()).isEqualTo(1);
        then(queryEmbeddingGenerator).should().generate("query text");
        then(recommendationResultCreator).should().create(
                eq(run),
                anyList(),
                anyInt(),
                anySet()
        );
        then(recommendationResultRepository).should().saveAll(results);
    }

    @Test
    @DisplayName("query embedding 생성 중 예외가 발생하면 FAILED 상태로 전이하고 결과를 저장하지 않는다")
    void query_embedding_생성_실패시_FAILED_처리하고_결과를_저장하지_않는다() {
        ProposalPosition proposalPosition = createProposalPositionWithSkills();
        RecommendationRun run = createRun(proposalPosition, true);
        RecommendationRunProcessor processorWithRealScorer = createProcessorWithRealScorer();

        List<RecommendationCandidate> candidates = List.of(
                createCandidate(101L, 3,
                        createCandidateSkill(1L, "Java", Proficiency.ADVANCED))
        );

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationQueryTextGenerator.generate(
                proposalPosition.getProposal(),
                proposalPosition,
                Set.of("Java"),
                Set.of("Spring")
        )).willReturn("query text");
        given(aiEmbeddingProperties.resolveEmbeddingModel()).willReturn("text-embedding-3-small:stub");
        given(queryEmbeddingGenerator.generate("query text"))
                .willThrow(new QueryEmbeddingGenerationException("임베딩 실패"));

        assertThatCode(() -> processorWithRealScorer.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("임베딩 실패");
        then(recommendationResultCreator).shouldHaveNoInteractions();
        then(recommendationResultRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("scorer 내부 예외가 발생하면 FAILED 상태로 전이하고 결과를 저장하지 않는다")
    void scorer_내부_예외가_발생하면_FAILED_처리하고_결과를_저장하지_않는다() {
        RecommendationRun run = createRun(true);

        List<RecommendationCandidate> candidates = List.of(
                createCandidate(10L, 3,
                        createCandidateSkill(1L, "Java", Proficiency.ADVANCED))
        );

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationScorer.score(
                any(),
                any(),
                anySet(),
                anySet(),
                anyList()
        )).willThrow(new RuntimeException("scorer 계산 실패"));

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("scorer 계산 실패");
        then(recommendationResultCreator).shouldHaveNoInteractions();
        then(recommendationResultRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("scorer 예외 메시지가 없으면 기본 실패 메시지로 FAILED 처리한다")
    void scorer_예외_메시지가_없으면_기본_실패_메시지로_FAILED_처리한다() {
        RecommendationRun run = createRun(true);

        List<RecommendationCandidate> candidates = List.of(
                createCandidate(10L, 3,
                        createCandidateSkill(1L, "Java", Proficiency.ADVANCED))
        );

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationScorer.score(
                any(),
                any(),
                anySet(),
                anySet(),
                anyList()
        )).willThrow(new RuntimeException());

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("추천 실행 중 오류가 발생했습니다.");
        then(recommendationResultCreator).shouldHaveNoInteractions();
        then(recommendationResultRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("예외 메시지가 없으면 기본 실패 메시지로 FAILED 처리한다")
    void 예외_메시지가_없으면_기본_실패_메시지로_FAILED_처리한다() {
        RecommendationRun run = createRun(true);

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willThrow(new RuntimeException());

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("추천 실행 중 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("예외 메시지가 공백이면 기본 실패 메시지로 FAILED 처리한다")
    void 예외_메시지가_공백이면_기본_실패_메시지로_FAILED_처리한다() {
        RecommendationRun run = createRun(true);

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willThrow(new RuntimeException("   "));

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("추천 실행 중 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("result 저장 중 예외가 발생하면 FAILED 상태로 전이한다")
    void 결과_저장_중_예외가_발생하면_FAILED_상태로_전이한다() {
        RecommendationRun run = createRun(true);

        List<RecommendationCandidate> candidates = List.of();
        List<ScoredCandidate> scoredCandidates = List.of();
        List<RecommendationResult> results = List.of();

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationScorer.score(
                any(),
                any(),
                anySet(),
                anySet(),
                anyList()
        )).willReturn(scoredCandidates);
        given(recommendationResultCreator.create(
                run,
                scoredCandidates,
                run.getTopK(),
                Set.of()
        )).willReturn(results);
        org.mockito.BDDMockito.willThrow(new RuntimeException("저장 실패"))
                .given(recommendationResultRepository).saveAll(results);

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(run.getErrorMessage()).isEqualTo("저장 실패");
    }

    @Test
    @DisplayName("추천 이유 생성 중 예외가 발생해도 추천 실행은 COMPUTED 상태로 완료된다")
    void 추천_이유_생성_중_예외가_발생해도_추천_실행은_COMPUTED_상태로_완료된다() {
        RecommendationRun run = createRun(true);

        List<RecommendationCandidate> candidates = List.of(
                createCandidate(10L, 3,
                        createCandidateSkill(1L, "Java", Proficiency.ADVANCED))
        );
        List<ScoredCandidate> scoredCandidates = List.of();
        List<RecommendationResult> results = List.of(org.mockito.Mockito.mock(RecommendationResult.class));

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationScorer.score(
                any(),
                any(),
                anySet(),
                anySet(),
                anyList()
        )).willReturn(scoredCandidates);
        given(recommendationResultCreator.create(
                run,
                scoredCandidates,
                run.getTopK(),
                Set.of()
        )).willReturn(results);
        org.mockito.BDDMockito.willThrow(new RuntimeException("추천 이유 생성 실패"))
                .given(recommendationReasonProcessor).process(results);

        assertThatCode(() -> recommendationRunProcessor.process(1L))
                .doesNotThrowAnyException();

        assertThat(run.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(run.getErrorMessage()).isNull();
        assertThat(run.getHardFilterStats()).isEqualTo(new HardFilterStat(1, 1));
        assertThat(run.getCandidateCount()).isEqualTo(1);
        then(recommendationResultRepository).should().saveAll(results);
        then(recommendationReasonProcessor).should().process(results);
    }

    private RecommendationRun createRun(boolean running) {
        return createRun(createProposalPosition(), running);
    }

    private RecommendationRun createRun(ProposalPosition proposalPosition, boolean running) {
        RecommendationRun run = RecommendationRun.create(
                proposalPosition,
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

    @Test
    @DisplayName("후보를 scoring 입력 모델로 변환해 scorer에 전달한다")
    void 후보를_scoring_입력_모델로_변환해_scorer에_전달한다() {
        RecommendationRun run = createRun(true);

        RecommendationCandidate candidate = new RecommendationCandidate(
                10L,
                ResumeStatus.ACTIVE,
                true,
                true,
                (byte) 3,
                List.of(
                        new RecommendationCandidate.CandidateSkill(1L, "Java", Proficiency.ADVANCED),
                        new RecommendationCandidate.CandidateSkill(2L, "Spring", Proficiency.INTERMEDIATE)
                )
        );

        List<RecommendationCandidate> candidates = List.of(candidate);
        List<ScoredCandidate> scoredCandidates = List.of();
        List<RecommendationResult> results = List.of();

        given(recommendationRunRepository.findById(1L))
                .willReturn(Optional.of(run));
        given(recommendationCandidateFinder.findCandidates(run.getProposalPosition(), run.getTopK()))
                .willReturn(candidates);
        given(recommendationScorer.score(
                any(),
                any(),
                anySet(),
                anySet(),
                any(List.class)
        )).willReturn(scoredCandidates);
        given(recommendationResultCreator.create(
                any(),
                any(List.class),
                anyInt(),
                anySet()
        )).willReturn(results);

        recommendationRunProcessor.process(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RecommendationScorableCandidate>> captor =
                ArgumentCaptor.forClass(List.class);

        then(recommendationScorer).should().score(
                any(),
                any(),
                anySet(),
                anySet(),
                captor.capture()
        );

        List<RecommendationScorableCandidate> actual = captor.getValue();
        assertThat(actual).hasSize(1);

        RecommendationScorableCandidate scorable = actual.get(0);
        assertThat(scorable.resumeId()).isEqualTo(10L);
        assertThat(scorable.careerYears()).isEqualTo(3);
        assertThat(scorable.ownedSkillNames()).containsExactlyInAnyOrder("Java", "Spring");
    }

    private ProposalPosition createProposalPositionWithSkills() {
        ProposalPosition proposalPosition = createProposalPosition();
        proposalPosition.addSkill(Skill.create("Java", null), ProposalPositionSkillImportance.ESSENTIAL);
        proposalPosition.addSkill(Skill.create("Spring", null), ProposalPositionSkillImportance.PREFERENCE);
        return proposalPosition;
    }

    private RecommendationCandidate createCandidate(
            long resumeId,
            int careerYears,
            RecommendationCandidate.CandidateSkill... skills
    ) {
        return new RecommendationCandidate(
                resumeId,
                ResumeStatus.ACTIVE,
                true,
                true,
                (byte) careerYears,
                List.of(skills)
        );
    }

    private RecommendationCandidate.CandidateSkill createCandidateSkill(
            long skillId,
            String skillName,
            Proficiency proficiency
    ) {
        return new RecommendationCandidate.CandidateSkill(skillId, skillName, proficiency);
    }

    private RecommendationRunProcessor createProcessorWithRealScorer() {
        HeuristicV1RecommendationScorer realScorer = new HeuristicV1RecommendationScorer(
                aiEmbeddingProperties,
                recommendationQueryTextGenerator,
                queryEmbeddingGenerator,
                resumeEmbeddingReader,
                cosineSimilarityCalculator,
                skillAdjustmentCalculator,
                careerAdjustmentCalculator
        );

        return new RecommendationRunProcessor(
                recommendationRunRepository,
                recommendationResultRepository,
                recommendationCandidateFinder,
                realScorer,
                recommendationResultCreator,
                recommendationReasonProcessor
        );
    }
}
