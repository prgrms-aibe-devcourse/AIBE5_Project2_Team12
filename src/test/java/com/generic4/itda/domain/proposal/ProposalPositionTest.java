package com.generic4.itda.domain.proposal;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.skill.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProposalPositionTest {

    @DisplayName("유효한 입력이 주어지면 OPEN 상태의 모집 단위를 생성한다")
    @Test
    void createWithValidInputs() {
        Proposal proposal = createProposal();
        Position position = Position.create("백엔드 개발자");

        ProposalPosition proposalPosition = ProposalPosition.create(proposal, position, 2L, 3_000_000L, 5_000_000L);

        assertThat(proposalPosition.getProposal()).isEqualTo(proposal);
        assertThat(proposalPosition.getPosition()).isEqualTo(position);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(2L);
        assertThat(proposalPosition.getUnitBudgetMin()).isEqualTo(3_000_000L);
        assertThat(proposalPosition.getUnitBudgetMax()).isEqualTo(5_000_000L);
        assertThat(proposalPosition.getTitle()).isNull();
        assertThat(proposalPosition.getWorkType()).isNull();
        assertThat(proposalPosition.getExpectedPeriod()).isNull();
        assertThat(proposalPosition.getCareerMinYears()).isNull();
        assertThat(proposalPosition.getCareerMaxYears()).isNull();
        assertThat(proposalPosition.getWorkPlace()).isNull();
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
        assertThat(proposal.getPositions()).isEmpty();
    }

    @DisplayName("상세 필드가 주어지면 모집 단위에 함께 저장한다")
    @Test
    void createWithDetailFields() {
        Proposal proposal = createProposal();
        Position position = Position.create("백엔드 개발자");

        ProposalPosition proposalPosition = ProposalPosition.create(
                proposal,
                position,
                "  Node.js 백엔드 개발자  ",
                ProposalWorkType.HYBRID,
                2L,
                3_000_000L,
                5_000_000L,
                12L,
                3,
                7,
                "  판교  "
        );

        assertThat(proposalPosition.getTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(proposalPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(proposalPosition.getExpectedPeriod()).isEqualTo(12L);
        assertThat(proposalPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(proposalPosition.getCareerMaxYears()).isEqualTo(7);
        assertThat(proposalPosition.getWorkPlace()).isEqualTo("판교");
    }

    @DisplayName("상위 제안서가 없으면 생성에 실패한다")
    @Test
    void failWhenProposalIsNull() {
        assertThatThrownBy(() -> ProposalPosition.create(
                null,
                Position.create("백엔드 개발자"),
                1L,
                1_000_000L,
                2_000_000L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("상위 제안서는 필수값입니다.");
    }

    @DisplayName("직무가 없으면 생성에 실패한다")
    @Test
    void failWhenPositionIsNull() {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                null,
                1L,
                1_000_000L,
                2_000_000L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("직무는 필수값입니다.");
    }

    @DisplayName("모집 인원이 0 이하이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void failWhenHeadCountIsNotPositive(long headCount) {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                headCount,
                1_000_000L,
                2_000_000L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모집 인원은 양수여야 합니다.");
    }

    @DisplayName("1인 기준 예산 범위가 뒤집히면 생성에 실패한다")
    @Test
    void failWhenUnitBudgetRangeIsInvalid() {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                5_000_000L,
                1_000_000L
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("1인 기준 최소 예산은 최대 예산보다 클 수 없습니다.");
    }

    @DisplayName("포지션 예상 기간이 0 이하이면 생성에 실패한다")
    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void failWhenExpectedPeriodIsNotPositive(long expectedPeriod) {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                1_000_000L,
                2_000_000L,
                expectedPeriod,
                1,
                3,
                "판교"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포지션 예상 기간은 양수여야 합니다.");
    }

    @DisplayName("경력 연차는 음수일 수 없다")
    @Test
    void failWhenCareerYearsIsNegative() {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                1_000_000L,
                2_000_000L,
                12L,
                -1,
                3,
                "판교"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최소 경력 연차는 음수일 수 없습니다.");
    }

    @DisplayName("최소 경력 연차는 최대 경력 연차보다 클 수 없다")
    @Test
    void failWhenCareerRangeIsInvalid() {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                1_000_000L,
                2_000_000L,
                12L,
                5,
                3,
                "판교"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최소 경력 연차는 최대 경력 연차보다 클 수 없습니다.");
    }

    @DisplayName("원격 근무이면 근무지는 비워야 한다")
    @Test
    void failWhenRemoteHasWorkPlace() {
        assertThatThrownBy(() -> ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                ProposalWorkType.REMOTE,
                1L,
                1_000_000L,
                2_000_000L,
                12L,
                1,
                3,
                "판교"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("원격 근무이면 근무지는 비워야 합니다.");
    }

    @DisplayName("유효한 입력이면 모집 단위를 수정한다")
    @Test
    void updateProposalPosition() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                2_000_000L,
                3_000_000L
        );
        Position updatedPosition = Position.create("프론트엔드 개발자");

        proposalPosition.update(updatedPosition, 3L, 4_000_000L, 6_000_000L);

        assertThat(proposalPosition.getPosition()).isEqualTo(updatedPosition);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(3L);
        assertThat(proposalPosition.getUnitBudgetMin()).isEqualTo(4_000_000L);
        assertThat(proposalPosition.getUnitBudgetMax()).isEqualTo(6_000_000L);
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @DisplayName("상세 필드와 함께 모집 단위를 수정할 수 있다")
    @Test
    void updateProposalPositionWithDetailFields() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                ProposalWorkType.SITE,
                1L,
                2_000_000L,
                3_000_000L,
                8L,
                2,
                5,
                "강남"
        );
        Position updatedPosition = Position.create("프론트엔드 개발자");

        proposalPosition.update(
                updatedPosition,
                "React 프론트엔드 개발자",
                ProposalWorkType.REMOTE,
                3L,
                4_000_000L,
                6_000_000L,
                16L,
                4,
                8,
                null
        );

        assertThat(proposalPosition.getPosition()).isEqualTo(updatedPosition);
        assertThat(proposalPosition.getTitle()).isEqualTo("React 프론트엔드 개발자");
        assertThat(proposalPosition.getWorkType()).isEqualTo(ProposalWorkType.REMOTE);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(3L);
        assertThat(proposalPosition.getUnitBudgetMin()).isEqualTo(4_000_000L);
        assertThat(proposalPosition.getUnitBudgetMax()).isEqualTo(6_000_000L);
        assertThat(proposalPosition.getExpectedPeriod()).isEqualTo(16L);
        assertThat(proposalPosition.getCareerMinYears()).isEqualTo(4);
        assertThat(proposalPosition.getCareerMaxYears()).isEqualTo(8);
        assertThat(proposalPosition.getWorkPlace()).isNull();
    }

    @DisplayName("기존 update 시그니처를 써도 새 상세 필드는 유지된다")
    @Test
    void updateWithLegacySignaturePreservesDetailFields() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "Node.js 백엔드 개발자",
                ProposalWorkType.HYBRID,
                1L,
                2_000_000L,
                3_000_000L,
                10L,
                3,
                6,
                "판교"
        );

        proposalPosition.update(Position.create("서버 개발자"), 2L, 4_000_000L, 5_000_000L);

        assertThat(proposalPosition.getTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(proposalPosition.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(proposalPosition.getExpectedPeriod()).isEqualTo(10L);
        assertThat(proposalPosition.getCareerMinYears()).isEqualTo(3);
        assertThat(proposalPosition.getCareerMaxYears()).isEqualTo(6);
        assertThat(proposalPosition.getWorkPlace()).isEqualTo("판교");
    }

    @DisplayName("같은 제안서 안에 이미 존재하는 직무로 모집 단위를 수정할 수 없다")
    @Test
    void failWhenUpdatePositionToDuplicatedPosition() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Position frontend = Position.create("프론트엔드 개발자");
        proposal.addPosition(backend, 1L, 2_000_000L, 3_000_000L);
        ProposalPosition proposalPosition = proposal.addPosition(frontend, 1L, 2_000_000L, 3_000_000L);

        assertThatThrownBy(() -> proposalPosition.update(backend, 2L, 3_000_000L, 4_000_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("같은 제안서에는 동일한 직무를 중복 등록할 수 없습니다.");
    }

    @DisplayName("모집 상태를 변경할 수 있다")
    @Test
    void changeStatus() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                2_000_000L,
                3_000_000L
        );

        proposalPosition.changeStatus(ProposalPositionStatus.FULL);
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.FULL);

        proposalPosition.changeStatus(ProposalPositionStatus.CLOSED);
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.CLOSED);
    }

    @DisplayName("모집 단위에 요구 스킬을 추가하고 제거할 수 있다")
    @Test
    void addAndRemoveSkill() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                2_000_000L,
                3_000_000L
        );
        Skill skill = Skill.create("Java", "백엔드 언어");

        ProposalPositionSkill proposalPositionSkill = proposalPosition.addSkill(skill,
                ProposalPositionSkillImportance.ESSENTIAL);

        assertThat(proposalPosition.getSkills()).containsExactly(proposalPositionSkill);
        assertThat(proposalPositionSkill.getProposalPosition()).isEqualTo(proposalPosition);

        proposalPosition.removeSkill(skill);

        assertThat(proposalPosition.getSkills()).isEmpty();
        assertThat(proposalPositionSkill.getProposalPosition()).isNull();
    }

    @DisplayName("같은 모집 단위에는 동일한 스킬을 중복 등록할 수 없다")
    @Test
    void failWhenSkillIsDuplicated() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                2_000_000L,
                3_000_000L
        );
        Skill skill = Skill.create("Java", "백엔드 언어");
        proposalPosition.addSkill(skill, ProposalPositionSkillImportance.ESSENTIAL);

        assertThatThrownBy(() -> proposalPosition.addSkill(skill, ProposalPositionSkillImportance.PREFERENCE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("같은 모집 단위에는 동일한 스킬을 중복 등록할 수 없습니다.");
    }

    @DisplayName("삭제할 스킬이 null이면 실패한다")
    @Test
    void failWhenRemoveSkillTargetIsNull() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                1L,
                2_000_000L,
                3_000_000L
        );

        assertThatThrownBy(() -> proposalPosition.removeSkill(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("삭제할 스킬은 필수값입니다.");
    }

    private Proposal createProposal() {
        return Proposal.create(
                createMember(),
                "제안서 제목",
                "원본 입력",
                "본문",
                1_000_000L,
                2_000_000L,
                ProposalWorkType.HYBRID,
                "서울",
                8L
        );
    }
}
