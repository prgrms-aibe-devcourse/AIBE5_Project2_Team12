package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.client.ClientDashboardFilter;
import com.generic4.itda.dto.client.ClientDashboardProjectItem;
import com.generic4.itda.dto.client.ClientDashboardSummaryItem;
import com.generic4.itda.dto.client.ClientDashboardViewModel;
import com.generic4.itda.repository.ProposalRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClientDashboardServiceTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private ProposalRepository proposalRepository;

    @InjectMocks
    private ClientDashboardService clientDashboardService;

    @Test
    @DisplayName("전체 필터 조회 시 상태별 카드 개수와 최근 제안서 목록을 함께 반환한다")
    void getDashboard_returnsSummariesAndProjectsForAllFilter() {
        Proposal matchingProposal = createProposal(
                1L,
                "핀테크 앱 고도화 프로젝트",
                ProposalStatus.MATCHING,
                4_500_000L,
                4_500_000L,
                LocalDateTime.of(2024, 3, 15, 10, 0),
                3
        );
        Proposal waitingProposal = createProposal(
                2L,
                "AI 기반 데이터 분석 툴 구축",
                ProposalStatus.WRITING,
                3_000_000L,
                3_000_000L,
                LocalDateTime.of(2024, 3, 20, 10, 0),
                2
        );

        given(proposalRepository.findAllByMember_Email_ValueOrderByModifiedAtDesc(OWNER_EMAIL))
                .willReturn(List.of(waitingProposal, matchingProposal));
        stubCounts(6L, 2L, 3L, 1L);

        ClientDashboardViewModel result = clientDashboardService.getDashboard(OWNER_EMAIL, ClientDashboardFilter.ALL);

        InOrder inOrder = inOrder(proposalRepository);
        inOrder.verify(proposalRepository).findAllByMember_Email_ValueOrderByModifiedAtDesc(OWNER_EMAIL);
        verifyCountQueries(inOrder);

        assertThat(result.selectedFilterKey()).isEqualTo("all");
        assertThat(result.selectedFilterTitle()).isEqualTo("전체 프로젝트");
        assertThat(result.summaries())
                .extracting(ClientDashboardSummaryItem::filterKey, ClientDashboardSummaryItem::count,
                        ClientDashboardSummaryItem::selected)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("all", 6L, true),
                        org.assertj.core.groups.Tuple.tuple("waiting", 2L, false),
                        org.assertj.core.groups.Tuple.tuple("matching", 3L, false),
                        org.assertj.core.groups.Tuple.tuple("complete", 1L, false)
                );

        assertThat(result.projects()).hasSize(2);
        ClientDashboardProjectItem firstProject = result.projects().get(0);
        assertThat(firstProject.proposalId()).isEqualTo(2L);
        assertThat(firstProject.title()).isEqualTo("AI 기반 데이터 분석 툴 구축");
        assertThat(firstProject.statusKey()).isEqualTo("WRITING");
        assertThat(firstProject.statusLabel()).isEqualTo("작성 중");
        assertThat(firstProject.positionCount()).isEqualTo(2);
        assertThat(firstProject.totalBudgetText()).isEqualTo("3,000,000원");
        assertThat(firstProject.modifiedDate()).isEqualTo("2024.03.20");
        assertThat(firstProject.matchingOverview()).isEqualTo("매칭 시작 전");

        verifyNoMoreInteractions(proposalRepository);
    }

    @Test
    @DisplayName("매칭 대기 필터 조회 시 WRITING 상태 제안서만 반환한다")
    void getDashboard_filtersProjectsByWaitingStatus() {
        Proposal waitingProposal = createProposal(
                2L,
                "AI 기반 데이터 분석 툴 구축",
                ProposalStatus.WRITING,
                3_000_000L,
                3_000_000L,
                LocalDateTime.of(2024, 3, 20, 10, 0),
                2
        );

        given(proposalRepository.findAllByMember_Email_ValueAndStatusOrderByModifiedAtDesc(
                OWNER_EMAIL,
                ProposalStatus.WRITING
        )).willReturn(List.of(waitingProposal));
        stubCounts(6L, 2L, 3L, 1L);

        ClientDashboardViewModel result = clientDashboardService.getDashboard(OWNER_EMAIL, ClientDashboardFilter.WAITING);

        InOrder inOrder = inOrder(proposalRepository);
        inOrder.verify(proposalRepository)
                .findAllByMember_Email_ValueAndStatusOrderByModifiedAtDesc(OWNER_EMAIL, ProposalStatus.WRITING);
        verifyCountQueries(inOrder);

        assertThat(result.selectedFilterKey()).isEqualTo("waiting");
        assertThat(result.selectedFilterTitle()).isEqualTo("매칭 대기 중");
        assertThat(result.projects()).singleElement()
                .satisfies(project -> {
                    assertThat(project.statusKey()).isEqualTo("WRITING");
                    assertThat(project.matchingOverview()).isEqualTo("매칭 시작 전");
                });

        verifyNoMoreInteractions(proposalRepository);
    }

    @Test
    @DisplayName("필터가 null이면 전체 필터로 조회한다")
    void getDashboard_defaultsToAllFilterWhenFilterIsNull() {
        given(proposalRepository.findAllByMember_Email_ValueOrderByModifiedAtDesc(OWNER_EMAIL))
                .willReturn(List.of());
        stubCounts(0L, 0L, 0L, 0L);

        ClientDashboardViewModel result = clientDashboardService.getDashboard(OWNER_EMAIL, null);

        InOrder inOrder = inOrder(proposalRepository);
        inOrder.verify(proposalRepository).findAllByMember_Email_ValueOrderByModifiedAtDesc(OWNER_EMAIL);
        verifyCountQueries(inOrder);

        assertThat(result.selectedFilterKey()).isEqualTo("all");
        assertThat(result.projects()).isEmpty();

        verifyNoMoreInteractions(proposalRepository);
    }

    private void stubCounts(long total, long waiting, long matching, long complete) {
        given(proposalRepository.countByMember_Email_Value(OWNER_EMAIL)).willReturn(total);
        given(proposalRepository.countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.WRITING))
                .willReturn(waiting);
        given(proposalRepository.countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.MATCHING))
                .willReturn(matching);
        given(proposalRepository.countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.COMPLETE))
                .willReturn(complete);
    }

    private void verifyCountQueries(InOrder inOrder) {
        inOrder.verify(proposalRepository).countByMember_Email_Value(OWNER_EMAIL);
        inOrder.verify(proposalRepository).countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.WRITING);
        inOrder.verify(proposalRepository).countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.MATCHING);
        inOrder.verify(proposalRepository).countByMember_Email_ValueAndStatus(OWNER_EMAIL, ProposalStatus.COMPLETE);
    }

    private Proposal createProposal(Long id, String title, ProposalStatus status, Long budgetMin, Long budgetMax,
            LocalDateTime modifiedAt, int positionCount) {
        Proposal proposal = Proposal.create(
                createMember(OWNER_EMAIL, "hashed-password", "소유자", "010-1234-5678"),
                title,
                "원본 입력",
                "설명",
                budgetMin,
                budgetMax,
                12L
        );

        ReflectionTestUtils.setField(proposal, "id", id);
        ReflectionTestUtils.setField(proposal, "status", status);
        ReflectionTestUtils.setField(proposal, "modifiedAt", modifiedAt);

        for (int index = 0; index < positionCount; index++) {
            String positionTitle = "포지션" + index;
            proposal.addPosition(Position.create(positionTitle), positionTitle, null, 1L, null, null,
                    null, null, null, null);
        }

        return proposal;
    }
}
