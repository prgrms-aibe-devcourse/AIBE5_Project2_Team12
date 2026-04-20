package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
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
                12L
        );
        ProposalPosition proposalPosition = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                2L,
                4_000_000L,
                6_000_000L,
                null,
                null,
                null,
                null
        );
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
                12L
        );
        ProposalPosition proposalPosition = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                2L,
                4_000_000L,
                6_000_000L,
                null,
                null,
                null,
                null
        );
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

    @DisplayName("모집 단위에서 요구 스킬을 제거하면 orphan removal로 DB에서도 삭제된다")
    @Test
    void removeSkillWithOrphanRemoval() {
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
                12L
        );
        ProposalPosition proposalPosition = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                2L,
                4_000_000L,
                6_000_000L,
                null,
                null,
                null,
                null
        );
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalPosition.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);
        proposalRepository.saveAndFlush(proposal);

        proposalPosition.removeSkill(java);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal persistedProposal = proposalRepository.findById(proposal.getId()).orElseThrow();
        ProposalPosition persistedPosition = persistedProposal.getPositions().get(0);

        assertThat(persistedPosition.getSkills()).hasSize(1);
        assertThat(persistedPosition.getSkills().get(0).getSkill().getName()).isEqualTo("Spring Boot");
        assertThat(countRows("ProposalPositionSkill")).isEqualTo(1L);
    }

    @DisplayName("같은 직무 마스터를 같은 제안서 안에 중복 저장할 수 있다")
    @Test
    void allowWhenPositionMasterIsDuplicatedWithinProposal() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));

        Proposal proposal = Proposal.create(
                member,
                "AI 기반 매칭 플랫폼 구축",
                "원본 입력",
                "최종 제안서 본문",
                10_000_000L,
                20_000_000L,
                12L
        );
        proposal.addPosition(backend, "백엔드 개발자 A", null, 1L, 3_000_000L, 4_000_000L,
                null, null, null, null);
        proposal.addPosition(backend, "백엔드 개발자 B", null, 2L, 5_000_000L, 6_000_000L,
                null, null, null, null);

        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal persistedProposal = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(persistedProposal.getPositions()).hasSize(2);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 두 단계 조회(findWithPositionsById -> findPositionsWithSkillsByProposalId) + 1차 캐시 합성 계약 검증
    // ──────────────────────────────────────────────────────────────────────────

    @DisplayName("findWithPositionsById 단독 호출 시 ProposalPosition.skills는 LAZY 미초기화 상태다")
    @Test
    void findWithPositionsById_단독_호출_시_skills는_LAZY_미초기화_상태다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", "백엔드 언어"));

        Proposal proposal = Proposal.create(
                member, "AI 매칭 플랫폼", "원문", null,
                null, null, null);
        proposal.addPosition(backend, "백엔드 개발자", null, 1L, 500_000L, 1_000_000L,
                null, null, null, null)
                .addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal found = proposalRepository.findWithPositionsById(proposal.getId()).orElseThrow();
        ProposalPosition pp = found.getPositions().get(0);

        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(pp, "skills")).isFalse();
    }

    @DisplayName("두 단계 조회 호출 시 ProposalPosition.skills는 추가 쿼리 없이 초기화된 상태다")
    @Test
    void 두_단계_조회_호출_시_skills는_추가_쿼리_없이_초기화된_상태다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", "백엔드 언어"));
        Skill spring = persist(Skill.create("Spring Boot", "웹 프레임워크"));

        Proposal proposal = Proposal.create(
                member, "AI 매칭 플랫폼", "원문", null,
                null, null, null);
        ProposalPosition pp = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                1L,
                500_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        );
        pp.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        pp.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal found = loadProposalDetail(proposal.getId());
        ProposalPosition foundPP = found.getPositions().get(0);

        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(foundPP, "skills")).isTrue();
        assertThat(foundPP.getSkills())
                .extracting(pps -> pps.getSkill().getName())
                .containsExactlyInAnyOrder("Java", "Spring Boot");
    }

    @DisplayName("두 단계 조회로 aggregate가 완성된다 — positions와 skills가 1차 캐시 합성으로 함께 반환된다")
    @Test
    void 두_단계_조회로_aggregate가_완성된다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Skill java = persist(Skill.create("Java", "백엔드 언어"));
        Skill spring = persist(Skill.create("Spring Boot", "웹 프레임워크"));

        Proposal proposal = Proposal.create(
                member, "AI 매칭 플랫폼", "원문", null,
                null, null, null);
        ProposalPosition pp = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                1L,
                500_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        );
        pp.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        pp.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal found = loadProposalDetail(proposal.getId());

        assertThat(found.getPositions()).hasSize(1);
        ProposalPosition foundPP = found.getPositions().get(0);
        assertThat(foundPP.getPosition().getName()).isEqualTo("백엔드 개발자");
        assertThat(foundPP.getSkills())
                .extracting(pps -> pps.getSkill().getName())
                .containsExactlyInAnyOrder("Java", "Spring Boot");
    }

    @DisplayName("두 단계 조회로 aggregate가 완성된다 — 복수 positions × skills 구조에서 positions 컬렉션에 중복이 없다 (DISTINCT 계약)")
    @Test
    void 두_단계_조회_복수_positions_x_skills_구조에서_positions_컬렉션에_중복이_없다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Position frontend = persist(Position.create("프론트엔드 개발자"));
        Skill java = persist(Skill.create("Java", null));
        Skill spring = persist(Skill.create("Spring Boot", null));
        Skill react = persist(Skill.create("React", null));
        Skill ts = persist(Skill.create("TypeScript", null));

        Proposal proposal = Proposal.create(
                member, "풀스택 프로젝트", "원문", null,
                null, null, null);
        ProposalPosition pp1 = proposal.addPosition(
                backend,
                "백엔드 개발자",
                null,
                1L,
                500_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        );
        pp1.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        pp1.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);
        ProposalPosition pp2 = proposal.addPosition(
                frontend,
                "프론트엔드 개발자",
                null,
                1L,
                400_000L,
                800_000L,
                null,
                null,
                null,
                null
        );
        pp2.addSkill(react, ProposalPositionSkillImportance.ESSENTIAL);
        pp2.addSkill(ts, ProposalPositionSkillImportance.PREFERENCE);
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        Proposal found = loadProposalDetail(proposal.getId());

        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(found.getPositions()).hasSize(2);
        found.getPositions().forEach(p ->
                assertThat(util.isLoaded(p, "skills")).isTrue()
        );
    }

    @DisplayName("findAllWithPositionsByMemberEmail은 해당 회원의 모든 제안서와 positions를 함께 조회한다")
    @Test
    void findAllWithPositionsByMemberEmail_positions를_함께_조회한다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));
        Position frontend = persist(Position.create("프론트엔드 개발자"));

        Proposal proposal1 = Proposal.create(member, "제안서1", "원문", null, null, null, null);
        proposal1.addPosition(backend, "백엔드", null, 1L, null, null, null, null, null, null);
        Proposal proposal2 = Proposal.create(member, "제안서2", "원문", null, null, null, null);
        proposal2.addPosition(frontend, "프론트", null, 1L, null, null, null, null, null, null);
        proposalRepository.saveAndFlush(proposal1);
        proposalRepository.saveAndFlush(proposal2);
        entityManager.clear();

        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        List<Proposal> result = proposalRepository.findAllWithPositionsByMemberEmail(
                member.getEmail().getValue());

        assertThat(result).hasSize(2);
        result.forEach(p -> assertThat(util.isLoaded(p, "positions")).isTrue());
        assertThat(result).flatExtracting(Proposal::getPositions).hasSize(2);
    }

    @DisplayName("findAllWithPositionsByMemberEmailAndStatus는 상태 필터링된 제안서와 positions를 함께 조회한다")
    @Test
    void findAllWithPositionsByMemberEmailAndStatus_상태별_positions를_함께_조회한다() {
        Member member = memberRepository.save(createMember());
        Position backend = persist(Position.create("백엔드 개발자"));

        Proposal writingProposal = Proposal.create(member, "작성중 제안서", "원문", null, null, null, null);
        writingProposal.addPosition(backend, "백엔드", null, 1L, null, null, null, null, null, null);
        proposalRepository.saveAndFlush(writingProposal);
        entityManager.clear();

        PersistenceUnitUtil util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
        List<Proposal> result = proposalRepository.findAllWithPositionsByMemberEmailAndStatus(
                member.getEmail().getValue(), com.generic4.itda.domain.proposal.ProposalStatus.WRITING);

        assertThat(result).hasSize(1);
        assertThat(util.isLoaded(result.get(0), "positions")).isTrue();
        assertThat(result.get(0).getPositions()).hasSize(1);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private Proposal loadProposalDetail(Long proposalId) {
        Proposal proposal = proposalRepository.findWithPositionsById(proposalId).orElseThrow();
        proposalRepository.findPositionsWithSkillsByProposalId(proposalId);
        return proposal;
    }

    private long countRows(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
                .getSingleResult();
    }
}
