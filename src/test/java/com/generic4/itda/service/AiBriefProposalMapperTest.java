package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.SkillRepository;
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
    private PositionRepository positionRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private AiBriefProposalMapper aiBriefProposalMapper;

    @Test
    @DisplayName("AI 브리프를 적용하면 제안서 기본 정보가 바뀌고 기존 모집 단위는 새 결과로 교체된다")
    void apply_updatesProposalFieldsAndReplacesPositions() {
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

        given(positionRepository.findByName("백엔드 개발자")).willReturn(Optional.empty());
        given(positionRepository.save(any(Position.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(skillRepository.findByName("Java")).willReturn(Optional.empty());
        given(skillRepository.save(any(Skill.class))).willAnswer(invocation -> invocation.getArgument(0));

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
        assertThat(proposal.getPositions().get(0).getPosition().getName()).isEqualTo("백엔드 개발자");
        assertThat(proposal.getPositions().get(0).getTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(proposal.getPositions().get(0).getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(proposal.getPositions().get(0).getExpectedPeriod()).isEqualTo(12L);
        assertThat(proposal.getPositions().get(0).getCareerMinYears()).isEqualTo(3);
        assertThat(proposal.getPositions().get(0).getCareerMaxYears()).isEqualTo(6);
        assertThat(proposal.getPositions().get(0).getWorkPlace()).isEqualTo("판교");
        assertThat(proposal.getPositions().get(0).getSkills()).hasSize(1);
        assertThat(proposal.getPositions().get(0).getSkills().get(0).getSkill().getName()).isEqualTo("Java");
        assertThat(proposal.getPositions().get(0).getSkills().get(0).getImportance())
                .isEqualTo(ProposalPositionSkillImportance.PREFERENCE);
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
    @DisplayName("AI 응답에 값이 없으면 기존 제안서 정보와 모집 단위를 유지한다")
    void apply_keepsExistingFieldsWhenAiReturnsNullOrEmpty() {
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
        assertThat(proposal.getPositions()).hasSize(1);
        assertThat(proposal.getPositions().get(0).getPosition().getName()).isEqualTo("디자이너");
        assertThat(proposal.getPositions().get(0).getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(tuple("Figma", ProposalPositionSkillImportance.ESSENTIAL));
    }

    @Test
    @DisplayName("동일 이름의 직무와 스킬이 이미 있으면 기존 마스터를 재사용한다")
    void apply_reusesExistingPositionAndSkillWhenFoundByName() {
        Proposal proposal = createProposal();
        Position backend = Position.create("백엔드 개발자");
        Skill java = Skill.create("Java", null);

        given(positionRepository.findByName("백엔드 개발자")).willReturn(Optional.of(backend));
        given(skillRepository.findByName("Java")).willReturn(Optional.of(java));

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
        then(positionRepository).should(never()).save(any(Position.class));
        then(skillRepository).should(never()).save(any(Skill.class));
    }

    private Proposal createProposal() {
        return Proposal.create(
                createMember(),
                "제안서 제목",
                "원본 입력",
                "기존 설명",
                1_000_000L,
                2_000_000L,
                3L
        );
    }
}
