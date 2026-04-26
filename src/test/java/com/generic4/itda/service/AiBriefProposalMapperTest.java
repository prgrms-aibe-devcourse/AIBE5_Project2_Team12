package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiBriefProposalMapperTest {

    @Mock
    private PositionResolver positionResolver;

    @Mock
    private SkillResolver skillResolver;

    @InjectMocks
    private AiBriefProposalMapper aiBriefProposalMapper;

    @Test
    @DisplayName("AI 브리프를 적용하면 제안서 기본 정보가 바뀌고 AI 결과 기준으로 모집 단위가 동기화된다")
    void apply_updatesProposalFieldsAndSynchronizesPositionsByAiResult() {
        Proposal proposal = createProposal();
        Position oldPosition = Position.create("디자이너");
        Position backend = Position.create("백엔드 개발자");
        Skill oldSkill = Skill.create("Figma", null);
        Skill java = Skill.create("Java", null);
        ProposalPosition oldProposalPosition = proposal.addPosition(
                oldPosition,
                "기존 디자이너",
                null,
                1L,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
        );
        oldProposalPosition.addSkill(oldSkill, ProposalPositionSkillImportance.ESSENTIAL);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Java")).willReturn(Optional.of(java));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "AI가 정리한 제목",
                "AI가 정리한 설명",
                5_000_000L,
                8_000_000L,
                12L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "Node.js 백엔드 개발자",
                                ProposalWorkType.HYBRID,
                                2L,
                                3_000_000L,
                                4_000_000L,
                                12L,
                                3,
                                6,
                                "판교",
                                List.of(AiBriefSkillResult.of("Java", null))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getTitle()).isEqualTo("AI가 정리한 제목");
        assertThat(proposal.getDescription()).isEqualTo("AI가 정리한 설명");
        assertThat(proposal.getTotalBudgetMin()).isEqualTo(5_000_000L);
        assertThat(proposal.getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(proposal.getExpectedPeriod()).isEqualTo(12L);
        assertThat(proposal.getPositions()).hasSize(1);

        ProposalPosition backendPosition = findProposalPosition(proposal, "백엔드 개발자");
        assertThat(backendPosition.getTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(backendPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(backendPosition.getExpectedPeriod()).isEqualTo(12L);
        assertThat(backendPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(backendPosition.getCareerMaxYears()).isEqualTo(6);
        assertThat(backendPosition.getWorkPlace()).isEqualTo("판교");
        assertThat(backendPosition.getSkills()).hasSize(1);
        assertThat(backendPosition.getSkills().get(0).getSkill()).isSameAs(java);
        assertThat(backendPosition.getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.PREFERENCE);
    }

    @Test
    @DisplayName("AI 브리프가 기존 직무를 다시 제안하면 기존 모집 단위를 업데이트한다")
    void apply_updatesExistingPositionWhenPositionNameMatches() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Skill oldSkill = Skill.create("Spring", null);
        Skill newSkill = Skill.create("Java", null);
        ProposalPosition existingPosition = proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        existingPosition.addSkill(oldSkill, ProposalPositionSkillImportance.ESSENTIAL);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Java")).willReturn(Optional.of(newSkill));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                8L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "플랫폼 백엔드 개발자",
                                ProposalWorkType.HYBRID,
                                2L,
                                3_000_000L,
                                4_000_000L,
                                8L,
                                3,
                                6,
                                "판교",
                                List.of(AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getPositions()).hasSize(1);
        ProposalPosition updatedPosition = proposal.getPositions().get(0);
        assertThat(updatedPosition).isSameAs(existingPosition);
        assertThat(updatedPosition.getPosition()).isSameAs(backend);
        assertThat(updatedPosition.getTitle()).isEqualTo("플랫폼 백엔드 개발자");
        assertThat(updatedPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(updatedPosition.getHeadCount()).isEqualTo(2L);
        assertThat(updatedPosition.getUnitBudgetMin()).isEqualTo(3_000_000L);
        assertThat(updatedPosition.getUnitBudgetMax()).isEqualTo(4_000_000L);
        assertThat(updatedPosition.getExpectedPeriod()).isEqualTo(8L);
        assertThat(updatedPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(updatedPosition.getCareerMaxYears()).isEqualTo(6);
        assertThat(updatedPosition.getWorkPlace()).isEqualTo("판교");
        assertThat(updatedPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(tuple("Java", ProposalPositionSkillImportance.ESSENTIAL));
    }

    @Test
    @DisplayName("AI 브리프가 positions 빈 배열을 반환하면 기존 모집 단위를 제거한다")
    void apply_removesExistingPositionsWhenAiBriefReturnsEmptyPositions() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of()
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getTitle()).isEqualTo("새 제목");
        assertThat(proposal.getDescription()).isEqualTo("새 설명");
        assertThat(proposal.getPositions()).isEmpty();
    }

    @Test
    @DisplayName("AI 브리프가 모두 매핑 불가능한 category만 반환하면 기존 모집 단위를 제거한다")
    void apply_removesExistingPositionsWhenAllAiPositionsAreUnresolved() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );

        given(positionResolver.resolve("플랫폼 개발자")).willReturn(Optional.empty());

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "AI가 정리한 제목",
                "AI가 정리한 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "플랫폼 개발자",
                                "플랫폼 엔지니어",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getTitle()).isEqualTo("AI가 정리한 제목");
        assertThat(proposal.getDescription()).isEqualTo("AI가 정리한 설명");
        assertThat(proposal.getPositions()).isEmpty();
    }

    @Test
    @DisplayName("AI 브리프가 같은 category 아래 서로 다른 title을 제안하면 여러 모집 단위를 함께 저장한다")
    void apply_keepsMultiplePositionsWithSameCategoryAndDifferentTitles() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "커머스 백엔드 개발",
                "커머스 서버 개발이 필요합니다.",
                null,
                null,
                8L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                3_000_000L,
                                4_000_000L,
                                4L,
                                3,
                                6,
                                null,
                                List.of()
                        ),
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "정산 서버 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                3_500_000L,
                                4_500_000L,
                                4L,
                                3,
                                6,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        List<String> titles = positionTitles(proposal);

        assertThat(titles).containsExactly("결제 서버 백엔드 개발자", "정산 서버 백엔드 개발자");
    }

    @Test
    @DisplayName("AI 인터뷰 적용은 같은 category 안에서 title이 일치하는 모집 단위만 갱신하고 다른 title은 유지한다")
    void applyForInterview_updatesMatchingTitleOnlyAndPreservesSameCategorySibling() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        ProposalPosition paymentBackendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                3L,
                3,
                6,
                null
        );
        ProposalPosition settlementBackendPosition = proposal.addPosition(
                backend,
                "정산 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_500_000L,
                4_500_000L,
                3L,
                3,
                6,
                null
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "커머스 백엔드 개발",
                "결제 서버 인원을 늘립니다.",
                null,
                null,
                3L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                2L,
                                3_000_000L,
                                4_000_000L,
                                3L,
                                3,
                                6,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자는 2명으로 늘리고 정산 서버 백엔드 개발자는 그대로 두자."
        );

        List<String> titles = positionTitles(proposal);
        ProposalPosition updatedPaymentBackendPosition = findProposalPositionByTitle(proposal, "결제 서버 백엔드 개발자");
        ProposalPosition preservedSettlementBackendPosition = findProposalPositionByTitle(proposal, "정산 서버 백엔드 개발자");

        assertThat(titles).containsExactly("결제 서버 백엔드 개발자", "정산 서버 백엔드 개발자");
        assertThat(updatedPaymentBackendPosition).isSameAs(paymentBackendPosition);
        assertThat(updatedPaymentBackendPosition.getHeadCount()).isEqualTo(2L);
        assertThat(preservedSettlementBackendPosition).isSameAs(settlementBackendPosition);
        assertThat(preservedSettlementBackendPosition.getHeadCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("AI 인터뷰 적용은 같은 category 안에서 title 공백 차이가 있어도 기존 모집 단위를 갱신한다")
    void applyForInterview_updatesExistingPositionWhenNormalizedTitleMatches() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        ProposalPosition paymentBackendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                3L,
                3,
                6,
                null
        );
        ProposalPosition settlementBackendPosition = proposal.addPosition(
                backend,
                "정산 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_500_000L,
                4_500_000L,
                3L,
                3,
                6,
                null
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "커머스 백엔드 개발",
                "결제 서버 인원을 늘립니다.",
                null,
                null,
                3L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제서버백엔드개발자",
                                ProposalWorkType.REMOTE,
                                2L,
                                3_000_000L,
                                4_000_000L,
                                3L,
                                3,
                                6,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자는 2명으로 늘려줘."
        );

        assertThat(proposal.getPositions()).hasSize(2);
        assertThat(paymentBackendPosition.getTitle()).isEqualTo("결제서버백엔드개발자");
        assertThat(paymentBackendPosition.getHeadCount()).isEqualTo(2L);
        assertThat(findProposalPositionByTitle(proposal, "결제서버백엔드개발자")).isSameAs(paymentBackendPosition);
        assertThat(findProposalPositionByTitle(proposal, "정산 서버 백엔드 개발자")).isSameAs(settlementBackendPosition);
    }

    @Test
    @DisplayName("AI 인터뷰에서 제안서 기간만 줄이면 기존 모집 단위 기간도 새 제안서 기간 이하로 보정한다")
    void applyForInterview_clampsExistingPositionPeriodWhenProposalPeriodIsReduced() {
        Proposal proposal = createProposalWithExpectedPeriod(12L);
        Position backend = Position.create("백엔드 개발자");

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                3_000_000L,
                4_000_000L,
                12L,
                3,
                6,
                "판교"
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                8L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "전체 기간만 8주로 줄여줘."
        );

        assertThat(proposal.getExpectedPeriod()).isEqualTo(8L);
        assertThat(backendPosition.getExpectedPeriod()).isEqualTo(8L);
        assertThat(backendPosition.getHeadCount()).isEqualTo(1L);
        assertThat(backendPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(backendPosition.getWorkPlace()).isEqualTo("판교");
        assertThat(backendPosition.getUnitBudgetMin()).isEqualTo(3_000_000L);
        assertThat(backendPosition.getUnitBudgetMax()).isEqualTo(4_000_000L);
        assertThat(backendPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(backendPosition.getCareerMaxYears()).isEqualTo(6);
    }

    @Test
    @DisplayName("AI 인터뷰에서 기존 모집 단위 인원만 수정하면 예산, 기간, 경력, 근무 형태, 근무지, 스킬은 유지한다")
    void applyForInterview_preservesExistingFieldsAndSkillsWhenOnlyHeadCountChanges() {
        Proposal proposal = createProposalWithExpectedPeriod(12L);
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);

        ProposalPosition paymentBackendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                3_000_000L,
                4_000_000L,
                12L,
                3,
                6,
                "판교"
        );
        paymentBackendPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        paymentBackendPosition.addSkill(springBoot, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                2L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자 인원만 2명으로 바꿔줘."
        );

        ProposalPosition updatedPosition = findProposalPositionByTitle(proposal, "결제 서버 백엔드 개발자");

        assertThat(updatedPosition).isSameAs(paymentBackendPosition);
        assertThat(updatedPosition.getHeadCount()).isEqualTo(2L);
        assertThat(updatedPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(updatedPosition.getWorkPlace()).isEqualTo("판교");
        assertThat(updatedPosition.getUnitBudgetMin()).isEqualTo(3_000_000L);
        assertThat(updatedPosition.getUnitBudgetMax()).isEqualTo(4_000_000L);
        assertThat(updatedPosition.getExpectedPeriod()).isEqualTo(12L);
        assertThat(updatedPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(updatedPosition.getCareerMaxYears()).isEqualTo(6);
        assertThat(updatedPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.PREFERENCE)
                );
    }

    @Test
    @DisplayName("AI 인터뷰에서 기존 모집 단위 예산만 수정하면 인원, 기간, 경력, 근무 형태, 근무지, 스킬은 유지한다")
    void applyForInterview_preservesExistingFieldsAndSkillsWhenOnlyBudgetChanges() {
        Proposal proposal = createProposalWithExpectedPeriod(12L);
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);

        ProposalPosition paymentBackendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.HYBRID,
                2L,
                3_000_000L,
                4_000_000L,
                12L,
                3,
                6,
                "판교"
        );
        paymentBackendPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        paymentBackendPosition.addSkill(springBoot, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                null,
                                4_000_000L,
                                5_000_000L,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자 예산만 400만에서 500만으로 바꿔줘."
        );

        ProposalPosition updatedPosition = findProposalPositionByTitle(proposal, "결제 서버 백엔드 개발자");

        assertThat(updatedPosition).isSameAs(paymentBackendPosition);
        assertThat(updatedPosition.getHeadCount()).isEqualTo(2L);
        assertThat(updatedPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(updatedPosition.getWorkPlace()).isEqualTo("판교");
        assertThat(updatedPosition.getUnitBudgetMin()).isEqualTo(4_000_000L);
        assertThat(updatedPosition.getUnitBudgetMax()).isEqualTo(5_000_000L);
        assertThat(updatedPosition.getExpectedPeriod()).isEqualTo(12L);
        assertThat(updatedPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(updatedPosition.getCareerMaxYears()).isEqualTo(6);
        assertThat(updatedPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.PREFERENCE)
                );
    }

    @Test
    @DisplayName("AI 인터뷰 응답 positions가 비어 있어도 기존 모집 단위는 유지한다")
    void applyForInterview_preservesExistingPositionsWhenAiReturnsEmptyPositions() {
        Proposal proposal = createProposalWithExpectedPeriod(8L);
        Position backend = Position.create("백엔드 개발자");
        Position frontend = Position.create("프론트엔드 개발자");

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                8L,
                3,
                6,
                null
        );
        ProposalPosition frontendPosition = proposal.addPosition(
                frontend,
                "기존 프론트엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                2_000_000L,
                3_000_000L,
                8L,
                2,
                5,
                null
        );

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "수정된 제목",
                "수정된 설명",
                null,
                null,
                null,
                List.of()
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "전체 설명만 조금 다듬어줘."
        );

        assertThat(proposal.getPositions()).hasSize(2);
        assertThat(findProposalPositionByTitle(proposal, "기존 백엔드 개발자")).isSameAs(backendPosition);
        assertThat(findProposalPositionByTitle(proposal, "기존 프론트엔드 개발자")).isSameAs(frontendPosition);
    }

    @Test
    @DisplayName("AI 인터뷰 응답 skills가 비어 있으면 기존 스킬을 유지한다")
    void applyForInterview_preservesExistingSkillsWhenAiReturnsEmptySkills() {
        Proposal proposal = createProposalWithExpectedPeriod(8L);
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                8L,
                3,
                6,
                null
        );
        backendPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        backendPosition.addSkill(springBoot, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                2L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자 인원만 2명으로 바꿔줘."
        );

        assertThat(backendPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.PREFERENCE)
                );
    }

    @Test
    @DisplayName("AI 인터뷰 응답 skills가 있으면 기존 스킬을 삭제하지 않고 새 스킬을 추가한다")
    void applyForInterview_addsNewSkillsWithoutRemovingExistingSkills() {
        Proposal proposal = createProposalWithExpectedPeriod(8L);
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);
        Skill redis = Skill.create("Redis", null);

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                8L,
                3,
                6,
                null
        );
        backendPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        backendPosition.addSkill(springBoot, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Redis")).willReturn(Optional.of(redis));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("Redis", ProposalPositionSkillImportance.PREFERENCE))
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자에 Redis도 추가해줘."
        );

        assertThat(backendPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.PREFERENCE),
                        tuple("Redis", ProposalPositionSkillImportance.PREFERENCE)
                );
    }

    @Test
    @DisplayName("AI 인터뷰 응답 skills가 기존 스킬을 포함하면 기존 스킬을 삭제하지 않고 중요도만 갱신한다")
    void applyForInterview_updatesExistingSkillImportanceWithoutRemovingOmittedSkills() {
        Proposal proposal = createProposalWithExpectedPeriod(8L);
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);
        Skill springBoot = Skill.create("Spring Boot", null);

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                8L,
                3,
                6,
                null
        );
        backendPosition.addSkill(java, ProposalPositionSkillImportance.PREFERENCE);
        backendPosition.addSkill(springBoot, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Java")).willReturn(Optional.of(java));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                null,
                null,
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "결제 서버 백엔드 개발자",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "결제 서버 백엔드 개발자에서 Java는 필수로 바꿔줘."
        );

        assertThat(backendPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.PREFERENCE)
                );
    }

    @Test
    @DisplayName("AI 브리프가 기존 스킬을 다시 제안하면 중복 추가하지 않고 중요도만 갱신한다")
    void apply_updatesExistingSkillImportanceWithoutDuplicatingSkill() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);

        ProposalPosition existingPosition = proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        existingPosition.addSkill(java, ProposalPositionSkillImportance.PREFERENCE);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Java")).willReturn(Optional.of(java));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                8L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "플랫폼 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                8L,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        ProposalPosition updatedPosition = proposal.getPositions().get(0);
        assertThat(updatedPosition.getSkills()).hasSize(1);
        assertThat(updatedPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(tuple("Java", ProposalPositionSkillImportance.ESSENTIAL));
    }

    @Test
    @DisplayName("AI 브리프 포지션 필수값이 비어 있으면 기본값으로 보정한다")
    void apply_fillsDefaultRequiredPositionFieldsWhenAiOmitsThem() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "백엔드 개발자",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getPositions()).hasSize(1);
        ProposalPosition proposalPosition = proposal.getPositions().get(0);
        assertThat(proposalPosition.getPosition()).isSameAs(backend);
        assertThat(proposalPosition.getTitle()).isEqualTo("백엔드 개발자");
        assertThat(proposalPosition.getWorkType()).isEqualTo(ProposalWorkType.REMOTE);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(1L);
        assertThat(proposalPosition.getWorkPlace()).isNull();
    }

    @Test
    @DisplayName("AI 브리프 제목이 비어 있으면 기존 제안서 제목을 유지한다")
    void apply_keepsExistingTitleWhenAiTitleIsBlank() {
        Proposal proposal = createProposal();
        AiBriefResult aiBriefResult = AiBriefResult.of(
                " ",
                "설명만 갱신",
                null,
                null,
                8L,
                List.of()
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getTitle()).isEqualTo("제안서 제목");
        assertThat(proposal.getDescription()).isEqualTo("설명만 갱신");
        assertThat(proposal.getExpectedPeriod()).isEqualTo(8L);
    }

    @Test
    @DisplayName("AI 응답에 값이 없으면 기존 제안서 정보는 유지하고 AI 브리프 positions 빈 결과 기준으로 모집 단위는 제거한다")
    void apply_keepsExistingFieldsWhenAiReturnsNullAndSynchronizesEmptyPositions() {
        Proposal proposal = createProposal();
        Position oldPosition = Position.create("디자이너");
        Skill oldSkill = Skill.create("Figma", null);
        ProposalPosition oldProposalPosition = proposal.addPosition(
                oldPosition,
                "기존 디자이너",
                null,
                1L,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
        );
        oldProposalPosition.addSkill(oldSkill, ProposalPositionSkillImportance.ESSENTIAL);

        AiBriefResult aiBriefResult = AiBriefResult.of(
                " ",
                null,
                null,
                null,
                null,
                List.of()
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getTitle()).isEqualTo("제안서 제목");
        assertThat(proposal.getDescription()).isEqualTo("기존 설명");
        assertThat(proposal.getTotalBudgetMin()).isEqualTo(1_000_000L);
        assertThat(proposal.getTotalBudgetMax()).isEqualTo(2_000_000L);
        assertThat(proposal.getExpectedPeriod()).isEqualTo(3L);
        assertThat(proposal.getPositions()).isEmpty();
    }

    @Test
    @DisplayName("동일 이름의 직무와 스킬이 이미 있으면 기존 마스터를 재사용한다")
    void apply_reusesExistingPositionAndSkillWhenFoundByName() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Java")).willReturn(Optional.of(java));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "플랫폼 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        assertThat(proposal.getPositions()).hasSize(1);
        assertThat(proposal.getPositions().get(0).getPosition()).isSameAs(backend);
        assertThat(proposal.getPositions().get(0).getSkills()).hasSize(1);
        assertThat(proposal.getPositions().get(0).getSkills().get(0).getSkill()).isSameAs(java);
        then(positionResolver).should().resolve("백엔드 개발자");
    }

    @Test
    @DisplayName("AI 브리프 스킬 문자열은 SkillResolver로 정규 Skill에 매핑해 저장한다")
    void apply_resolvesAiSkillAliasToCanonicalSkill() {
        Proposal proposal = createProposal();
        Position frontend = Position.create("프론트엔드 개발자");
        Skill react = Skill.create("React", null);

        given(positionResolver.resolve("프론트엔드 개발자")).willReturn(Optional.of(frontend));
        given(skillResolver.resolve("React.js")).willReturn(Optional.of(react));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "프론트엔드 개발자",
                                "React 프론트엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("React.js", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        ProposalPosition proposalPosition = proposal.getPositions().get(0);
        assertThat(proposalPosition.getSkills()).hasSize(1);
        assertThat(proposalPosition.getSkills().get(0).getSkill()).isSameAs(react);
        assertThat(proposalPosition.getSkills().get(0).getSkill().getName()).isEqualTo("React");
        assertThat(proposalPosition.getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.ESSENTIAL);
    }

    @Test
    @DisplayName("AI 브리프 스킬이 기존 Skill에 매핑되지 않으면 저장하지 않는다")
    void apply_skipsUnresolvedAiSkillWithoutCreatingSkill() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillResolver.resolve("Unknown Skill")).willReturn(Optional.empty());

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("Unknown Skill", ProposalPositionSkillImportance.ESSENTIAL))
                        )
                )
        );

        aiBriefProposalMapper.apply(proposal, aiBriefResult);

        ProposalPosition proposalPosition = proposal.getPositions().get(0);
        assertThat(proposalPosition.getSkills()).isEmpty();
        then(skillResolver).should().resolve("Unknown Skill");
    }

    @Test
    @DisplayName("AI 인터뷰 적용은 응답에 없는 기존 모집 단위를 삭제하지 않는다")
    void applyForInterview_preservesExistingPositionsMissingFromAiResult() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Position frontend = Position.create("프론트엔드 개발자");

        ProposalPosition backendPosition = proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        ProposalPosition frontendPosition = proposal.addPosition(
                frontend,
                "기존 프론트엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "플랫폼 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                2L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(proposal, aiBriefResult, "백엔드 개발자는 2명으로 하자.");

        assertThat(proposal.getPositions()).hasSize(2);
        assertThat(findProposalPosition(proposal, "백엔드 개발자")).isSameAs(backendPosition);
        assertThat(findProposalPosition(proposal, "백엔드 개발자").getHeadCount()).isEqualTo(2L);
        assertThat(findProposalPosition(proposal, "프론트엔드 개발자")).isSameAs(frontendPosition);
    }

    @Test
    @DisplayName("AI 인터뷰 적용은 사용자 메시지에 명시적으로 삭제 의도가 있는 모집 단위만 삭제한다")
    void applyForInterview_removesExplicitlyDeletedPositionOnly() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Position frontend = Position.create("프론트엔드 개발자");
        Position designer = Position.create("UI/UX 디자이너");

        proposal.addPosition(
                backend,
                "기존 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        proposal.addPosition(
                frontend,
                "기존 프론트엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );
        proposal.addPosition(
                designer,
                "UI/UX 디자이너",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                3L,
                null,
                null,
                null
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));
        given(positionResolver.resolve("프론트엔드 개발자")).willReturn(Optional.of(frontend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "새 제목",
                "새 설명",
                null,
                null,
                null,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                2L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        ),
                        AiBriefPositionResult.of(
                                "프론트엔드 개발자",
                                "프론트엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "백엔드 개발자 2명, 프론트엔드 개발자 1명으로 가고 UI/UX 디자이너는 빼자."
        );

        assertThat(proposal.getPositions()).hasSize(2);
        assertThat(findProposalPosition(proposal, "백엔드 개발자").getHeadCount()).isEqualTo(2L);
        assertThat(findProposalPosition(proposal, "프론트엔드 개발자").getHeadCount()).isEqualTo(1L);
        assertThat(proposal.getPositions())
                .noneMatch(position -> position.getPosition().getName().equals("UI/UX 디자이너"));
    }

    @Test
    @DisplayName("AI 인터뷰에서 같은 category에 여러 title이 있을 때 category만 삭제 요청하면 기존 모집 단위를 유지하고 같은 category 신규 추가를 막는다")
    void applyForInterview_preservesSameCategorySiblingsWhenCategoryOnlyDeletionIsAmbiguous() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");

        ProposalPosition paymentBackendPosition = proposal.addPosition(
                backend,
                "결제 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_000_000L,
                4_000_000L,
                3L,
                3,
                6,
                null
        );
        ProposalPosition settlementBackendPosition = proposal.addPosition(
                backend,
                "정산 서버 백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                3_500_000L,
                4_500_000L,
                3L,
                3,
                6,
                null
        );

        given(positionResolver.resolve("백엔드 개발자")).willReturn(Optional.of(backend));

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "커머스 백엔드 개발",
                "백엔드 모집 단위 삭제 요청을 확인합니다.",
                null,
                null,
                3L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                "플랫폼 백엔드 개발자",
                                ProposalWorkType.REMOTE,
                                1L,
                                3_000_000L,
                                4_000_000L,
                                3L,
                                3,
                                6,
                                null,
                                List.of()
                        )
                )
        );

        aiBriefProposalMapper.applyForInterview(
                proposal,
                aiBriefResult,
                "백엔드 개발자는 빼자."
        );

        List<String> titles = positionTitles(proposal);

        assertThat(titles).containsExactly("결제 서버 백엔드 개발자", "정산 서버 백엔드 개발자");
        assertThat(findProposalPositionByTitle(proposal, "결제 서버 백엔드 개발자")).isSameAs(paymentBackendPosition);
        assertThat(findProposalPositionByTitle(proposal, "정산 서버 백엔드 개발자")).isSameAs(settlementBackendPosition);
    }

    private ProposalPosition findProposalPosition(Proposal proposal, String positionName) {
        return proposal.getPositions().stream()
                .filter(proposalPosition -> proposalPosition.getPosition().getName().equals(positionName))
                .findFirst()
                .orElseThrow();
    }

    private ProposalPosition findProposalPositionByTitle(Proposal proposal, String title) {
        return proposal.getPositions().stream()
                .filter(proposalPosition -> title.equals(proposalPosition.getTitle()))
                .findFirst()
                .orElseThrow();
    }

    private List<String> positionTitles(Proposal proposal) {
        return proposal.getPositions().stream()
                .map(ProposalPosition::getTitle)
                .toList();
    }

    private Proposal createProposal() {
        return createProposalWithExpectedPeriod(3L);
    }

    private Proposal createProposalWithExpectedPeriod(Long expectedPeriod) {
        return Proposal.create(
                createMember(),
                "제안서 제목",
                "원본 입력",
                "기존 설명",
                1_000_000L,
                2_000_000L,
                expectedPeriod
        );
    }
}