package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.recommend.RecommendationResumeDetailViewModel;
import com.generic4.itda.dto.recommend.RecommendationResultsViewModel;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RecommendationRunQueryService 단위 테스트: - repository가 준비한 RecommendationRun 상세 aggregate를 바탕으로 접근 검증과 상태별 ViewModel 조립을
 * 수행하는지 확인한다.
 * <p>
 * 한계: - 이 테스트는 Mockito 기반 단위 테스트라 findDetailById의 fetch join 범위나 lazy 초기화는 검증하지 않는다. - proposal/member/proposalPosition
 * 그래프 preload 계약은 RecommendationRunRepositoryTest가 보장해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationRunQueryServiceTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long RUN_ID = 301L;
    private static final Long RESULT_ID = 100L;
    private static final Long OWNER_ID = 1L;
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
    @DisplayName("PENDING 상태면 새로고침 가능한 상태 ViewModel을 반환한다")
    void getRecommendationRunStatus_returnsPendingViewModel() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.PENDING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when
        RecommendationRunStatusViewModel result = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertViewModel(
                result,
                RecommendationRunStatus.PENDING,
                "추천 요청이 접수되었습니다.",
                "선택한 포지션의 추천 결과를 준비하고 있습니다.",
                "새로고침",
                "/proposals/10/runs/301",
                true
        );

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("RUNNING 상태면 새로고침 가능한 진행중 ViewModel을 반환한다")
    void getRecommendationRunStatus_returnsRunningViewModel() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.RUNNING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when
        RecommendationRunStatusViewModel result = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertViewModel(
                result,
                RecommendationRunStatus.RUNNING,
                "추천을 계산하고 있습니다.",
                "잠시 후 추천 결과를 확인할 수 있습니다.",
                "새로고침",
                "/proposals/10/runs/301",
                true
        );

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("COMPUTED 상태면 결과 페이지로 이동하는 ViewModel을 반환한다")
    void getRecommendationRunStatus_returnsComputedViewModel() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when
        RecommendationRunStatusViewModel result = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertViewModel(
                result,
                RecommendationRunStatus.COMPUTED,
                "추천 결과가 준비되었습니다.",
                "추천 결과 목록으로 이동할 수 있습니다.",
                "결과 보기",
                "/proposals/10/recommendations/results?runId=301",
                false
        );

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("FAILED 상태면 재실행 안내 ViewModel을 반환한다")
    void getRecommendationRunStatus_returnsFailedViewModel() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.FAILED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when
        RecommendationRunStatusViewModel result = service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertViewModel(
                result,
                RecommendationRunStatus.FAILED,
                "추천 생성에 실패했습니다.",
                "잠시 후 다시 시도해 주세요.",
                "추천 다시 실행",
                "/proposals/10/recommendations",
                false
        );

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("존재하지 않는 run이면 IllegalArgumentException이 발생한다")
    void getRecommendationRunStatus_throwsWhenRunNotFound() {
        // given
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("추천 실행 정보를 찾을 수 없습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("run이 다른 proposal에 속하면 IllegalArgumentException이 발생한다")
    void getRecommendationRunStatus_throwsWhenProposalDoesNotMatch() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.PENDING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationRunStatus(999L, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 추천 실행 접근입니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("제안서 소유자 이메일과 다르면 IllegalArgumentException이 발생한다")
    void getRecommendationRunStatus_throwsWhenEmailDoesNotMatchOwner() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.PENDING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationRunStatus(PROPOSAL_ID, RUN_ID, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("접근 권한이 없습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
    }

    @Test
    @DisplayName("COMPUTED 상태가 아니면 추천 결과 조회 시 IllegalStateException이 발생한다")
    void getRecommendationResults_throwsWhenRunIsNotComputed() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.PENDING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 결과가 아직 준비되지 않았습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("RUNNING 상태면 추천 결과 조회 시 IllegalStateException이 발생한다")
    void getRecommendationResults_throwsWhenRunIsRunning() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.RUNNING);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 결과가 아직 준비되지 않았습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("FAILED 상태면 추천 결과 조회 시 IllegalStateException이 발생한다")
    void getRecommendationResults_throwsWhenRunIsFailed() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.FAILED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 결과가 아직 준비되지 않았습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("추천 실행(run)이 존재하지 않으면 추천 결과 조회 시 IllegalArgumentException이 발생한다")
    void getRecommendationResults_throwsWhenRunNotFound() {
        // given
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("추천 실행 정보를 찾을 수 없습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("run이 다른 proposal에 속하면 추천 결과 조회 시 IllegalArgumentException이 발생한다")
    void getRecommendationResults_throwsWhenProposalDoesNotMatch() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(999L, RUN_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 추천 실행 접근입니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("제안서 소유자 이메일과 다르면 추천 결과 조회 시 IllegalArgumentException이 발생한다")
    void getRecommendationResults_throwsWhenEmailDoesNotMatchOwner() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationResults(PROPOSAL_ID, RUN_ID, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("접근 권한이 없습니다.");

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("COMPUTED 상태에서도 결과가 없으면 빈 후보 리스트를 반환한다")
    void getRecommendationResults_returnsEmptyCandidatesWhenNoResults() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));
        given(recommendationResultRepository.findByRunIdWithResume(RUN_ID)).willReturn(List.of());

        // when
        RecommendationResultsViewModel view = service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.proposalTitle()).isEqualTo("추천 테스트 제안서");
        assertThat(view.positionTitle()).isEqualTo("백엔드 개발자");
        assertThat(view.topK()).isEqualTo(3);
        assertThat(view.candidateCount()).isEqualTo(3);
        assertThat(view.candidates()).isEmpty();

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verify(recommendationResultRepository).findByRunIdWithResume(RUN_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("COMPUTED 상태면 추천 결과 ViewModel을 조립하고 후보 정보를 마스킹하여 반환한다")
    void getRecommendationResults_returnsViewModelWithCandidates() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn("닉네임");

        Resume candidateResume = org.mockito.Mockito.mock(Resume.class);
        given(candidateResume.getMember()).willReturn(candidateMember);
        given(candidateResume.getIntroduction()).willReturn(null);
        given(candidateResume.getPreferredWorkType()).willReturn(WorkType.REMOTE);

        ReasonFacts facts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of("플랫폼"),
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
        result.markLlmReady("추천 사유입니다.");

        given(recommendationResultRepository.findByRunIdWithResume(RUN_ID)).willReturn(List.of(result));
        given(matchingRepository.getLatestMatchingStatusMap(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection()
        )).willReturn(Map.of());

        // when
        RecommendationResultsViewModel view = service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.proposalTitle()).isEqualTo("추천 테스트 제안서");
        assertThat(view.positionTitle()).isEqualTo("백엔드 개발자");
        assertThat(view.topK()).isEqualTo(3);
        assertThat(view.candidateCount()).isEqualTo(3);
        assertThat(view.candidates()).hasSize(1);

        var item = view.candidates().get(0);
        assertThat(item.rank()).isEqualTo(1);
        assertThat(item.maskedName()).isEqualTo("닉*임");
        assertThat(item.introduction()).isEmpty();
        assertThat(item.careerYears()).isEqualTo(7);
        assertThat(item.preferredWorkTypeLabel()).isEqualTo(WorkType.REMOTE.getDescription());
        assertThat(item.finalScorePercent()).isEqualTo(95);
        assertThat(item.embeddingScorePercent()).isEqualTo(88);
        assertThat(item.matchedSkills()).containsExactly("Java", "Spring");
        assertThat(item.highlights()).containsExactly("대규모 트래픽 처리 경험");
        assertThat(item.llmReason()).isEqualTo("추천 사유입니다.");
        assertThat(item.llmStatusLabel()).isEqualTo(LlmStatus.READY.getDescription());
        assertThat(item.llmReady()).isTrue();
        assertThat(item.matchingStatus()).isNull();  // 매칭 없음 → null

        verify(recommendationRunRepository).findDetailById(RUN_ID);
        verify(recommendationResultRepository).findByRunIdWithResume(RUN_ID);
        verify(matchingRepository).getLatestMatchingStatusMap(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection()
        );
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("매칭이 PROPOSED 상태면 후보의 matchingStatus는 'PROPOSED'이다")
    void getRecommendationResults_setsMatchingStatusProposedWhenMatchingExists() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        long resumeId = 500L;

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn("테스터");

        Resume candidateResume = org.mockito.Mockito.mock(Resume.class);
        given(candidateResume.getId()).willReturn(resumeId);
        given(candidateResume.getMember()).willReturn(candidateMember);
        given(candidateResume.getIntroduction()).willReturn(null);
        given(candidateResume.getPreferredWorkType()).willReturn(WorkType.REMOTE);

        RecommendationResult result = RecommendationResult.create(
                run, candidateResume, 1,
                new BigDecimal("0.9000"), new BigDecimal("0.8000"),
                new ReasonFacts(List.of(), List.of(), 3, List.of())
        );

        given(recommendationResultRepository.findByRunIdWithResume(RUN_ID)).willReturn(List.of(result));

        given(matchingRepository.getLatestMatchingStatusMap(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection()
        )).willReturn(Map.of(resumeId, MatchingStatus.PROPOSED));

        // when
        RecommendationResultsViewModel view = service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertThat(view.candidates()).hasSize(1);
        assertThat(view.candidates().get(0).matchingStatus()).isEqualTo("PROPOSED");
    }

    @Test
    @DisplayName("매칭이 ACCEPTED 상태면 후보의 matchingStatus는 'ACCEPTED'이다")
    void getRecommendationResults_setsMatchingStatusAcceptedWhenMatchingIsAccepted() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);
        given(recommendationRunRepository.findDetailById(RUN_ID)).willReturn(Optional.of(run));

        long resumeId = 501L;

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn("수락자");

        Resume candidateResume = org.mockito.Mockito.mock(Resume.class);
        given(candidateResume.getId()).willReturn(resumeId);
        given(candidateResume.getMember()).willReturn(candidateMember);
        given(candidateResume.getIntroduction()).willReturn(null);
        given(candidateResume.getPreferredWorkType()).willReturn(WorkType.SITE);

        RecommendationResult result = RecommendationResult.create(
                run, candidateResume, 1,
                new BigDecimal("0.8500"), new BigDecimal("0.7500"),
                new ReasonFacts(List.of(), List.of(), 2, List.of())
        );

        given(recommendationResultRepository.findByRunIdWithResume(RUN_ID)).willReturn(List.of(result));

        given(matchingRepository.getLatestMatchingStatusMap(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyCollection()
        )).willReturn(Map.of(resumeId, MatchingStatus.ACCEPTED));

        // when
        RecommendationResultsViewModel view = service.getRecommendationResults(PROPOSAL_ID, RUN_ID, OWNER_EMAIL);

        // then
        assertThat(view.candidates()).hasSize(1);
        assertThat(view.candidates().get(0).matchingStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    @DisplayName("COMPUTED 상태면 추천 후보 이력서 상세 ViewModel을 조립하여 반환한다")
    void getRecommendationCandidateResume_returnsDetailViewModel() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.COMPUTED);

        Member candidateMember = org.mockito.Mockito.mock(Member.class);
        given(candidateMember.getNickname()).willReturn(null);
        given(candidateMember.getName()).willReturn("홍길동");

        Resume candidateResume = org.mockito.Mockito.mock(Resume.class);
        given(candidateResume.getMember()).willReturn(candidateMember);
        given(candidateResume.getIntroduction()).willReturn("소개");
        given(candidateResume.getPreferredWorkType()).willReturn(WorkType.HYBRID);
        given(candidateResume.getPortfolioUrl()).willReturn("https://example.com");

        SortedSet<ResumeSkill> skills = new TreeSet<>(Comparator.comparing(rs -> rs.getSkill().getName()));
        skills.add(ResumeSkill.create(candidateResume, Skill.create("Java", null), Proficiency.ADVANCED));
        skills.add(ResumeSkill.create(candidateResume, Skill.create("Spring", null), Proficiency.INTERMEDIATE));
        given(candidateResume.getSkills()).willReturn(skills);

        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("ACME");
        item.setPosition("Backend");
        item.setEmploymentType(com.generic4.itda.domain.resume.CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth("2025-12");
        item.setCurrentlyWorking(false);
        item.setSummary("요약");
        item.setTechStack(List.of("Java", "Spring"));

        CareerPayload career = new CareerPayload();
        career.setItems(List.of(item));
        given(candidateResume.getCareer()).willReturn(career);

        ReasonFacts facts = new ReasonFacts(
                List.of("Java", "Spring"),
                List.of(),
                7,
                List.of("강점")
        );

        RecommendationResult result = RecommendationResult.create(
                run,
                candidateResume,
                1,
                new BigDecimal("0.9000"),
                new BigDecimal("0.8000"),
                facts
        );
        ReflectionTestUtils.setField(result, "id", RESULT_ID);
        result.markLlmReady("사유");

        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(result));
        given(matchingRepository.getLatestMatchingStatus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(Optional.empty());

        // when
        RecommendationResumeDetailViewModel view = service.getRecommendationCandidateResume(PROPOSAL_ID, RESULT_ID, OWNER_EMAIL);

        // then
        assertThat(view.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(view.runId()).isEqualTo(RUN_ID);
        assertThat(view.resultId()).isEqualTo(RESULT_ID);
        assertThat(view.backUrl()).isEqualTo("/proposals/10/recommendations/results?runId=301");
        assertThat(view.candidate().maskedName()).isEqualTo("홍*동");
        assertThat(view.candidate().matchingStatus()).isNull();  // 매칭 없음 → null
        assertThat(view.resumeSkills()).extracting("name").contains("Java", "Spring");
        assertThat(view.careerItems()).hasSize(1);
        assertThat(view.careerItems().get(0).companyName()).isEqualTo("ACME");

        verify(recommendationResultRepository).findDetailById(RESULT_ID);
        verify(matchingRepository).getLatestMatchingStatus(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    @Test
    @DisplayName("COMPUTED 상태가 아니면 추천 후보 이력서 상세 조회 시 IllegalStateException이 발생한다")
    void getRecommendationCandidateResume_throwsWhenNotComputed() {
        // given
        RecommendationRun run = createOwnedRunWithStatus(RecommendationRunStatus.RUNNING);

        Resume resume = org.mockito.Mockito.mock(Resume.class);

        RecommendationResult result = RecommendationResult.create(
                run,
                resume,
                1,
                new BigDecimal("0.5000"),
                new BigDecimal("0.5000"),
                new ReasonFacts(List.of(), List.of(), 1, List.of())
        );
        ReflectionTestUtils.setField(result, "id", RESULT_ID);

        given(recommendationResultRepository.findDetailById(RESULT_ID)).willReturn(Optional.of(result));

        // when / then
        assertThatThrownBy(() -> service.getRecommendationCandidateResume(PROPOSAL_ID, RESULT_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("추천 결과가 아직 준비되지 않았습니다.");

        verify(recommendationResultRepository).findDetailById(RESULT_ID);
        verifyNoMoreInteractions(recommendationRunRepository);
        verifyNoMoreInteractions(recommendationResultRepository);
    }

    private void assertViewModel(
            RecommendationRunStatusViewModel viewModel,
            RecommendationRunStatus status,
            String title,
            String message,
            String nextActionLabel,
            String nextActionUrl,
            boolean autoRefresh
    ) {
        assertThat(viewModel.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(viewModel.runId()).isEqualTo(RUN_ID);
        assertThat(viewModel.proposalTitle()).isEqualTo("추천 테스트 제안서");
        assertThat(viewModel.status()).isEqualTo(status);
        assertThat(viewModel.title()).isEqualTo(title);
        assertThat(viewModel.message()).isEqualTo(message);
        assertThat(viewModel.nextActionLabel()).isEqualTo(nextActionLabel);
        assertThat(viewModel.nextActionUrl()).isEqualTo(nextActionUrl);
        assertThat(viewModel.autoRefresh()).isEqualTo(autoRefresh);
    }

    private RecommendationRun createOwnedRunWithStatus(RecommendationRunStatus status) {
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposal(owner, PROPOSAL_ID);
        ProposalPosition proposalPosition = addPosition(proposal, 21L, "백엔드 개발자");

        RecommendationRun run = RecommendationRun.create(
                proposalPosition,
                "fp-abc123",
                RecommendationAlgorithm.HEURISTIC_V1,
                3
        );
        ReflectionTestUtils.setField(run, "id", RUN_ID);

        applyStatus(run, status);
        return run;
    }

    private void applyStatus(RecommendationRun run, RecommendationRunStatus status) {
        switch (status) {
            case PENDING -> {
                return;
            }
            case RUNNING -> run.markRunning();
            case COMPUTED -> {
                run.markRunning();
                run.markCompleted(new HardFilterStat(12, 3));
            }
            case FAILED -> {
                run.markRunning();
                run.markFailed("추천 생성 실패");
            }
        }
    }

    private Proposal createProposal(Member member, Long proposalId) {
        Proposal proposal = Proposal.create(
                member,
                "추천 테스트 제안서",
                "raw-input",
                "description",
                3_000_000L,
                5_000_000L,
                3L
        );
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        ReflectionTestUtils.setField(proposal, "status", ProposalStatus.MATCHING);
        return proposal;
    }

    private ProposalPosition addPosition(Proposal proposal, Long proposalPositionId, String positionName) {
        Position position = Position.create(positionName);
        ReflectionTestUtils.setField(position, "id", proposalPositionId + 1000);

        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                positionName,
                null,
                1L,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(proposalPosition, "id", proposalPositionId);
        return proposalPosition;
    }

    private Member createMemberWithId(Long memberId, String email) {
        Member member = Member.create(
                email,
                "hashed-password",
                "테스터",
                null,
                null,
                "010-1234-5678"
        );
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }
}
