package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.proposal.ProposalPositionForm;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import com.generic4.itda.repository.SkillRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    private static final String EMAIL = "client@example.com";

    @InjectMocks
    private ProposalService proposalService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private MatchingRepository matchingRepository;

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = createMember(EMAIL, "hashed-password", "클라이언트", "010-1234-5678");

        lenient().when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        lenient().when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(matchingRepository.existsByProposalPosition_Proposal_Id(any(Long.class))).thenReturn(false);
        lenient().when(recommendationRunRepository.existsByProposalPosition_Proposal_Id(any(Long.class))).thenReturn(false);
        lenient().doNothing().when(recommendationResultRepository)
                .deleteAllByRecommendationRun_ProposalPosition_Proposal_Id(any(Long.class));
        lenient().doNothing().when(recommendationRunRepository).deleteAllByProposalPosition_Proposal_Id(any(Long.class));
    }

    @Test
    @DisplayName("임시저장은 rawInputText가 비어 있어도 WRITING 상태로 저장한다")
    void saveDraft_allowsBlankRawInput() {
        ProposalForm form = new ProposalForm();
        form.setTitle("새 프로젝트");
        form.setRawInputText(null);

        Proposal proposal = proposalService.saveDraft(EMAIL, form);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.WRITING);
        assertThat(proposal.getRawInputText()).isEmpty();
    }

    @Test
    @DisplayName("제안서 등록은 포지션 예산을 합산해 MATCHING 상태로 전환한다")
    void register_aggregatesBudgetAndStartsMatching() {
        Position backend = Position.create("백엔드 개발자");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(backend));

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(1L);
        positionForm.setTitle("Node.js 백엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(2L);
        positionForm.setUnitBudgetMin(3_000_000L);
        positionForm.setUnitBudgetMax(4_000_000L);
        positionForm.setExpectedPeriod(4L);

        ProposalForm form = new ProposalForm();
        form.setTitle("쇼핑몰 앱 개발");
        form.setRawInputText("");
        form.setExpectedPeriod(8L);
        form.getPositions().add(positionForm);

        Proposal proposal = proposalService.register(EMAIL, form);

        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.MATCHING);
        assertThat(proposal.getTotalBudgetMin()).isEqualTo(6_000_000L);
        assertThat(proposal.getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(proposal.getPositions()).hasSize(1);
    }

    @Test
    @DisplayName("MATCHING 상태 제안서를 임시저장하면 WRITING으로 되돌린다")
    void saveDraft_revertsMatchingProposalToWriting() {
        Proposal proposal = Proposal.create(
                member,
                "기존 프로젝트",
                "기존 원본 입력",
                "설명",
                null,
                null,
                6L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        ProposalForm form = new ProposalForm();
        form.setTitle("수정된 프로젝트");
        form.setRawInputText("");
        form.setExpectedPeriod(6L);

        Proposal updated = proposalService.saveDraft(1L, EMAIL, form);

        assertThat(updated.getStatus()).isEqualTo(ProposalStatus.WRITING);
        assertThat(updated.getTitle()).isEqualTo("수정된 프로젝트");
    }

    @Test
    @DisplayName("추천만 받은 MATCHING 제안서를 수정하면 WRITING으로 되돌리고 기존 추천 결과를 삭제한다")
    void prepareForEdit_revertsMatchingWithoutMatchingsAndClearsRecommendations() {
        Proposal proposal = Proposal.create(member, "기존 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(matchingRepository.existsByProposalPosition_Proposal_Id(1L)).thenReturn(false);
        when(recommendationRunRepository.existsByProposalPosition_Proposal_Id(1L)).thenReturn(true);

        Proposal editable = proposalService.prepareForEdit(1L, EMAIL);

        assertThat(editable).isSameAs(proposal);
        assertThat(editable.getStatus()).isEqualTo(ProposalStatus.WRITING);
        verify(recommendationResultRepository).deleteAllByRecommendationRun_ProposalPosition_Proposal_Id(1L);
        verify(recommendationRunRepository).deleteAllByProposalPosition_Proposal_Id(1L);
    }

    @Test
    @DisplayName("거절 또는 취소 매칭 이력만 있는 MATCHING 제안서를 수정하면 새 WRITING 초안을 복제한다")
    void prepareForEdit_clonesProposalWhenOnlyRejectedOrCanceledMatchingsExist() {
        Proposal source = Proposal.create(member, "기존 프로젝트", "원본", "설명", 3_000_000L, 4_000_000L, 8L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.startMatching();
        Position backend = Position.create("백엔드 개발자");
        source.addPosition(backend, "Node.js 백엔드 개발자", ProposalWorkType.REMOTE, 1L,
                3_000_000L, 4_000_000L, 4L, 3, 5, null);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(matchingRepository.existsByProposalPosition_Proposal_Id(1L)).thenReturn(true);
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(source));
        when(proposalRepository.findPositionsWithSkillsByProposalId(1L)).thenReturn(source.getPositions());

        Proposal copied = proposalService.prepareForEdit(1L, EMAIL);

        assertThat(copied).isNotSameAs(source);
        assertThat(copied.getStatus()).isEqualTo(ProposalStatus.WRITING);
        assertThat(copied.getTitle()).isEqualTo(source.getTitle());
        assertThat(copied.getPositions()).hasSize(1);
        assertThat(copied.getPositions().get(0).getTitle()).isEqualTo("Node.js 백엔드 개발자");
        verify(recommendationResultRepository, never()).deleteAllByRecommendationRun_ProposalPosition_Proposal_Id(any(Long.class));
        verify(recommendationRunRepository, never()).deleteAllByProposalPosition_Proposal_Id(any(Long.class));
    }

    @Test
    @DisplayName("진행 중이거나 완료된 매칭 이력이 있으면 제안서를 수정할 수 없다")
    void prepareForEdit_blocksWhenBlockingMatchingExists() {
        Proposal proposal = Proposal.create(member, "기존 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(matchingRepository.existsByProposalPosition_Proposal_Id(1L)).thenReturn(true);
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(eq(1L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> proposalService.prepareForEdit(1L, EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중이거나 완료된 매칭이 있는 제안서는 수정할 수 없습니다.");
    }
}
