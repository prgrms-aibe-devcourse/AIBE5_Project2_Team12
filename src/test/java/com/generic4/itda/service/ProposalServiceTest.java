package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiInterviewDraftSaveRequest;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.proposal.ProposalPositionForm;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.exception.UnresolvedSkillException;
import com.generic4.itda.repository.MatchingRepository;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalAiInterviewMessageRepository;
import com.generic4.itda.repository.ProposalPositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    private static final String EMAIL = "client@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @InjectMocks
    private ProposalService proposalService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private PositionResolver positionResolver;

    @Mock
    private SkillResolver skillResolver;

    @Mock
    private MatchingRepository matchingRepository;

    @Mock
    private ProposalAiInterviewMessageRepository aiInterviewMessageRepository;

    @Mock
    private ProposalPositionRepository proposalPositionRepository;

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
        lenient().when(matchingRepository.findByProposalPosition_IdAndClientMember_Email_Value(any(Long.class), any()))
                .thenReturn(List.of());
        lenient().when(proposalRepository.findFirstBySourceProposal_IdAndStatusOrderByModifiedAtDescIdDesc(any(Long.class), any()))
                .thenReturn(Optional.empty());
        lenient().when(positionResolver.isAllowedPosition(any(Position.class))).thenReturn(true);
        lenient().when(aiInterviewMessageRepository.findAllByProposalIdOrderBySequenceAsc(any(Long.class)))
                .thenReturn(List.of());
        lenient().when(aiInterviewMessageRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
    @DisplayName("같은 직무 category라도 title이 다르면 여러 모집 단위를 함께 저장한다")
    void saveDraft_keepsMultiplePositionsWithSamePositionIdAndDifferentTitles() {
        Position backend = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(backend, "id", 1L);

        ProposalPositionForm paymentBackendForm = new ProposalPositionForm();
        paymentBackendForm.setPositionId(1L);
        paymentBackendForm.setTitle("결제 서버 백엔드 개발자");
        paymentBackendForm.setWorkType(ProposalWorkType.REMOTE);
        paymentBackendForm.setHeadCount(1L);
        paymentBackendForm.setUnitBudgetMin(3_000_000L);
        paymentBackendForm.setUnitBudgetMax(4_000_000L);
        paymentBackendForm.setExpectedPeriod(4L);

        ProposalPositionForm settlementBackendForm = new ProposalPositionForm();
        settlementBackendForm.setPositionId(1L);
        settlementBackendForm.setTitle("정산 서버 백엔드 개발자");
        settlementBackendForm.setWorkType(ProposalWorkType.REMOTE);
        settlementBackendForm.setHeadCount(1L);
        settlementBackendForm.setUnitBudgetMin(3_500_000L);
        settlementBackendForm.setUnitBudgetMax(4_500_000L);
        settlementBackendForm.setExpectedPeriod(4L);

        ProposalForm form = new ProposalForm();
        form.setTitle("커머스 백엔드 개발");
        form.setRawInputText("");
        form.setExpectedPeriod(8L);
        form.getPositions().add(paymentBackendForm);
        form.getPositions().add(settlementBackendForm);

        when(positionRepository.findById(1L)).thenReturn(Optional.of(backend));

        Proposal proposal = proposalService.saveDraft(EMAIL, form);

        List<String> titles = proposal.getPositions().stream()
                .map(ProposalPosition::getTitle)
                .toList();

        assertThat(titles).hasSize(2);
        assertThat(titles).containsExactly("결제 서버 백엔드 개발자", "정산 서버 백엔드 개발자");
    }

    @Test
    @DisplayName("제안서 수정은 proposalPositionId가 있는 모집 단위만 갱신하고 같은 positionId 신규 row는 새로 추가한다")
    void saveDraft_updatesExistingPositionByProposalPositionIdAndAddsNewSamePositionIdRow() {
        Proposal proposal = Proposal.create(
                member,
                "기존 프로젝트",
                "기존 원본 입력",
                "기존 설명",
                null,
                null,
                8L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);

        Position backend = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(backend, "id", 1L);

        ProposalPosition existingPaymentBackend = proposal.addPosition(
                backend,
                "기존 결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                2_000_000L,
                3_000_000L,
                4L,
                2,
                5,
                null
        );
        ReflectionTestUtils.setField(existingPaymentBackend, "id", 10L);

        ProposalPosition existingSettlementBackend = proposal.addPosition(
                backend,
                "기존 정산 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                2_500_000L,
                3_500_000L,
                4L,
                2,
                5,
                null
        );
        ReflectionTestUtils.setField(existingSettlementBackend, "id", 11L);

        ProposalPositionForm updatedPaymentBackendForm = new ProposalPositionForm();
        updatedPaymentBackendForm.setId(10L);
        updatedPaymentBackendForm.setPositionId(1L);
        updatedPaymentBackendForm.setTitle("수정된 결제 서버 백엔드 개발자");
        updatedPaymentBackendForm.setWorkType(ProposalWorkType.REMOTE);
        updatedPaymentBackendForm.setHeadCount(2L);
        updatedPaymentBackendForm.setUnitBudgetMin(3_000_000L);
        updatedPaymentBackendForm.setUnitBudgetMax(4_000_000L);
        updatedPaymentBackendForm.setExpectedPeriod(5L);

        ProposalPositionForm searchBackendForm = new ProposalPositionForm();
        searchBackendForm.setPositionId(1L);
        searchBackendForm.setTitle("검색 서버 백엔드 개발자");
        searchBackendForm.setWorkType(ProposalWorkType.REMOTE);
        searchBackendForm.setHeadCount(1L);
        searchBackendForm.setUnitBudgetMin(3_500_000L);
        searchBackendForm.setUnitBudgetMax(4_500_000L);
        searchBackendForm.setExpectedPeriod(4L);

        ProposalForm form = new ProposalForm();
        form.setTitle("수정된 프로젝트");
        form.setRawInputText("수정된 원본 입력");
        form.setDescription("수정된 설명");
        form.setExpectedPeriod(8L);
        form.getPositions().add(updatedPaymentBackendForm);
        form.getPositions().add(searchBackendForm);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(backend));

        Proposal saved = proposalService.saveDraft(1L, EMAIL, form);

        List<String> titles = saved.getPositions().stream()
                .map(ProposalPosition::getTitle)
                .toList();
        Long updatedPaymentBackendId = saved.getPositions().stream()
                .filter(position -> "수정된 결제 서버 백엔드 개발자".equals(position.getTitle()))
                .map(ProposalPosition::getId)
                .findFirst()
                .orElse(null);

        assertThat(titles).hasSize(2);
        assertThat(titles).containsExactly("수정된 결제 서버 백엔드 개발자", "검색 서버 백엔드 개발자");
        assertThat(updatedPaymentBackendId).isEqualTo(10L);
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
    @DisplayName("AI 인터뷰 draft 저장은 rawInputText를 유지하고 현재 draft 필드를 저장한다")
    void saveInterviewDraft_keepsRawInputTextAndUpdatesDraftFields() {
        Proposal proposal = Proposal.create(
                member,
                "기존 프로젝트",
                "기존 원본 입력",
                "기존 설명",
                null,
                null,
                6L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);

        Position backend = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(backend, "id", 1L);

        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);
        Skill aws = Skill.create("AWS", null);

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(1L);
        positionForm.setTitle("Spring 백엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(2L);
        positionForm.setUnitBudgetMin(3_000_000L);
        positionForm.setUnitBudgetMax(4_000_000L);
        positionForm.setExpectedPeriod(4L);
        positionForm.setCareerMinYears(2);
        positionForm.setCareerMaxYears(5);
        positionForm.setEssentialSkillNames(List.of("Java", "Spring Boot"));
        positionForm.setPreferredSkillNames(List.of("AWS"));

        AiInterviewDraftSaveRequest request = new AiInterviewDraftSaveRequest();
        request.setTitle("수정된 프로젝트");
        request.setDescription("수정된 설명");
        request.setExpectedPeriod(8L);
        request.setPositions(List.of(positionForm));

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(backend));
        when(skillResolver.resolveRequired("Java")).thenReturn(java);
        when(skillResolver.resolveRequired("Spring Boot")).thenReturn(springBoot);
        when(skillResolver.resolveRequired("AWS")).thenReturn(aws);

        Proposal saved = proposalService.saveInterviewDraft(1L, EMAIL, request);

        assertThat(saved).isSameAs(proposal);
        assertThat(saved.getTitle()).isEqualTo("수정된 프로젝트");
        assertThat(saved.getRawInputText()).isEqualTo("기존 원본 입력");
        assertThat(saved.getDescription()).isEqualTo("수정된 설명");
        assertThat(saved.getExpectedPeriod()).isEqualTo(8L);
        assertThat(saved.getTotalBudgetMin()).isEqualTo(6_000_000L);
        assertThat(saved.getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(saved.getPositions()).hasSize(1);
        assertThat(saved.getPositions().get(0).getTitle()).isEqualTo("Spring 백엔드 개발자");
        assertThat(saved.getPositions().get(0).getSkills()).hasSize(3);
    }

    @Test
    @DisplayName("제안서 폼 스킬 문자열은 SkillResolver로 정규 Skill에 매핑해 저장한다")
    void saveDraft_resolvesSkillAliasToCanonicalSkill() {
        Position frontend = Position.create("프론트엔드 개발자");
        ReflectionTestUtils.setField(frontend, "id", 1L);
        Skill react = Skill.create("React", null);

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(1L);
        positionForm.setTitle("React 프론트엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(1L);
        positionForm.setEssentialSkillNames(List.of("React.js"));

        ProposalForm form = new ProposalForm();
        form.setTitle("프론트엔드 프로젝트");
        form.setRawInputText("");
        form.getPositions().add(positionForm);

        when(positionRepository.findById(1L)).thenReturn(Optional.of(frontend));
        when(skillResolver.resolveRequired("React.js")).thenReturn(react);

        Proposal proposal = proposalService.saveDraft(EMAIL, form);

        ProposalPosition proposalPosition = proposal.getPositions().get(0);
        assertThat(proposalPosition.getSkills()).hasSize(1);
        assertThat(proposalPosition.getSkills().get(0).getSkill()).isSameAs(react);
        assertThat(proposalPosition.getSkills().get(0).getSkill().getName()).isEqualTo("React");
        assertThat(proposalPosition.getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.ESSENTIAL);
    }

    @Test
    @DisplayName("제안서 폼 스킬이 기존 Skill에 매핑되지 않으면 저장에 실패한다")
    void saveDraft_throwsWhenSkillCannotBeResolved() {
        Position backend = Position.create("백엔드 개발자");
        ReflectionTestUtils.setField(backend, "id", 1L);

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(1L);
        positionForm.setTitle("백엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(1L);
        positionForm.setEssentialSkillNames(List.of("Unknown Skill"));

        ProposalForm form = new ProposalForm();
        form.setTitle("백엔드 프로젝트");
        form.setRawInputText("");
        form.getPositions().add(positionForm);

        when(positionRepository.findById(1L)).thenReturn(Optional.of(backend));
        when(skillResolver.resolveRequired("Unknown Skill"))
                .thenThrow(new UnresolvedSkillException("등록되지 않은 스킬입니다: Unknown Skill"));

        assertThatThrownBy(() -> proposalService.saveDraft(EMAIL, form))
                .isInstanceOf(UnresolvedSkillException.class)
                .hasMessage("등록되지 않은 스킬입니다: Unknown Skill");

        then(proposalRepository).should(never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("제안서 저장은 허용 목록 밖 Position id가 와도 저장하지 않는다")
    void saveDraft_throwsWhenPositionIsNotAllowed() {
        Position legacyBackend = Position.create("백엔드");
        ReflectionTestUtils.setField(legacyBackend, "id", 99L);

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(99L);
        positionForm.setTitle("백엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(1L);

        ProposalForm form = new ProposalForm();
        form.setTitle("백엔드 프로젝트");
        form.setRawInputText("");
        form.getPositions().add(positionForm);

        when(positionRepository.findById(99L)).thenReturn(Optional.of(legacyBackend));
        when(positionResolver.isAllowedPosition(legacyBackend)).thenReturn(false);

        assertThatThrownBy(() -> proposalService.saveDraft(EMAIL, form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 직무입니다. id=99");

        then(proposalRepository).should(never()).save(any(Proposal.class));
    }

    @Test
    @DisplayName("AI 인터뷰 draft 저장은 WRITING 상태가 아니면 실패한다")
    void saveInterviewDraft_blocksNonWritingProposal() {
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

        AiInterviewDraftSaveRequest request = new AiInterviewDraftSaveRequest();
        request.setTitle("수정된 프로젝트");
        request.setDescription("수정된 설명");
        request.setExpectedPeriod(6L);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.saveInterviewDraft(1L, EMAIL, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("매칭 중 제안서는 원본을 직접 수정할 수 없습니다. 수정 시작을 통해 새 초안을 만든 뒤 편집해주세요.");
    }

    @Test
    @DisplayName("AI 인터뷰 draft 저장은 본인 제안서가 아니면 실패한다")
    void saveInterviewDraft_blocksOtherMemberProposal() {
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

        AiInterviewDraftSaveRequest request = new AiInterviewDraftSaveRequest();
        request.setTitle("수정된 프로젝트");
        request.setDescription("수정된 설명");
        request.setExpectedPeriod(6L);

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.saveInterviewDraft(1L, OTHER_EMAIL, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("AI 인터뷰 draft 저장은 존재하지 않는 제안서면 실패한다")
    void saveInterviewDraft_throwsWhenNotFound() {
        AiInterviewDraftSaveRequest request = new AiInterviewDraftSaveRequest();
        request.setTitle("수정된 프로젝트");
        request.setDescription("수정된 설명");
        request.setExpectedPeriod(6L);

        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.saveInterviewDraft(99L, EMAIL, request))
                .isInstanceOf(ProposalNotFoundException.class);
    }

    @Test
    @DisplayName("AI 인터뷰 draft 저장도 허용 목록 밖 Position id를 거부한다")
    void saveInterviewDraft_throwsWhenPositionIsNotAllowed() {
        Proposal proposal = Proposal.create(
                member,
                "기존 프로젝트",
                "기존 원본 입력",
                "기존 설명",
                null,
                null,
                6L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);

        Position legacyBackend = Position.create("백엔드");
        ReflectionTestUtils.setField(legacyBackend, "id", 99L);

        ProposalPositionForm positionForm = new ProposalPositionForm();
        positionForm.setPositionId(99L);
        positionForm.setTitle("백엔드 개발자");
        positionForm.setWorkType(ProposalWorkType.REMOTE);
        positionForm.setHeadCount(1L);

        AiInterviewDraftSaveRequest request = new AiInterviewDraftSaveRequest();
        request.setTitle("수정된 프로젝트");
        request.setDescription("수정된 설명");
        request.setExpectedPeriod(6L);
        request.setPositions(List.of(positionForm));

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(positionRepository.findById(99L)).thenReturn(Optional.of(legacyBackend));
        when(positionResolver.isAllowedPosition(legacyBackend)).thenReturn(false);

        assertThatThrownBy(() -> proposalService.saveInterviewDraft(1L, EMAIL, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 직무입니다. id=99");
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
    @DisplayName("MATCHING 제안서를 수정 초안으로 복제할 때 AI 인터뷰 메시지도 함께 복제한다")
    void createEditDraft_copiesAiInterviewMessages() {
        Proposal source = Proposal.create(member, "기존 프로젝트", "원본", "설명", 3_000_000L, 4_000_000L, 8L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.startMatching();

        ProposalAiInterviewMessage userMessage = ProposalAiInterviewMessage.createUserMessage(
                source,
                "온라인 쇼핑몰 웹사이트를 만들고 싶어요.",
                1
        );
        ProposalAiInterviewMessage assistantMessage = ProposalAiInterviewMessage.createAssistantMessage(
                source,
                "필요한 포지션을 알려주세요.",
                2
        );

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(source));
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(source));
        when(proposalRepository.findPositionsWithSkillsByProposalId(1L)).thenReturn(source.getPositions());
        when(aiInterviewMessageRepository.findAllByProposalIdOrderBySequenceAsc(1L))
                .thenReturn(List.of(userMessage, assistantMessage));

        Proposal copied = proposalService.createEditDraft(1L, EMAIL);

        ArgumentCaptor<List<ProposalAiInterviewMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiInterviewMessageRepository).saveAll(captor.capture());

        List<ProposalAiInterviewMessage> copiedMessages = captor.getValue();
        assertThat(copiedMessages).hasSize(2);
        assertThat(copiedMessages.get(0).getProposal()).isSameAs(copied);
        assertThat(copiedMessages.get(0).getRole()).isEqualTo(userMessage.getRole());
        assertThat(copiedMessages.get(0).getContent()).isEqualTo(userMessage.getContent());
        assertThat(copiedMessages.get(0).getSequence()).isEqualTo(userMessage.getSequence());
        assertThat(copiedMessages.get(1).getProposal()).isSameAs(copied);
        assertThat(copiedMessages.get(1).getRole()).isEqualTo(assistantMessage.getRole());
        assertThat(copiedMessages.get(1).getContent()).isEqualTo(assistantMessage.getContent());
        assertThat(copiedMessages.get(1).getSequence()).isEqualTo(assistantMessage.getSequence());
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

    @Test
    @DisplayName("WRITING 상태 제안서는 정상적으로 삭제된다")
    void delete_writingProposal_succeeds() {
        Proposal proposal = Proposal.create(member, "삭제할 프로젝트", "", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        proposalService.delete(1L, EMAIL);

        then(proposalRepository).should().delete(proposal);
    }

    @Test
    @DisplayName("WRITING 상태가 아닌 제안서는 삭제할 수 없다")
    void delete_nonWritingProposal_throws() {
        Proposal proposal = Proposal.create(member, "매칭 중 프로젝트", "", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.delete(1L, EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("작성 중인 제안서만 삭제할 수 있습니다.");
    }

    @Test
    @DisplayName("본인 제안서가 아니면 삭제할 수 없다")
    void delete_othersProposal_throws() {
        Proposal proposal = Proposal.create(member, "다른 사람 프로젝트", "", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.delete(1L, OTHER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
        then(proposalRepository).should().findById(1L);
        then(proposalRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 제안서는 삭제할 수 없다")
    void delete_throws_whenNotFound() {
        when(proposalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.delete(99L, EMAIL))
                .isInstanceOf(ProposalNotFoundException.class);
        then(proposalRepository).should().findById(99L);
        then(proposalRepository).should(never()).delete(any(Proposal.class));
    }

    @Test
    @DisplayName("소유자는 findOwnedProposal로 제안서를 조회할 수 있다")
    void findOwnedProposal_returnsProposal_whenOwner() {
        Proposal proposal = Proposal.create(member, "내 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(proposal));

        Proposal result = proposalService.findOwnedProposal(1L, EMAIL);

        assertThat(result).isSameAs(proposal);
        then(proposalRepository).should().findWithPositionsById(1L);
        then(proposalRepository).should().findPositionsWithSkillsByProposalId(1L);
    }

    @Test
    @DisplayName("존재하지 않는 제안서는 findOwnedProposal에서 예외가 발생한다")
    void findOwnedProposal_throws_whenNotFound() {
        when(proposalRepository.findWithPositionsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.findOwnedProposal(99L, EMAIL))
                .isInstanceOf(ProposalNotFoundException.class);
        then(proposalRepository).should().findWithPositionsById(99L);
        then(proposalRepository).should(never()).findPositionsWithSkillsByProposalId(99L);
    }

    @Test
    @DisplayName("타인 제안서는 findOwnedProposal에서 접근 거부 예외가 발생한다")
    void findOwnedProposal_throws_whenNotOwner() {
        Proposal proposal = Proposal.create(member, "내 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> proposalService.findOwnedProposal(1L, OTHER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
        then(proposalRepository).should().findWithPositionsById(1L);
        then(proposalRepository).should(never()).findPositionsWithSkillsByProposalId(1L);
    }

    @Test
    @DisplayName("매칭 이력이 있는 프리랜서는 findProposalForFreelancer로 제안서를 조회할 수 있다")
    void findProposalForFreelancer_returnsProposal_whenMatchingExists() {
        Proposal proposal = Proposal.create(member, "클라이언트 프로젝트", "원본", "설명", null, null, 6L);
        ReflectionTestUtils.setField(proposal, "id", 1L);
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(1L, OTHER_EMAIL))
                .thenReturn(true);
        when(proposalRepository.findWithPositionsById(1L)).thenReturn(Optional.of(proposal));

        Proposal result = proposalService.findProposalForFreelancer(1L, OTHER_EMAIL);

        assertThat(result).isSameAs(proposal);
        then(proposalRepository).should().findWithPositionsById(1L);
        then(proposalRepository).should().findPositionsWithSkillsByProposalId(1L);
    }

    @Test
    @DisplayName("매칭 이력이 없는 프리랜서는 findProposalForFreelancer에서 접근 거부 예외가 발생한다")
    void findProposalForFreelancer_throws_whenNoMatchingHistory() {
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(1L, OTHER_EMAIL))
                .thenReturn(false);

        assertThatThrownBy(() -> proposalService.findProposalForFreelancer(1L, OTHER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
        then(proposalRepository).should(never()).findWithPositionsById(any());
    }

    @Test
    @DisplayName("매칭 이력은 있지만 존재하지 않는 제안서는 findProposalForFreelancer에서 예외가 발생한다")
    void findProposalForFreelancer_throws_whenProposalNotFound() {
        when(matchingRepository.existsByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(99L, OTHER_EMAIL))
                .thenReturn(true);
        when(proposalRepository.findWithPositionsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.findProposalForFreelancer(99L, OTHER_EMAIL))
                .isInstanceOf(ProposalNotFoundException.class);
    }

    @Test
    @DisplayName("MATCHING 상태 제안서의 OPEN 포지션을 소유자가 종료하면 CLOSED로 전환된다")
    void closePosition_closesOpenPosition() {
        Proposal proposal = createMatchingProposal(member);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        ProposalPosition result = proposalService.closePosition(1L, 10L, EMAIL);

        assertThat(result.getStatus()).isEqualTo(ProposalPositionStatus.CLOSED);
    }

    @Test
    @DisplayName("MATCHING 상태 제안서의 FULL 포지션을 소유자가 종료하면 CLOSED로 전환된다")
    void closePosition_closesFullPosition() {
        Proposal proposal = createMatchingProposal(member);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);
        position.changeStatus(ProposalPositionStatus.FULL);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        ProposalPosition result = proposalService.closePosition(1L, 10L, EMAIL);

        assertThat(result.getStatus()).isEqualTo(ProposalPositionStatus.CLOSED);
    }

    @Test
    @DisplayName("포지션 종료 시 해당 포지션의 PROPOSED 매칭은 종료 사유와 함께 REJECTED로 전환되고 ACCEPTED 매칭은 유지된다")
    void closePosition_rejectsPendingMatchingsOnly() {
        Proposal proposal = createMatchingProposal(member);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);

        Member freelancerA = createMember("freelancer-a@example.com", "hashed-password", "프리랜서A", "010-1111-1111");
        Member freelancerB = createMember("freelancer-b@example.com", "hashed-password", "프리랜서B", "010-2222-2222");
        Matching proposed = Matching.create(null, position, member, freelancerA);
        Matching accepted = Matching.create(null, position, member, freelancerB);
        accepted.accept();

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));
        when(matchingRepository.findByProposalPosition_IdAndClientMember_Email_Value(10L, EMAIL))
                .thenReturn(List.of(proposed, accepted));

        ProposalPosition result = proposalService.closePosition(1L, 10L, EMAIL);

        assertThat(result.getStatus()).isEqualTo(ProposalPositionStatus.CLOSED);
        assertThat(proposed.getStatus()).isEqualTo(MatchingStatus.REJECTED);
        assertThat(proposed.getRejectedAt()).isNotNull();
        assertThat(proposed.getCancellationReasonDetail()).isEqualTo("클라이언트가 모집을 종료했습니다.");
        assertThat(accepted.getStatus()).isEqualTo(MatchingStatus.ACCEPTED);
    }

    @Test
    @DisplayName("이미 CLOSED인 포지션을 종료하면 예외가 발생한다")
    void closePosition_throws_whenAlreadyClosed() {
        Proposal proposal = createMatchingProposal(member);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);
        position.changeStatus(ProposalPositionStatus.CLOSED);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> proposalService.closePosition(1L, 10L, EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 종료된 모집 포지션입니다.");
    }

    @Test
    @DisplayName("MATCHING이 아닌 제안서의 포지션을 종료하면 예외가 발생한다")
    void closePosition_throws_whenProposalNotMatching() {
        Proposal proposal = Proposal.create(
                member,
                "프로젝트",
                "",
                "설명",
                null,
                null,
                6L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> proposalService.closePosition(1L, 10L, EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모집/추천 진행 중인 제안서의 포지션만 종료할 수 있습니다.");
    }

    @Test
    @DisplayName("소유자가 아닌 사용자가 포지션을 종료하면 예외가 발생한다")
    void closePosition_throws_whenNotOwner() {
        Proposal proposal = createMatchingProposal(member);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> proposalService.closePosition(1L, 10L, OTHER_EMAIL))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 포지션을 종료하면 예외가 발생한다")
    void closePosition_throws_whenPositionNotFound() {
        when(proposalPositionRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.closePosition(1L, 999L, EMAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("다른 제안서 경로의 포지션을 종료하면 예외가 발생한다")
    void closePosition_throws_whenPositionDoesNotBelongToProposal() {
        Proposal proposal = createMatchingProposal(member);
        ReflectionTestUtils.setField(proposal, "id", 2L);
        ProposalPosition position = proposal.addPosition(
                Position.create("백엔드"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                2L,
                1_000_000L,
                2_000_000L,
                4L,
                1,
                5,
                null
        );
        ReflectionTestUtils.setField(position, "id", 10L);

        when(proposalPositionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(position));

        assertThatThrownBy(() -> proposalService.closePosition(1L, 10L, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proposalId=1")
                .hasMessageContaining("positionId=10");
        then(matchingRepository).should(never()).findByProposalPosition_IdAndClientMember_Email_Value(any(), any());
    }

    private Proposal createMatchingProposal(Member owner) {
        Proposal proposal = Proposal.create(
                owner,
                "프로젝트",
                "",
                "설명",
                null,
                null,
                6L
        );
        ReflectionTestUtils.setField(proposal, "id", 1L);
        proposal.startMatching();
        return proposal;
    }
}