package com.generic4.itda.service;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefPositionResult;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.dto.proposal.AiBriefSkillResult;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.SkillRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class ProposalAiBriefServiceIntegrationTest {

    @Autowired
    private ProposalAiBriefService proposalAiBriefService;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SkillRepository skillRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private AiBriefGenerator aiBriefGenerator;

    @Test
    @DisplayName("AI 브리프를 생성하면 제안서와 하위 모집 단위가 DB에 반영된다")
    void generatePersistsProposalAndNestedPositions() {
        Member member = memberRepository.save(
                createMember("owner@example.com", "hashed-password", "소유자", "010-1234-5678")
        );
        Position oldPosition = positionRepository.save(Position.create("디자이너"));
        Skill oldSkill = skillRepository.save(Skill.create("Figma", null));

        Proposal proposal = Proposal.create(
                member,
                "기존 제목",
                "백엔드 개발자 1명과 프론트엔드 개발자 1명이 필요한 프로젝트입니다.",
                "기존 설명",
                1_000_000L,
                2_000_000L,
                ProposalWorkType.SITE,
                "서울",
                4L
        );
        ProposalPosition existingPosition = proposal.addPosition(oldPosition, 1L, 500_000L, 700_000L);
        existingPosition.addSkill(oldSkill, ProposalPositionSkillImportance.ESSENTIAL);
        Proposal savedProposal = proposalRepository.save(proposal);

        AiBriefResult aiBriefResult = AiBriefResult.of(
                "웹 서비스 개발 외주(백엔드1·프론트1)",
                "웹 서비스 개발 외주입니다. 백엔드 개발자 1명과 프론트엔드 개발자 1명을 12주 동안 채용합니다.",
                8_000_000L,
                8_000_000L,
                ProposalWorkType.HYBRID,
                "판교",
                12L,
                List.of(
                        AiBriefPositionResult.of(
                                "백엔드 개발자",
                                1L,
                                null,
                                null,
                                List.of(
                                        AiBriefSkillResult.of("Java", ProposalPositionSkillImportance.ESSENTIAL),
                                        AiBriefSkillResult.of("Spring Boot",
                                                ProposalPositionSkillImportance.ESSENTIAL)
                                )
                        ),
                        AiBriefPositionResult.of(
                                "프론트엔드 개발자",
                                1L,
                                null,
                                null,
                                List.of(AiBriefSkillResult.of("React", null))
                        )
                )
        );
        given(aiBriefGenerator.generate(any(AiBriefGenerateRequest.class))).willReturn(aiBriefResult);

        AiBriefResult generated = proposalAiBriefService.generate(savedProposal.getId(), member.getEmail().getValue());

        entityManager.flush();
        entityManager.clear();

        Proposal persistedProposal = proposalRepository.findById(savedProposal.getId()).orElseThrow();
        Map<String, ProposalPosition> positionsByName = persistedProposal.getPositions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        proposalPosition -> proposalPosition.getPosition().getName(),
                        Function.identity()
                ));

        assertThat(generated.getTitle()).isEqualTo("웹 서비스 개발 외주(백엔드1·프론트1)");
        assertThat(persistedProposal.getTitle()).isEqualTo("웹 서비스 개발 외주(백엔드1·프론트1)");
        assertThat(persistedProposal.getRawInputText()).isEqualTo("백엔드 개발자 1명과 프론트엔드 개발자 1명이 필요한 프로젝트입니다.");
        assertThat(persistedProposal.getDescription())
                .isEqualTo("웹 서비스 개발 외주입니다. 백엔드 개발자 1명과 프론트엔드 개발자 1명을 12주 동안 채용합니다.");
        assertThat(persistedProposal.getTotalBudgetMin()).isEqualTo(8_000_000L);
        assertThat(persistedProposal.getTotalBudgetMax()).isEqualTo(8_000_000L);
        assertThat(persistedProposal.getWorkType()).isEqualTo(ProposalWorkType.HYBRID);
        assertThat(persistedProposal.getWorkPlace()).isEqualTo("판교");
        assertThat(persistedProposal.getExpectedPeriod()).isEqualTo(12L);
        assertThat(persistedProposal.getPositions()).hasSize(2);
        assertThat(positionsByName).doesNotContainKey("디자이너");

        ProposalPosition backendPosition = positionsByName.get("백엔드 개발자");
        assertThat(backendPosition).isNotNull();
        assertThat(backendPosition.getHeadCount()).isEqualTo(1L);
        assertThat(backendPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("Java", ProposalPositionSkillImportance.ESSENTIAL),
                        tuple("Spring Boot", ProposalPositionSkillImportance.ESSENTIAL)
                );

        ProposalPosition frontendPosition = positionsByName.get("프론트엔드 개발자");
        assertThat(frontendPosition).isNotNull();
        assertThat(frontendPosition.getHeadCount()).isEqualTo(1L);
        assertThat(frontendPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName(), skill -> skill.getImportance())
                .containsExactly(
                        tuple("React", ProposalPositionSkillImportance.PREFERENCE)
                );
    }
}
