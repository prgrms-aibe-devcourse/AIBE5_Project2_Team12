package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.dto.matching.LatestMatchingSummary;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationRunQueryServiceTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long RUN_ID = 301L;
    private static final Long RESULT_ID = 100L;
    private static final Long PROPOSAL_POSITION_ID = 21L;
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private MatchingRepository matchingRepository;

    @InjectMocks
    private RecommendationRunQueryService service;

    @Test
    @DisplayName("COMPUTED 상태가 아니면 추천 결과 조회 시 예외를 던진다")
    void getRecommendationResults_throwsWhenRunNotComputed() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.PENDING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class);

        then(recommendationRunRepository).should().findDetailById(RUN_ID);
    }

    @Test
    @DisplayName("추천 결과 조회 시 후보별 최신 매칭 상태/ID를 포함한다")
    void getRecommendationResults_includesLatestMatchingSummary() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        long resumeId = 500L;
        long matchingId = 9001L;

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn("홍길동");

        Resume candidateResume = org.mockito.Mockito.mock(Resume.class);
        given(candidateResume.getId()).willReturn(resumeId);
        given(candidateResume.getMember()).willReturn(candidateMember);
        given(candidateResume.getIntroduction()).willReturn(null);
        given(candidateResume.getPreferredWorkType()).willReturn(WorkType.REMOTE);

        ReasonFacts facts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of(),
                7,
                List.of("대규모 트래픽 처리 경험")
        );

        RecommendationResult result = RecommendationResult.create(
                run,
                candidateResume,
                1,
                new BigDecimal("0.9500"),
                new BigDecimal("0.8800"),
                facts
        );
        ReflectionTestUtils.setField(result, "id", RESULT_ID);

        given(recommendationResultRepository.findByRunIdWithResume(RUN_ID)).willReturn(List.of(result));
        given(matchingRepository.getLatestMatchingSummaryMap(eq(PROPOSAL_POSITION_ID), anyCollection()))
                .willReturn(Map.of(resumeId, new LatestMatchingSummary(matchingId, MatchingStatus.PROPOSED)));

        RecommendationResultsViewModel view = service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.candidates()).hasSize(1);

        var item = view.candidates().get(0);
        assertThat(item.resultId()).isEqualTo(RESULT_ID);
        assertThat(item.rank()).isEqualTo(1);
        assertThat(item.maskedName()).isEqualTo("홍*동");
        assertThat(item.introduction()).isEmpty();
        assertThat(item.careerYears()).isEqualTo(7);
        assertThat(item.preferredWorkTypeLabel()).isEqualTo(WorkType.REMOTE.getDescription());
        assertThat(item.finalScorePercent()).isEqualTo(95);
        assertThat(item.embeddingScorePercent()).isEqualTo(88);
        assertThat(item.matchedSkills()).containsExactly("Java", "Spring");
        assertThat(item.highlights()).containsExactly("대규모 트래픽 처리 경험");
        assertThat(item.matchingId()).isEqualTo(matchingId);
        assertThat(item.matchingStatus()).isEqualTo("PROPOSED");

        then(recommendationRunRepository).should().findDetailById(RUN_ID);
        then(recommendationResultRepository).should().findByRunIdWithResume(RUN_ID);
        then(matchingRepository).should().getLatestMatchingSummaryMap(eq(PROPOSAL_POSITION_ID), anyCollection());
    }

    @Test
    @DisplayName("추천 후보 이력서 상세 조회 시 최신 매칭 상태/ID를 포함한다")
    void getRecommendationCandidateResume_includesLatestMatchingSummary() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.COMPUTED);

        long resumeId = 500L;
        long matchingId = 9001L;

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn("이순신");

        Resume resume = org.mockito.Mockito.mock(Resume.class);
        given(resume.getId()).willReturn(resumeId);
        given(resume.getMember()).willReturn(candidateMember);
        given(resume.getIntroduction()).willReturn("소개글");
        given(resume.getPreferredWorkType()).willReturn(WorkType.HYBRID);
        given(resume.getPortfolioUrl()).willReturn("https://example.com");
        given(resume.getSkills()).willReturn(null);
        given(resume.getCareer()).willReturn(null);

        ReasonFacts facts = new ReasonFacts(List.of(), List.of(), 3, List.of());

        RecommendationResult result = RecommendationResult.create(
                run,
                resume,
                1,
                new BigDecimal("0.9000"),
                new BigDecimal("0.8000"),
                facts
        );
        ReflectionTestUtils.setField(result, "id", RESULT_ID);

        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(result));
        given(matchingRepository.getLatestMatchingSummary(eq(PROPOSAL_POSITION_ID), eq(resumeId)))
                .willReturn(Optional.of(new LatestMatchingSummary(matchingId, MatchingStatus.IN_PROGRESS)));

        RecommendationResumeDetailViewModel view = service.getRecommendationCandidateResume(PROPOSAL_ID, RESULT_ID, OWNER_EMAIL);

        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.resultId()).isEqualTo(RESULT_ID);
        assertThat(view.candidate().matchingId()).isEqualTo(matchingId);
        assertThat(view.candidate().matchingStatus()).isEqualTo("IN_PROGRESS");

        then(recommendationResultRepository).should().findDetailById(RESULT_ID);
        then(matchingRepository).should().getLatestMatchingSummary(PROPOSAL_POSITION_ID, resumeId);
    }

    @Test
    @DisplayName("추천 실행 상태 조회 시 상태별 nextActionUrl/autoRefresh 값을 설정한다")
    void getRecommendationRunStatus_setsNextActionUrlByStatus() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        RecommendationRunStatusViewModel view = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.status()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(view.nextActionUrl()).isEqualTo("/proposals/10/recommendations/results?runId=301");
        assertThat(view.autoRefresh()).isFalse();

        then(recommendationRunRepository).should().findDetailById(RUN_ID);
    }

    @ParameterizedTest
    @EnumSource(value = RecommendationRunStatus.class, names = {"PENDING", "RUNNING"})
    @DisplayName("추천 실행 상태가 PENDING/RUNNING이면 autoRefresh=true이고 nextActionUrl이 runs 경로이다")
    void getRecommendationRunStatus_pendingOrRunning_autoRefreshTrueAndRunsUrl(RecommendationRunStatus status) {
        RecommendationRun run = createOwnedRun(status);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        RecommendationRunStatusViewModel view = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        assertThat(view.status()).isEqualTo(status);
        assertThat(view.autoRefresh()).isTrue();
        assertThat(view.nextActionUrl()).isEqualTo("/proposals/10/runs/301");
    }

    @Test
    @DisplayName("추천 실행 상태가 FAILED이면 autoRefresh=false이고 nextActionUrl이 recommendations 경로이다")
    void getRecommendationRunStatus_failed_autoRefreshFalseAndRecommendationsUrl() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.FAILED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        RecommendationRunStatusViewModel view = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        assertThat(view.status()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(view.autoRefresh()).isFalse();
        assertThat(view.nextActionUrl()).isEqualTo("/proposals/10/recommendations");
    }

    @Test
    @DisplayName("추천 실행 조회 시 runId가 존재하지 않으면 예외를 던진다")
    void getRecommendationRunStatus_runNotFound_throwsException() {
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("추천 실행 조회 시 다른 제안서의 runId로 접근하면 예외를 던진다")
    void getRecommendationRunStatus_wrongProposalId_throwsException() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getRecommendationRunStatus(999L, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("추천 실행 조회 시 소유자가 아닌 이메일로 접근하면 예외를 던진다")
    void getRecommendationRunStatus_wrongOwner_throwsException() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        assertThatThrownBy(() -> service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("추천 결과 조회 시 runId가 존재하지 않으면 예외를 던진다")
    void getRecommendationResults_runNotFound_throwsException() {
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("추천 후보 이력서 조회 시 COMPUTED 상태가 아니면 예외를 던진다")
    void getRecommendationCandidateResume_notComputed_throwsException() {
        RecommendationRun run = createOwnedRun(RecommendationRunStatus.PENDING);

        Resume resume = org.mockito.Mockito.mock(Resume.class);
        ReasonFacts facts = new ReasonFacts(List.of(), List.of(), 0, List.of());
        RecommendationResult result = RecommendationResult.create(
                run, resume, 1, new BigDecimal("0.9"), new BigDecimal("0.8"), facts);
        ReflectionTestUtils.setField(result, "id", RESULT_ID);

        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(result));

        assertThatThrownBy(() -> service.getRecommendationCandidateResume(PROPOSAL_ID, RESULT_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("추천 후보 이력서 조회 시 resultId가 존재하지 않으면 예외를 던진다")
    void getRecommendationCandidateResume_resultNotFound_throwsException() {
        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRecommendationCandidateResume(PROPOSAL_ID, RESULT_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RecommendationRun createOwnedRun(RecommendationRunStatus status) {
        Member owner = Member.create(OWNER_EMAIL, "hashed", "Owner", null, null, "010-0000-0000");
        ReflectionTestUtils.setField(owner, "id", 1L);

        Proposal proposal = Proposal.create(owner, "Test Proposal", "raw", "desc", 3_000_000L, 5_000_000L, 3L);
        proposal.startMatching();
        ReflectionTestUtils.setField(proposal, "id", PROPOSAL_ID);
        ReflectionTestUtils.setField(proposal, "status", ProposalStatus.MATCHING);

        Position position = Position.create("Backend");
        ReflectionTestUtils.setField(position, "id", 1001L);

        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                "Backend Dev",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(proposalPosition, "id", PROPOSAL_POSITION_ID);

        RecommendationRun run = RecommendationRun.create(
                proposalPosition,
                "fp-test",
                RecommendationAlgorithm.HEURISTIC_V1,
                3
        );
        ReflectionTestUtils.setField(run, "id", RUN_ID);

        applyStatus(run, status);
        return run;
    }

    private static void applyStatus(RecommendationRun run, RecommendationRunStatus status) {
        switch (status) {
            case PENDING -> {
            }
            case RUNNING -> run.markRunning();
            case COMPUTED -> {
                run.markRunning();
                run.markCompleted(new HardFilterStat(10, 1));
            }
            case FAILED -> {
                run.markRunning();
                run.markFailed("추천 생성 실패");
            }
        }
    }
}
