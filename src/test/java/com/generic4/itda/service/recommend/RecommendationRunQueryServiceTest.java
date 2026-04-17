package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import com.generic4.itda.dto.recommend.RecommendationRunStatusViewModel;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RecommendationRunQueryService 단위 테스트:
 * - repository가 준비한 RecommendationRun 상세 aggregate를 바탕으로 접근 검증과 상태별 ViewModel 조립을 수행하는지 확인한다.
 *
 * 한계:
 * - 이 테스트는 Mockito 기반 단위 테스트라 findDetailById의 fetch join 범위나 lazy 초기화는 검증하지 않는다.
 * - proposal/member/proposalPosition 그래프 preload 계약은 RecommendationRunRepositoryTest가 보장해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationRunQueryServiceTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long RUN_ID = 301L;
    private static final Long OWNER_ID = 1L;
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

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
                run.markCompleted(new HardFilterStat(12, 8, 5, 3));
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
                ProposalWorkType.REMOTE,
                "판교",
                3L
        );
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        ReflectionTestUtils.setField(proposal, "status", ProposalStatus.MATCHING);
        return proposal;
    }

    private ProposalPosition addPosition(Proposal proposal, Long proposalPositionId, String positionName) {
        Position position = Position.create(positionName);
        ReflectionTestUtils.setField(position, "id", proposalPositionId + 1000);

        ProposalPosition proposalPosition = proposal.addPosition(position, 1L, 1_000_000L, 2_000_000L);
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
