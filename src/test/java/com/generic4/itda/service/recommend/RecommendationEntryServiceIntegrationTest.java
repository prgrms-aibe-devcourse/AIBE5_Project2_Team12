package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.annotation.IntegrationTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.recommend.RecommendationEntryPositionItem;
import com.generic4.itda.dto.recommend.RecommendationEntryViewModel;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class RecommendationEntryServiceIntegrationTest {

    @Autowired
    private RecommendationEntryService recommendationEntryService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("저장된 recommendation aggregate를 조회해 entry ViewModel을 조립한다")
    void getEntry_assemblesViewModelFromPersistedAggregate() {
        Member owner = memberRepository.save(createMember("entry-owner@test.com", "pw", "제안자", "010-0000-1111"));
        Position designer = persist(Position.create("Entry UI/UX 디자이너"));
        Position backend = persist(Position.create("Entry 백엔드 개발자"));
        Position frontend = persist(Position.create("Entry 프론트엔드 개발자"));
        Skill figma = persist(Skill.create("Entry Figma", null));
        Skill java = persist(Skill.create("Entry Java", null));
        Skill react = persist(Skill.create("Entry React", null));

        Proposal proposal = Proposal.create(
                owner,
                "AI 매칭 플랫폼 구축",
                "원본 입력",
                "최종 제안서 설명",
                10_000_000L,
                20_000_000L,
                6L
        );
        ProposalPosition designerPosition = proposal.addPosition(designer, "프로덕트 디자이너", null, 1L, null, null, null, null, null, null);
        designerPosition.addSkill(figma, ProposalPositionSkillImportance.PREFERENCE);
        ProposalPosition backendPosition = proposal.addPosition(backend, "Node.js 백엔드 개발자", null, 2L, 1_000_000L, 2_000_000L, 5L, null, null, null);
        backendPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        ProposalPosition frontendPosition = proposal.addPosition(frontend, "프론트엔드 개발자", null, 1L, 800_000L, 1_200_000L, 4L, null, null, null);
        frontendPosition.addSkill(react, ProposalPositionSkillImportance.PREFERENCE);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);

        Long proposalId = proposal.getId();
        Long designerPositionId = designerPosition.getId();
        Long backendPositionId = backendPosition.getId();
        Long frontendPositionId = frontendPosition.getId();

        entityManager.clear();

        RecommendationEntryViewModel result = recommendationEntryService.getEntry(
                proposalId,
                owner.getEmail().getValue()
        );

        assertThat(result.proposalId()).isEqualTo(proposalId);
        assertThat(result.proposalTitle()).isEqualTo("AI 매칭 플랫폼 구축");
        assertThat(result.proposalStatus()).isEqualTo("모집/추천 진행 중");
        assertThat(result.runnable()).isTrue();
        assertThat(result.helperMessage()).isEqualTo("선택한 포지션 기준으로 추천을 시작할 수 있습니다.");
        assertThat(result.selectedProposalPositionId()).isEqualTo(designerPositionId);
        assertThat(result.positions())
                .extracting(RecommendationEntryPositionItem::proposalPositionId)
                .containsExactly(designerPositionId, backendPositionId, frontendPositionId);

        RecommendationEntryPositionItem first = result.positions().get(0);
        assertThat(first.positionTitle()).isEqualTo("프로덕트 디자이너");
        assertThat(first.positionCategoryName()).isEqualTo("Entry UI/UX 디자이너");
        assertThat(first.budgetText()).isEqualTo("예산 미정");
        assertThat(first.skills())
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.skillName()).isEqualTo("Entry Figma");
                    assertThat(skill.importanceLabel()).isEqualTo("우대");
                });

        RecommendationEntryPositionItem second = result.positions().get(1);
        assertThat(second.positionTitle()).isEqualTo("Node.js 백엔드 개발자");
        assertThat(second.positionCategoryName()).isEqualTo("Entry 백엔드 개발자");
        assertThat(second.budgetText()).isEqualTo("1,000,000 ~ 2,000,000");
        assertThat(second.expectedPeriod()).isEqualTo(5L);
        assertThat(second.skills())
                .singleElement()
                .satisfies(skill -> {
                    assertThat(skill.skillName()).isEqualTo("Entry Java");
                    assertThat(skill.importanceLabel()).isEqualTo("필수");
                });
    }

    @Test
    @DisplayName("저장된 제안서에 모집 포지션이 없으면 빈 positions와 null 선택값을 반환한다")
    void getEntry_returnsEmptyPositionsWhenProposalHasNoPositions() {
        Member owner = memberRepository.save(createMember("entry-empty@test.com", "pw", "제안자", "010-0000-2222"));
        Proposal proposal = Proposal.create(
                owner,
                "포지션 없는 제안서",
                "원본 입력",
                null,
                null,
                null,
                null
        );
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        RecommendationEntryViewModel result = recommendationEntryService.getEntry(
                proposal.getId(),
                owner.getEmail().getValue()
        );

        assertThat(result.runnable()).isFalse();
        assertThat(result.positions()).isEmpty();
        assertThat(result.selectedProposalPositionId()).isNull();
    }

    @Test
    @DisplayName("저장된 제안서의 소유자가 아니면 entry 조회를 거부한다")
    void getEntry_rejectsNonOwner() {
        Member owner = memberRepository.save(createMember("entry-real-owner@test.com", "pw", "소유자", "010-0000-3333"));
        Member other = memberRepository.save(createMember("entry-other@test.com", "pw", "다른회원", "010-0000-4444"));
        Position backend = persist(Position.create("Entry 권한체크 백엔드"));

        Proposal proposal = Proposal.create(
                owner,
                "권한 체크용 제안서",
                "원본 입력",
                null,
                null,
                null,
                null
        );
        proposal.addPosition(backend, "통합 백엔드", null, 1L, null, null, null, null, null, null);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        assertThatThrownBy(() -> recommendationEntryService.getEntry(proposal.getId(), other.getEmail().getValue()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("본인 제안서만 조회할 수 있습니다.");
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
