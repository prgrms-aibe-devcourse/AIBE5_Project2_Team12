package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.BDDMockito.given;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeStatus;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import com.generic4.itda.service.recommend.reason.RecommendationReasonGenerator;
import com.generic4.itda.service.recommend.scoring.HeuristicV1RecommendationScorer;
import com.generic4.itda.service.recommend.scoring.model.RecommendationScorableCandidate;
import com.generic4.itda.service.recommend.scoring.model.ScoreBreakdown;
import com.generic4.itda.service.recommend.scoring.model.ScoredCandidate;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class RecommendationRunProcessorIntegrationTest {

    private static final RecommendationAlgorithm DEFAULT_ALGORITHM = RecommendationAlgorithm.HEURISTIC_V1;
    private static final int DEFAULT_TOP_K = 3;

    @Autowired
    private RecommendationRunProcessor recommendationRunProcessor;

    @Autowired
    private RecommendationRunRepository recommendationRunRepository;

    @Autowired
    private RecommendationResultRepository recommendationResultRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private RecommendationCandidateFinder recommendationCandidateFinder;

    @MockitoBean
    private HeuristicV1RecommendationScorer recommendationScorer;

    @MockitoBean
    private RecommendationResultCreator recommendationResultCreator;

    @MockitoBean
    private RecommendationReasonGenerator recommendationReasonGenerator;

    @Test
    @DisplayName("RUNNING 상태 run 처리 완료 후 RecommendationResult가 DB에 저장되고 추천 이유까지 반영되며 status가 COMPUTED로 변경된다")
    void process_happyPath_savesResultsAndMarksComputed() {
        // given
        RunFixture fixture = createRunningRunFixture("proc-happy@test.com", "fp-happy");
        PreparedRecommendationFixture prepared = createSingleCandidateResultFixture(
                fixture,
                "resume-owner-happy@test.com"
        );

        given(recommendationCandidateFinder.findCandidates(any(), anyInt())).willReturn(List.of(prepared.candidate()));
        given(recommendationScorer.score(any(), any(), anySet(), anySet(), anyList())).willReturn(List.of(prepared.scoredCandidate()));
        given(recommendationResultCreator.create(any(), anyList(), anyInt(), anySet())).willReturn(List.of(prepared.result()));
        given(recommendationReasonGenerator.generate(any())).willReturn("  Java 경험이 적합합니다.  ");

        entityManager.flush();
        entityManager.clear();

        // when
        assertThatCode(() -> recommendationRunProcessor.process(fixture.runId()))
                .doesNotThrowAnyException();

        entityManager.flush();
        entityManager.clear();

        // then: RecommendationRun 상태 검증
        RecommendationRun persistedRun = recommendationRunRepository.findById(fixture.runId()).orElseThrow();
        assertThat(persistedRun.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(persistedRun.getErrorMessage()).isNull();

        // then: HardFilterStat 반영 검증 (candidates=1개, results=1개)
        HardFilterStat stat = persistedRun.getHardFilterStats();
        assertThat(stat).isNotNull();
        assertThat(stat.totalCandidates()).isEqualTo(1);
        assertThat(stat.finalCandidates()).isEqualTo(1);
        assertThat(persistedRun.getCandidateCount()).isEqualTo(1);

        // then: RecommendationResult DB 저장 검증
        List<RecommendationResult> savedResults = recommendationResultRepository.findAll();
        assertThat(savedResults).hasSize(1);
        RecommendationResult savedResult = savedResults.get(0);
        assertThat(savedResult.getRecommendationRun().getId()).isEqualTo(fixture.runId());
        assertThat(savedResult.getResume().getId()).isEqualTo(prepared.resume().getId());
        assertThat(savedResult.getRank()).isEqualTo(1);
        assertThat(savedResult.getLlmStatus()).isEqualTo(LlmStatus.READY);
        assertThat(savedResult.getLlmReason()).isEqualTo("Java 경험이 적합합니다.");
    }

    @Test
    @DisplayName("처리 중 예외 발생 시 process()가 예외를 전파하지 않고 status가 FAILED로 변경되며 errorMessage가 저장된다")
    void process_exceptionDuringProcessing_doesNotPropagateAndMarksFailed() {
        // given
        RunFixture fixture = createRunningRunFixture("proc-fail@test.com", "fp-fail");

        given(recommendationCandidateFinder.findCandidates(any(), anyInt()))
                .willThrow(new RuntimeException("후보 조회 중 오류 발생"));

        entityManager.flush();
        entityManager.clear();

        // when
        assertThatCode(() -> recommendationRunProcessor.process(fixture.runId()))
                .doesNotThrowAnyException();

        entityManager.flush();
        entityManager.clear();

        // then: RecommendationRun 상태 검증
        RecommendationRun persistedRun = recommendationRunRepository.findById(fixture.runId()).orElseThrow();
        assertThat(persistedRun.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(persistedRun.getErrorMessage()).isEqualTo("후보 조회 중 오류 발생");

        // then: RecommendationResult가 저장되지 않았음을 검증
        assertThat(recommendationResultRepository.count()).isZero();
    }

    @Test
    @DisplayName("추천 이유 생성에 실패해도 RecommendationResult는 저장되고 run status는 COMPUTED로 유지된다")
    void process_reasonGenerationFails_savesResultAndKeepsComputed() {
        // given
        RunFixture fixture = createRunningRunFixture("proc-reason-fail@test.com", "fp-reason-fail");
        PreparedRecommendationFixture prepared = createSingleCandidateResultFixture(
                fixture,
                "resume-owner-reason-fail@test.com"
        );

        given(recommendationCandidateFinder.findCandidates(any(), anyInt())).willReturn(List.of(prepared.candidate()));
        given(recommendationScorer.score(any(), any(), anySet(), anySet(), anyList())).willReturn(List.of(prepared.scoredCandidate()));
        given(recommendationResultCreator.create(any(), anyList(), anyInt(), anySet())).willReturn(List.of(prepared.result()));
        given(recommendationReasonGenerator.generate(any())).willThrow(new RuntimeException("추천 이유 생성 실패"));

        entityManager.flush();
        entityManager.clear();

        // when
        assertThatCode(() -> recommendationRunProcessor.process(fixture.runId()))
                .doesNotThrowAnyException();

        entityManager.flush();
        entityManager.clear();

        // then
        RecommendationRun persistedRun = recommendationRunRepository.findById(fixture.runId()).orElseThrow();
        assertThat(persistedRun.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(persistedRun.getErrorMessage()).isNull();
        assertThat(persistedRun.getHardFilterStats()).isEqualTo(new HardFilterStat(1, 1));

        List<RecommendationResult> savedResults = recommendationResultRepository.findAll();
        assertThat(savedResults).hasSize(1);
        RecommendationResult savedResult = savedResults.get(0);
        assertThat(savedResult.getResume().getId()).isEqualTo(prepared.resume().getId());
        assertThat(savedResult.getLlmStatus()).isEqualTo(LlmStatus.FAILED);
        assertThat(savedResult.getLlmReason()).isNull();
    }

    private RunFixture createRunningRunFixture(String ownerEmail, String fingerprint) {
        Member owner = memberRepository.save(createMember(ownerEmail, "pw", "제안자", "010-0000-0001"));
        Position position = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", null));

        Proposal proposal = Proposal.create(owner, "AI 추천 테스트 제안서", "원본 입력", "설명", 5_000_000L, 10_000_000L, 6L);
        ProposalPosition proposalPosition = proposal.addPosition(position, "백엔드 개발자", null, 2L, 1_000_000L, 2_000_000L, null, null, null, null);
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);

        RecommendationRun run = RecommendationRun.create(proposalPosition, fingerprint, DEFAULT_ALGORITHM, DEFAULT_TOP_K);
        run.markRunning();
        persist(run);

        return new RunFixture(run.getId(), run);
    }

    private PreparedRecommendationFixture createSingleCandidateResultFixture(RunFixture fixture, String resumeOwnerEmail) {
        Member resumeOwner = memberRepository.save(createMember(resumeOwnerEmail, "pw", "이력서소유자", "010-1111-2222"));
        Resume resume = persist(Resume.create(
                resumeOwner,
                "백엔드 개발 경험",
                (byte) 3,
                new CareerPayload(),
                null,
                ResumeWritingStatus.DONE,
                null
        ));

        RecommendationCandidate candidate = new RecommendationCandidate(
                resume.getId(),
                ResumeStatus.ACTIVE,
                true,
                true,
                (byte) 3,
                List.of(new RecommendationCandidate.CandidateSkill(1L, "Java", com.generic4.itda.domain.resume.Proficiency.ADVANCED))
        );
        RecommendationScorableCandidate scorableCandidate = new RecommendationScorableCandidate(
                resume.getId(),
                3,
                Set.of("Java")
        );
        ScoredCandidate scoredCandidate = new ScoredCandidate(
                scorableCandidate,
                new ScoreBreakdown(0.7000, 0.1000, 0.0500, 0.8500)
        );
        RecommendationResult result = RecommendationResult.create(
                fixture.run(),
                resume,
                1,
                BigDecimal.valueOf(0.8500),
                BigDecimal.valueOf(0.7000),
                new ReasonFacts(List.of("Java"), List.of(), 3, List.of())
        );

        return new PreparedRecommendationFixture(resume, candidate, scoredCandidate, result);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private record RunFixture(Long runId, RecommendationRun run) {}

    private record PreparedRecommendationFixture(
            Resume resume,
            RecommendationCandidate candidate,
            ScoredCandidate scoredCandidate,
            RecommendationResult result
    ) {}
}
