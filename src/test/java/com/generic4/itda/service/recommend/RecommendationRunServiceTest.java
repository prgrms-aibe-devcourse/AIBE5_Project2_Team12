package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationResultRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * RecommendationRunService 단위 테스트:
 * - service가 제안서 상세를 두 단계 조회(findWithPositionsById -> findPositionsWithSkillsByProposalId)로 명시적으로 조합하는지
 * - 권한/상태 검증 이후 fingerprint 기반 재사용 또는 신규 run 생성을 올바르게 오케스트레이션하는지
 *
 * 한계:
 * - 이 테스트는 Mockito 기반 단위 테스트라 실제 JPA fetch/lazy, 1차 캐시 합성은 검증하지 않는다.
 * - proposal/position 그래프 preload 계약은 ProposalRepositoryTest가, fingerprint 내용 자체의 계약은
 *   RecommendationFingerprintGeneratorTest가 보장해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationRunServiceTest {

    private static final Long PROPOSAL_ID = 10L;
    private static final Long OWNER_ID = 1L;
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final Long SELECTED_POSITION_ID = 20L;
    private static final String FINGERPRINT = "fp-abc123";
    private static final Long EXISTING_RUN_ID = 301L;
    private static final Long NEW_RUN_ID = 302L;
    private static final RecommendationAlgorithm DEFAULT_ALGORITHM = RecommendationAlgorithm.HEURISTIC_V1;
    private static final int DEFAULT_TOP_K = 3;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RecommendationRunRepository recommendationRunRepository;

    @Mock
    private RecommendationResultRepository recommendationResultRepository;

    @Mock
    private RecommendationFingerprintGenerator fingerprintGenerator;

    @InjectMocks
    private RecommendationRunService service;

    @Test
    @DisplayName("동일한 fingerprint의 기존 run이 있으면 저장하지 않고 기존 id를 재사용한다")
    void createOrReuse_returnsExistingRunIdWhenReusableRunExists() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, 10L, "UI/UX 디자이너", ProposalPositionStatus.OPEN);
        ProposalPosition selectedPosition = addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);

        RecommendationRun existingRun = RecommendationRun.create(
                selectedPosition,
                FINGERPRINT,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K
        );
        ReflectionTestUtils.setField(existingRun, "id", EXISTING_RUN_ID);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);
        given(fingerprintGenerator.generate(selectedPosition, DEFAULT_ALGORITHM, DEFAULT_TOP_K))
                .willReturn(FINGERPRINT);
        given(recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                SELECTED_POSITION_ID,
                FINGERPRINT,
                DEFAULT_ALGORITHM
        )).willReturn(Optional.of(existingRun));

        // when
        Long result = service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL);

        // then
        assertThat(result).isEqualTo(EXISTING_RUN_ID);

        InOrder inOrder = inOrder(proposalRepository, memberRepository, fingerprintGenerator, recommendationRunRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        inOrder.verify(fingerprintGenerator).generate(selectedPosition, DEFAULT_ALGORITHM, DEFAULT_TOP_K);
        inOrder.verify(recommendationRunRepository)
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        SELECTED_POSITION_ID,
                        FINGERPRINT,
                        DEFAULT_ALGORITHM
                );

        verifyNoMoreInteractions(proposalRepository, memberRepository, fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("재사용 가능한 run이 없으면 PENDING 상태의 새 run을 생성해 저장한다")
    void createOrReuse_createsNewPendingRunWhenReusableRunDoesNotExist() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, 10L, "UI/UX 디자이너", ProposalPositionStatus.OPEN);
        ProposalPosition selectedPosition = addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);
        given(fingerprintGenerator.generate(selectedPosition, DEFAULT_ALGORITHM, DEFAULT_TOP_K))
                .willReturn(FINGERPRINT);
        given(recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                SELECTED_POSITION_ID,
                FINGERPRINT,
                DEFAULT_ALGORITHM
        )).willReturn(Optional.empty());
        given(recommendationRunRepository.save(any(RecommendationRun.class))).willAnswer(invocation -> {
            RecommendationRun run = invocation.getArgument(0);
            ReflectionTestUtils.setField(run, "id", NEW_RUN_ID);
            return run;
        });

        // when
        Long result = service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL);

        // then
        assertThat(result).isEqualTo(NEW_RUN_ID);

        ArgumentCaptor<RecommendationRun> runCaptor = ArgumentCaptor.forClass(RecommendationRun.class);

        InOrder inOrder = inOrder(proposalRepository, memberRepository, fingerprintGenerator, recommendationRunRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        inOrder.verify(fingerprintGenerator).generate(selectedPosition, DEFAULT_ALGORITHM, DEFAULT_TOP_K);
        inOrder.verify(recommendationRunRepository)
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        SELECTED_POSITION_ID,
                        FINGERPRINT,
                        DEFAULT_ALGORITHM
                );
        inOrder.verify(recommendationRunRepository).save(runCaptor.capture());

        RecommendationRun savedRun = runCaptor.getValue();
        assertThat(savedRun.getProposalPosition()).isSameAs(selectedPosition);
        assertThat(savedRun.getRequestFingerprint()).isEqualTo(FINGERPRINT);
        assertThat(savedRun.getAlgorithm()).isEqualTo(DEFAULT_ALGORITHM);
        assertThat(savedRun.getTopK()).isEqualTo(DEFAULT_TOP_K);
        assertThat(savedRun.getStatus()).isEqualTo(RecommendationRunStatus.PENDING);

        verifyNoMoreInteractions(proposalRepository, memberRepository, fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("추가 추천에서 동일한 제외 이력서 목록 기반 fingerprint의 기존 run이 있으면 기존 id를 재사용한다")
    void createAdditional_returnsExistingRunIdWhenReusableAdditionalRunExists() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, 10L, "UI/UX 디자이너", ProposalPositionStatus.OPEN);
        ProposalPosition selectedPosition = addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);
        List<Long> excludedResumeIds = List.of(1L, 2L, 3L);

        RecommendationRun existingRun = RecommendationRun.create(
                selectedPosition,
                FINGERPRINT,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K
        );
        ReflectionTestUtils.setField(existingRun, "id", EXISTING_RUN_ID);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);
        given(recommendationResultRepository.findRecommendedResumeIdsByProposalPositionId(SELECTED_POSITION_ID))
                .willReturn(excludedResumeIds);
        given(fingerprintGenerator.generateAdditional(
                selectedPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K,
                excludedResumeIds
        )).willReturn(FINGERPRINT);
        given(recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                SELECTED_POSITION_ID,
                FINGERPRINT,
                DEFAULT_ALGORITHM
        )).willReturn(Optional.of(existingRun));

        // when
        Long result = service.createAdditional(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL);

        // then
        assertThat(result).isEqualTo(EXISTING_RUN_ID);

        InOrder inOrder = inOrder(
                proposalRepository,
                memberRepository,
                recommendationResultRepository,
                fingerprintGenerator,
                recommendationRunRepository
        );
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        inOrder.verify(recommendationResultRepository)
                .findRecommendedResumeIdsByProposalPositionId(SELECTED_POSITION_ID);
        inOrder.verify(fingerprintGenerator).generateAdditional(
                selectedPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K,
                excludedResumeIds
        );
        inOrder.verify(recommendationRunRepository)
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        SELECTED_POSITION_ID,
                        FINGERPRINT,
                        DEFAULT_ALGORITHM
                );

        verifyNoMoreInteractions(
                proposalRepository,
                memberRepository,
                recommendationResultRepository,
                fingerprintGenerator,
                recommendationRunRepository
        );
    }

    @Test
    @DisplayName("추가 추천에서 재사용 가능한 run이 없으면 기존 추천 이력서를 제외한 새 run을 생성해 저장한다")
    void createAdditional_createsNewPendingRunWhenReusableAdditionalRunDoesNotExist() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, 10L, "UI/UX 디자이너", ProposalPositionStatus.OPEN);
        ProposalPosition selectedPosition = addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);
        List<Long> excludedResumeIds = List.of(1L, 2L, 3L);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);
        given(recommendationResultRepository.findRecommendedResumeIdsByProposalPositionId(SELECTED_POSITION_ID))
                .willReturn(excludedResumeIds);
        given(fingerprintGenerator.generateAdditional(
                selectedPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K,
                excludedResumeIds
        )).willReturn(FINGERPRINT);
        given(recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                SELECTED_POSITION_ID,
                FINGERPRINT,
                DEFAULT_ALGORITHM
        )).willReturn(Optional.empty());
        given(recommendationRunRepository.save(any(RecommendationRun.class))).willAnswer(invocation -> {
            RecommendationRun run = invocation.getArgument(0);
            ReflectionTestUtils.setField(run, "id", NEW_RUN_ID);
            return run;
        });

        // when
        Long result = service.createAdditional(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL);

        // then
        assertThat(result).isEqualTo(NEW_RUN_ID);

        ArgumentCaptor<RecommendationRun> runCaptor = ArgumentCaptor.forClass(RecommendationRun.class);

        InOrder inOrder = inOrder(
                proposalRepository,
                memberRepository,
                recommendationResultRepository,
                fingerprintGenerator,
                recommendationRunRepository
        );
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);
        inOrder.verify(recommendationResultRepository)
                .findRecommendedResumeIdsByProposalPositionId(SELECTED_POSITION_ID);
        inOrder.verify(fingerprintGenerator).generateAdditional(
                selectedPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K,
                excludedResumeIds
        );
        inOrder.verify(recommendationRunRepository)
                .findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                        SELECTED_POSITION_ID,
                        FINGERPRINT,
                        DEFAULT_ALGORITHM
                );
        inOrder.verify(recommendationRunRepository).save(runCaptor.capture());

        RecommendationRun savedRun = runCaptor.getValue();
        assertThat(savedRun.getProposalPosition()).isSameAs(selectedPosition);
        assertThat(savedRun.getRequestFingerprint()).isEqualTo(FINGERPRINT);
        assertThat(savedRun.getAlgorithm()).isEqualTo(DEFAULT_ALGORITHM);
        assertThat(savedRun.getTopK()).isEqualTo(DEFAULT_TOP_K);
        assertThat(savedRun.getStatus()).isEqualTo(RecommendationRunStatus.PENDING);

        verifyNoMoreInteractions(
                proposalRepository,
                memberRepository,
                recommendationResultRepository,
                fingerprintGenerator,
                recommendationRunRepository
        );
    }

    @Test
    @DisplayName("추가 추천에서 OPEN 상태가 아닌 모집 포지션이면 기존 추천 결과를 조회하지 않고 거부한다")
    void createAdditional_throwsBeforeLoadingExcludedResumesWhenProposalPositionStatusIsNotOpen() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.FULL);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when / then
        assertThatThrownBy(() -> service.createAdditional(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPEN 상태의 모집 포지션만 추천을 실행할 수 있습니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(recommendationResultRepository, fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("존재하지 않는 proposal이면 IllegalArgumentException이 발생한다")
    void createOrReuse_throwsWhenProposalNotFound() {
        // given
        given(proposalRepository.findWithPositionsById(PROPOSAL_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 제안서입니다.");

        verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        verifyNoMoreInteractions(proposalRepository);
        verifyNoInteractions(memberRepository, fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("존재하지 않는 member이면 IllegalArgumentException이 발생한다")
    void createOrReuse_throwsWhenMemberNotFound() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(null);

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 회원입니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("본인 제안서가 아니면 IllegalArgumentException이 발생한다")
    void createOrReuse_throwsWhenMemberIsNotOwner() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Member other = createMemberWithId(2L, "other@example.com");
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value("other@example.com")).willReturn(other);

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 제안서만 조회할 수 있습니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value("other@example.com");

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("MATCHING 상태가 아닌 proposal이면 IllegalStateException이 발생한다")
    void createOrReuse_throwsWhenProposalStatusIsNotMatching() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.WRITING);
        addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.OPEN);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("MATCHING 상태의 제안서만 추천을 실행할 수 있습니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("선택한 proposalPositionId가 제안서에 없으면 IllegalArgumentException이 발생한다")
    void createOrReuse_throwsWhenProposalPositionDoesNotBelongToProposal() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, 10L, "디자이너", ProposalPositionStatus.OPEN);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, 999L, OWNER_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 제안서에 속한 모집 포지션이 아닙니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(fingerprintGenerator, recommendationRunRepository);
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 모집 포지션이면 IllegalStateException이 발생한다")
    void createOrReuse_throwsWhenProposalPositionStatusIsNotOpen() {
        // given
        Member owner = createMemberWithId(OWNER_ID, OWNER_EMAIL);
        Proposal proposal = createProposalWithStatus(owner, PROPOSAL_ID, ProposalStatus.MATCHING);
        addPosition(proposal, SELECTED_POSITION_ID, "백엔드 개발자", ProposalPositionStatus.FULL);

        stubProposalDetailLoad(proposal);
        given(memberRepository.findByEmail_Value(OWNER_EMAIL)).willReturn(owner);

        // when / then
        assertThatThrownBy(() -> service.createOrReuse(PROPOSAL_ID, SELECTED_POSITION_ID, OWNER_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPEN 상태의 모집 포지션만 추천을 실행할 수 있습니다.");

        InOrder inOrder = inOrder(proposalRepository, memberRepository);
        inOrder.verify(proposalRepository).findWithPositionsById(PROPOSAL_ID);
        inOrder.verify(proposalRepository).findPositionsWithSkillsByProposalId(PROPOSAL_ID);
        inOrder.verify(memberRepository).findByEmail_Value(OWNER_EMAIL);

        verifyNoMoreInteractions(proposalRepository, memberRepository);
        verifyNoInteractions(fingerprintGenerator, recommendationRunRepository);
    }

    private void stubProposalDetailLoad(Proposal proposal) {
        // mock 기반 서비스 테스트에서는 repository가 필요한 aggregate를 준비해 준다는 전제만 고정한다.
        // 실제 fetch join/lazy 초기화 계약은 ProposalRepositoryTest에서 검증되어야 한다.
        given(proposalRepository.findWithPositionsById(proposal.getId())).willReturn(Optional.of(proposal));
        given(proposalRepository.findPositionsWithSkillsByProposalId(proposal.getId())).willReturn(proposal.getPositions());
    }

    private Proposal createProposalWithStatus(Member member, Long proposalId, ProposalStatus status) {
        Proposal proposal = Proposal.create(
                member,
                "테스트 제안서",
                "raw-input",
                "설명",
                3_000_000L,
                5_000_000L,
                3L
        );
        ReflectionTestUtils.setField(proposal, "id", proposalId);
        ReflectionTestUtils.setField(proposal, "status", status);
        return proposal;
    }

    private ProposalPosition addPosition(
            Proposal proposal,
            Long proposalPositionId,
            String positionName,
            ProposalPositionStatus status
    ) {
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
        proposalPosition.changeStatus(status);
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
