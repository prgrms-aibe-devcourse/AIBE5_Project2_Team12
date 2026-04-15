package com.generic4.itda.domain.proposal;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.skill.Skill;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProposalPositionSkillTest {

    @DisplayName("유효한 입력이 주어지면 모집 단위 요구 스킬을 생성한다")
    @Test
    void createWithValidInputs() {
        ProposalPosition proposalPosition = createProposalPosition();
        Skill skill = Skill.create("Java", "백엔드 언어");

        ProposalPositionSkill proposalPositionSkill = ProposalPositionSkill.create(
                proposalPosition,
                skill,
                ProposalPositionSkillImportance.ESSENTIAL
        );

        assertThat(proposalPositionSkill.getProposalPosition()).isEqualTo(proposalPosition);
        assertThat(proposalPositionSkill.getSkill()).isEqualTo(skill);
        assertThat(proposalPositionSkill.getImportance()).isEqualTo(ProposalPositionSkillImportance.ESSENTIAL);
    }

    @DisplayName("모집 단위가 없으면 생성에 실패한다")
    @Test
    void failWhenProposalPositionIsNull() {
        assertThatThrownBy(() -> ProposalPositionSkill.create(
                null,
                Skill.create("Java", "백엔드 언어"),
                ProposalPositionSkillImportance.ESSENTIAL
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모집 단위는 필수값입니다.");
    }

    @DisplayName("스킬이 없으면 생성에 실패한다")
    @Test
    void failWhenSkillIsNull() {
        assertThatThrownBy(() -> ProposalPositionSkill.create(
                createProposalPosition(),
                null,
                ProposalPositionSkillImportance.ESSENTIAL
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수값입니다.");
    }

    @DisplayName("중요도가 없으면 기본값으로 PREFERENCE를 사용한다")
    @Test
    void createWithDefaultImportanceWhenNull() {
        ProposalPositionSkill proposalPositionSkill = ProposalPositionSkill.create(
                createProposalPosition(),
                Skill.create("Java", "백엔드 언어"),
                null
        );

        assertThat(proposalPositionSkill.getImportance()).isEqualTo(ProposalPositionSkillImportance.PREFERENCE);
    }

    @DisplayName("중요도를 null로 변경하면 기본값으로 PREFERENCE를 사용한다")
    @Test
    void changeImportanceToDefaultWhenNull() {
        ProposalPositionSkill proposalPositionSkill = ProposalPositionSkill.create(
                createProposalPosition(),
                Skill.create("Java", "백엔드 언어"),
                ProposalPositionSkillImportance.ESSENTIAL
        );

        proposalPositionSkill.changeImportance(null);

        assertThat(proposalPositionSkill.getImportance()).isEqualTo(ProposalPositionSkillImportance.PREFERENCE);
    }

    private ProposalPosition createProposalPosition() {
        Proposal proposal = Proposal.create(
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

        return proposal.addPosition(Position.create("백엔드 개발자"), 1L, 2_000_000L, 3_000_000L);
    }
}
