package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import com.generic4.itda.repository.SkillRepository;
import jakarta.persistence.EntityManager;
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
    private EntityManager entityManager;

    private Member member;

    @BeforeEach
    void setUp() {
        member = createMember(EMAIL, "hashed-password", "클라이언트", "010-1234-5678");

        lenient().when(memberRepository.findByEmail_Value(EMAIL)).thenReturn(member);
        lenient().when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(any(Long.class), any()))
                .thenReturn(false);
        lenient().when(proposalRepository.findFirstBySourceProposal_IdAndStatusOrderByModifiedAtDescIdDesc(any(Long.class), any()))
                .thenReturn(Optional.empty());
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
    @DisplayName("MATCHING 상태 원본 제안서는 직접 임시저장할 수 없다")
    void saveDraft_blocksDirectUpdateForMatchingProposal() {
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

        assertThatThrownBy(() -> proposalService.saveDraft(1L, EMAIL, form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("매칭 중 제안서는 원본을 직접 수정할 수 없습니다. 수정 시작을 통해 새 초안을 만든 뒤 편집해주세요.");
    }

    @Test
    @DisplayName("작성 중인 제안서는 edit draft 생성 없이 그대로 편집할 수 있다")
    void createEditDraft_returnsWritingProposalAsIs() {
        Proposal proposal = Proposal.create(member, "기존 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        Proposal editable = proposalService.createEditDraft(1L, EMAIL);

        assertThat(editable).isSameAs(proposal);
        assertThat(editable.getStatus()).isEqualTo(ProposalStatus.WRITING);
    }

    @Test
    @DisplayName("MATCHING 제안서를 수정 시작하면 원본 대신 새 WRITING 초안을 복제한다")
    void createEditDraft_clonesProposalWhenEditableMatchingExists() {
        Proposal source = Proposal.create(member, "기존 프로젝트", "원본", "설명", 3_000_000L, 4_000_000L, 8L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.startMatching();
        Position backend = Position.create("백엔드 개발자");
        source.addPosition(backend, "Node.js 백엔드 개발자", ProposalWorkType.REMOTE, 1L,
                3_000_000L, 4_000_000L, 4L, 3, 5, null);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(source));
        when(proposalRepository.findPositionsWithSkillsByProposalId(1L)).thenReturn(source.getPositions());

        Proposal copied = proposalService.createEditDraft(1L, EMAIL);

        assertThat(copied).isNotSameAs(source);
        assertThat(copied.getStatus()).isEqualTo(ProposalStatus.WRITING);
        assertThat(copied.getTitle()).isEqualTo(source.getTitle());
        assertThat(copied.getPositions()).hasSize(1);
        assertThat(copied.getPositions().get(0).getTitle()).isEqualTo("Node.js 백엔드 개발자");
    }

    @Test
    @DisplayName("같은 원본에서 이미 열린 WRITING draft가 있으면 그 draft를 재사용한다")
    void createEditDraft_reusesExistingWritingDraft() {
        Proposal source = Proposal.create(member, "기존 프로젝트", "원본", "설명", 3_000_000L, 4_000_000L, 8L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.startMatching();

        Proposal existingDraft = Proposal.createDraftCopy(
                source,
                member,
                source.getTitle(),
                source.getRawInputText(),
                source.getDescription(),
                source.getTotalBudgetMin(),
                source.getTotalBudgetMax(),
                source.getExpectedPeriod()
        );
        ReflectionTestUtils.setField(existingDraft, "id", 7L);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(proposalRepository.findFirstBySourceProposal_IdAndStatusOrderByModifiedAtDescIdDesc(1L, ProposalStatus.WRITING))
                .thenReturn(Optional.of(existingDraft));

        Proposal reused = proposalService.createEditDraft(1L, EMAIL);

        assertThat(reused).isSameAs(existingDraft);
    }

    @Test
    @DisplayName("진행 중이거나 완료된 매칭 이력이 있으면 제안서를 수정할 수 없다")
    void createEditDraft_blocksWhenBlockingMatchingExists() {
        Proposal proposal = Proposal.create(member, "기존 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(eq(1L), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> proposalService.createEditDraft(1L, EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("진행 중이거나 완료된 매칭이 있는 제안서는 수정할 수 없습니다.");
    }
}
