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

        ProposalPosition proposalPosition = ProposalPosition.create(
                proposal,
                position,
                "백엔드 개발자",
                null,
                2L,
                3_000_000L,
                5_000_000L,
                null,
                null,
                null,
                null
        );

        assertThat(proposalPosition.getProposal()).isEqualTo(proposal);
        assertThat(proposalPosition.getPosition()).isEqualTo(position);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(2L);
        assertThat(proposalPosition.getUnitBudgetMin()).isEqualTo(3_000_000L);
        assertThat(proposalPosition.getUnitBudgetMax()).isEqualTo(5_000_000L);
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
        assertThat(proposal.getPositions()).isEmpty();
    }

    @DisplayName("상위 제안서가 없으면 생성에 실패한다")
    @Test
    void failWhenProposalIsNull() {
        assertThatThrownBy(() -> ProposalPosition.create(
                null,
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                null,
                1L,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                1L,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                headCount,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                1L,
                5_000_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("1인 기준 최소 예산은 최대 예산보다 클 수 없습니다.");
    }

    @DisplayName("유효한 입력이면 모집 단위를 수정한다")
    @Test
    void updateProposalPosition() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                null,
                1L,
                2_000_000L,
                3_000_000L,
                null,
                null,
                null,
                null
        );
        Position updatedPosition = Position.create("프론트엔드 개발자");

        proposalPosition.update(updatedPosition, "프론트엔드 개발자", null, 3L, 4_000_000L, 6_000_000L,
                null, null, null, null);

        assertThat(proposalPosition.getPosition()).isEqualTo(updatedPosition);
        assertThat(proposalPosition.getHeadCount()).isEqualTo(3L);
        assertThat(proposalPosition.getUnitBudgetMin()).isEqualTo(4_000_000L);
        assertThat(proposalPosition.getUnitBudgetMax()).isEqualTo(6_000_000L);
        assertThat(proposalPosition.getStatus()).isEqualTo(ProposalPositionStatus.OPEN);
    }

    @DisplayName("같은 제안서 안에 이미 존재하는 직무로도 모집 단위를 수정할 수 있다")
    @Test
    void allowWhenUpdatePositionToDuplicatedPosition() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Position frontend = Position.create("프론트엔드 개발자");
        proposal.addPosition(backend, "백엔드 개발자 A", null, 1L, 2_000_000L, 3_000_000L, null, null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(frontend, "프론트엔드 개발자", null, 1L, 2_000_000L,
                3_000_000L, null, null, null, null);

        proposalPosition.update(backend, "백엔드 개발자 B", null, 2L, 3_000_000L, 4_000_000L,
                null, null, null, null);

        assertThat(proposalPosition.getPosition()).isEqualTo(backend);
        assertThat(proposal.getPositions()).hasSize(2);
    }

    @DisplayName("모집 상태를 변경할 수 있다")
    @Test
    void changeStatus() {
        ProposalPosition proposalPosition = ProposalPosition.create(
                createProposal(),
                Position.create("백엔드 개발자"),
                "백엔드 개발자",
                null,
                1L,
                2_000_000L,
                3_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                1L,
                2_000_000L,
                3_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                1L,
                2_000_000L,
                3_000_000L,
                null,
                null,
                null,
                null
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
                "백엔드 개발자",
                null,
                1L,
                2_000_000L,
                3_000_000L,
                null,
                null,
                null,
                null
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
                8L
        );
    }
}
