package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.recommend.RecommendationEntryPositionItem;
import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RecommendationEntryService 단위 테스트:
 * - service가 두 단계 조회(findWithPositionsById -> findPositionsWithSkillsByProposalId)를 명시적으로 수행하는지
 * - repository가 준비해 준 aggregate를 바탕으로 ViewModel/권한/상태 로직을 올바르게 조립하는지
 *
 * 한계:
 * - 이 테스트는 Mockito 기반 단위 테스트라서 실제 JPA fetch/lazy 계약을 검증하지 않는다.
 * - 특히 skills preload는 "repository가 필요한 그래프를 준비해 준다"는 전제만 검증하며,
 *   실제 1차 캐시 합성 및 lazy 초기화 여부는 ProposalRepositoryTest가 보장해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationEntryServiceTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final String OWNER_EMAIL = "owner@example.com";

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private RecommendationEntryService service;

    @Test
    @DisplayName("정상 조회 시 제안서 상세는 두 단계 조회로 조립되고 ViewModel도 올바르게 구성된다")
    void getEntry_assemblesViewModelFromExplicitTwoStepRepositoryLoad() {
        // given
        Member owner = createMemberWithId(OWNER_ID);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);

        ProposalPosition designer = addPosition(proposal, 30L, "UI/UX 디자이너", "프로덕트 디자이너", 1L, null, null, null);
        addSkill(designer, 201L, "Figma", ProposalPositionSkillImportance.PREFERENCE);

        ProposalPosition backend = addPosition(proposal, 10L, "백엔드 개발자", "Node.js 백엔드 개발자", 3L, 1_000_000L, 2_000_000L, 3L);
        addSkill(backend, 202L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        ProposalPosition frontend = addPosition(proposal, 20L, "프론트엔드 개발자", "프론트엔드 개발자", 2L, 700_000L, 900_000L, 2L);
        addSkill(frontend, 203L, "React", ProposalPositionSkillImportance.PREFERENCE);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when
        RecommendationEntryViewModel result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL);

        // then
        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        assertThat(result.proposalId()).isEqualTo(PROPOSAL_ID);
        assertThat(result.proposalTitle()).isEqualTo("테스트 제안서");
        assertThat(result.proposalStatus()).isEqualTo("모집/추천 진행 중");
        assertThat(result.runnable()).isTrue();
        assertThat(result.helperMessage()).isEqualTo("선택한 포지션 기준으로 추천을 시작할 수 있습니다.");
        assertThat(result.selectedProposalPositionId()).isEqualTo(10L);

        assertThat(result.positions())
                .extracting(RecommendationEntryPositionItem::proposalPositionId)
                .containsExactly(10L, 20L, 30L);

        RecommendationEntryPositionItem firstPosition = result.positions().get(0);
        assertThat(firstPosition.positionTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(firstPosition.positionCategoryName()).isEqualTo("백엔드 개발자");
        assertThat(firstPosition.headCount()).isEqualTo(3L);
        assertThat(firstPosition.budgetText()).isEqualTo("1,000,000 ~ 2,000,000");
        assertThat(firstPosition.expectedPeriod()).isEqualTo(3L);
        assertThat(firstPosition.skills())
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.skillName()).isEqualTo("Java");
                    assertThat(skill.importanceLabel()).isEqualTo("필수");
                });

        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    @Test
    @DisplayName("positions가 비어 있으면 selectedProposalPositionId는 null이고 positions도 빈 리스트다")
    void getEntry_returnsEmptyPositionsAndNullSelectionWhenProposalHasNoPositions() {
        // given
        Member owner = createMemberWithId(OWNER_ID);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when
        RecommendationEntryViewModel result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL);

        // then
        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        assertThat(result.positions()).isEmpty();
        assertThat(result.selectedProposalPositionId()).isNull();
        assertThat(result.runnable()).isTrue();

        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    @Test
    @DisplayName("MATCHING 상태이면 runnable=true다")
    void getEntry_runnableIsTrueWhenStatusIsMatching() {
        // given
        Proposal proposal = ownedProposalWithSinglePosition(ProposalStatus.MATCHING, null, null);

        // when
        RecommendationEntryViewModel result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL);

        // then
        assertThat(result.runnable()).isTrue();
        assertThat(result.helperMessage()).isEqualTo("선택한 포지션 기준으로 추천을 시작할 수 있습니다.");
        verifyExpectedInteractions();
    }

    @Test
    @DisplayName("WRITING 상태이면 runnable=false다")
    void getEntry_runnableIsFalseWhenStatusIsWriting() {
        // given
        ownedProposalWithSinglePosition(ProposalStatus.WRITING, null, null);

        // when
        RecommendationEntryViewModel result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL);

        // then
        assertThat(result.runnable()).isFalse();
        assertThat(result.helperMessage()).isEqualTo("제안서가 MATCHING 상태일 때만 추천을 실행할 수 있습니다.");
        verifyExpectedInteractions();
    }

    @Test
    @DisplayName("포지션 title이 비어 있으면 category 이름을 fallback으로 사용한다")
    void getEntry_usesCategoryNameWhenPositionTitleIsBlank() {
        // given
        Member owner = createMemberWithId(OWNER_ID);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        ProposalPosition position = addPosition(proposal, 100L, "백엔드 개발자", "백엔드 개발자", 1L, null, null, null);
        ReflectionTestUtils.setField(position, "title", "   ");

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when
        RecommendationEntryPositionItem result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL).positions().get(0);

        // then
        assertThat(result.positionTitle()).isEqualTo("백엔드 개발자");
        assertThat(result.positionCategoryName()).isEqualTo("백엔드 개발자");
        verifyExpectedInteractions();
    }

    @Test
    @DisplayName("COMPLETE 상태이면 runnable=false다")
    void getEntry_runnableIsFalseWhenStatusIsComplete() {
        // given
        ownedProposalWithSinglePosition(ProposalStatus.COMPLETE, null, null);

        // when
        RecommendationEntryViewModel result = service.getEntry(PROPOSAL_ID, OWNER_EMAIL);

        // then
        assertThat(result.runnable()).isFalse();
        assertThat(result.helperMessage()).isEqualTo("제안서가 MATCHING 상태일 때만 추천을 실행할 수 있습니다.");
        verifyExpectedInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 proposal이면 IllegalArgumentException이 발생한다")
    void getEntry_throwsWhenProposalNotFound() {
        // given
        given(proposalRepository.findWithPositionsById(999L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.getEntry(999L, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 제안서입니다.");

        verify(proposalRepository).findWithPositionsById(999L);
        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    @Test
    @DisplayName("존재하지 않는 member이면 IllegalArgumentException이 발생한다")
    void getEntry_throwsWhenMemberNotFound() {
        // given
        Member owner = createMemberWithId(OWNER_ID);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> service.getEntry(PROPOSAL_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    @Test
    @DisplayName("본인 제안서가 아니면 IllegalArgumentException이 발생한다")
    void getEntry_throwsWhenMemberIsNotOwner() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Member other = createMemberWithId(2L, "other@example.com");
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value("other@example.com")).willReturn(other);

        // when / then
        assertThatThrownBy(() -> service.getEntry(PROPOSAL_ID, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 제안서만 조회할 수 있습니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value("other@example.com");
        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    @Test
    @DisplayName("budget min/max 모두 null이면 '예산 미정'을 반환한다")
    void getEntry_budgetBothNull_returnsUndefined() {
        assertThat(getBudgetText(null, null)).isEqualTo("예산 미정");
    }

    @Test
    @DisplayName("budget min/max 모두 있으면 'min ~ max' 형식으로 반환한다")
    void getEntry_budgetBothPresent_returnsRange() {
        assertThat(getBudgetText(1_000_000L, 3_000_000L)).isEqualTo("1,000,000 ~ 3,000,000");
    }

    @Test
    @DisplayName("budget min만 있으면 '이상' 포함 문자열을 반환한다")
    void getEntry_budgetMinOnly_returnsAtLeast() {
        assertThat(getBudgetText(1_000_000L, null)).isEqualTo("1,000,000 이상");
    }

    @Test
    @DisplayName("budget max만 있으면 '이하' 포함 문자열을 반환한다")
    void getEntry_budgetMaxOnly_returnsAtMost() {
        assertThat(getBudgetText(null, 3_000_000L)).isEqualTo("3,000,000 이하");
    }

    private Proposal ownedProposalWithSinglePosition(ProposalStatus status, Long budgetMin, Long budgetMax) {
        Member owner = createMemberWithId(OWNER_ID);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, status);
        addPosition(proposal, 100L, "포지션", "포지션", 1L, budgetMin, budgetMax, null);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);
        return proposal;
    }

    /**
     * mock 기반 테스트에서는 두 번째 조회의 반환값보다 "서비스가 preload 쿼리를 명시 호출한다"는 사실이 중요하다.
     * 실제 1차 캐시 합성/fetch 계약은 ProposalRepositoryTest가 검증한다.
     */
    private void stubProposalDetailLoad(Proposal proposal) {
        given(proposalRepository.findWithPositionsById(proposal.getId())).willReturn(Optional.of(proposal));
        given(proposalRepository.findPositionsWithSkillsByProposalId(proposal.getId()))
                .willReturn(List.copyOf(proposal.getPositions()));
    }

    private void verifyExpectedInteractions() {
        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        verifyNoMoreInteractions(proposalRepository, memberRepository);
    }

    private String getBudgetText(Long min, Long max) {
        ownedProposalWithSinglePosition(ProposalStatus.MATCHING, min, max);
        return service.getEntry(PROPOSAL_ID, OWNER_EMAIL).positions().get(0).budgetText();
    }

    private Member createMemberWithId(Long id) {
        return createMemberWithId(id, OWNER_EMAIL);
    }

    private Member createMemberWithId(Long id, String email) {
        Member member = Member.create(email, "hashed-pw", "테스터", null, null, "010-1234-5678");
        setId(member, id);
        return member;
    }

    private Proposal createProposalWithStatus(Member owner, Long proposalId, ProposalStatus status) {
        Proposal proposal = Proposal.create(
                owner, "테스트 제안서", "원본 입력", "설명",
                1_000_000L, 5_000_000L, 3L
        );
        setId(proposal, proposalId);

        if (status == ProposalStatus.MATCHING || status == ProposalStatus.COMPLETE) {
            proposal.startMatching();
        }
        if (status == ProposalStatus.COMPLETE) {
            proposal.complete();
        }
        return proposal;
    }

    private ProposalPosition addPosition(
            Proposal proposal,
            Long proposalPositionId,
            String positionCategoryName,
            String positionTitle,
            Long headCount,
            Long budgetMin,
            Long budgetMax,
            Long expectedPeriod
    ) {
        ProposalPosition position = proposal.addPosition(
                Position.create(positionCategoryName),
                positionTitle,
                null,
                headCount,
                budgetMin,
                budgetMax,
                expectedPeriod,
                null,
                null,
                null
        );
        setId(position, proposalPositionId);
        return position;
    }

    private void addSkill(
            ProposalPosition proposalPosition,
            Long skillId,
            String skillName,
            ProposalPositionSkillImportance importance
    ) {
        proposalPosition.addSkill(createSkillWithId(skillId, skillName), importance);
    }

    private Skill createSkillWithId(Long id, String name) {
        Skill skill = Skill.create(name, null);
        setId(skill, id);
        return skill;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
