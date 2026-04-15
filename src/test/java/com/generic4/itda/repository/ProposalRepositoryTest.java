package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@H2RepositoryTest
class ProposalRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("제안서 aggregate를 저장하면 모집 단위와 요구 스킬까지 함께 저장된다")
    @Test
    void saveProposalAggregate() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", "백엔드 언어"));
        Skill spring = persist(Skill.create("Spring Boot", "웹 프레임워크"));

        Proposal proposal = Proposal.create(
                member,
                "AI 기반 매칭 플랫폼 구축",
                "원본 입력",
                "최종 제안서 본문",
                10_000_000L,
                20_000_000L,
                ProposalWorkType.HYBRID,
                "서울",
                12L
        );
        ProposalPosition proposalPosition = proposal.addPosition(backend, 2L, 4_000_000L, 6_000_000L);
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalPosition.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);

        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal persistedProposal = proposalRepository.findById(proposal.getId()).orElseThrow();

        assertThat(persistedProposal.getPositions()).hasSize(1);
        ProposalPosition persistedPosition = persistedProposal.getPositions().get(0);
        assertThat(persistedPosition.getPosition().getName()).isEqualTo("백엔드 개발자");
        assertThat(persistedPosition.getSkills()).hasSize(2);
        assertThat(persistedPosition.getSkills())
                .extracting(skill -> skill.getSkill().getName())
                .containsExactly("Java", "Spring Boot");
    }

    @DisplayName("제안서에서 모집 단위를 제거하면 하위 요구 스킬까지 orphan removal 된다")
    @Test
    void removePositionWithOrphanRemoval() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", "백엔드 언어"));

        Proposal proposal = Proposal.create(
                member,
                "AI 기반 매칭 플랫폼 구축",
                "원본 입력",
                "최종 제안서 본문",
                10_000_000L,
                20_000_000L,
                ProposalWorkType.HYBRID,
                "서울",
                12L
        );
        ProposalPosition proposalPosition = proposal.addPosition(backend, 2L, 4_000_000L, 6_000_000L);
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalRepository.saveAndFlush(proposal);

        proposal.removePosition(proposalPosition);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal persistedProposal = proposalRepository.findById(proposal.getId()).orElseThrow();

        assertThat(persistedProposal.getPositions()).isEmpty();
        assertThat(countRows("ProposalPosition")).isZero();
        assertThat(countRows("ProposalPositionSkill")).isZero();
    }

    @DisplayName("같은 직무 마스터를 같은 제안서 안에 중복 저장할 수 없다")
    @Test
    void failWhenPositionMasterIsDuplicatedWithinProposal() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));

        Proposal proposal = Proposal.create(
                member,
                "AI 기반 매칭 플랫폼 구축",
                "원본 입력",
                "최종 제안서 본문",
                10_000_000L,
                20_000_000L,
                ProposalWorkType.HYBRID,
                "서울",
                12L
        );
        proposal.addPosition(backend, 1L, 3_000_000L, 4_000_000L);

        assertThatThrownBy(() -> proposal.addPosition(backend, 2L, 5_000_000L, 6_000_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("같은 제안서에는 동일한 직무를 중복 등록할 수 없습니다.");
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private long countRows(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
                .getSingleResult();
    }
}
