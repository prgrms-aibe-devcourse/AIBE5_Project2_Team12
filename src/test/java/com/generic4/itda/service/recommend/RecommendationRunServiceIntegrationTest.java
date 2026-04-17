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
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.RecommendationRunRepository;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class RecommendationRunServiceIntegrationTest {

    private static final RecommendationAlgorithm DEFAULT_ALGORITHM = RecommendationAlgorithm.HEURISTIC_V1;
    private static final int DEFAULT_TOP_K = 3;

    @Autowired
    private RecommendationRunService recommendationRunService;

    @Autowired
    private RecommendationFingerprintGenerator fingerprintGenerator;

    @Autowired
    private RecommendationRunRepository recommendationRunRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("같은 proposalPosition과 fingerprint 조합이면 recommendation run을 재사용한다")
    void createOrReuse_reusesPersistedRunWhenRequestIsSame() {
        Member owner = memberRepository.save(createMember("run-owner@test.com", "pw", "제안자", "010-0000-5555"));
        Position backend = persist(Position.create("런 백엔드 개발자"));
        Skill java = persist(Skill.create("Run Java", null));
        Skill spring = persist(Skill.create("Run Spring", null));

        Proposal proposal = Proposal.create(
                owner,
                "추천 실행 제안서",
                "원본 입력",
                "설명",
                5_000_000L,
                8_000_000L,
                ProposalWorkType.REMOTE,
                "판교",
                4L
        );
        ProposalPosition proposalPosition = proposal.addPosition(backend, 2L, 1_500_000L, 2_500_000L);
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposalPosition.addSkill(spring, ProposalPositionSkillImportance.PREFERENCE);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);

        Long proposalId = proposal.getId();
        Long proposalPositionId = proposalPosition.getId();
        String ownerEmail = owner.getEmail().getValue();

        entityManager.clear();

        Long firstRunId = recommendationRunService.createOrReuse(proposalId, proposalPositionId, ownerEmail);
        entityManager.flush();
        entityManager.clear();

        Long reusedRunId = recommendationRunService.createOrReuse(proposalId, proposalPositionId, ownerEmail);
        entityManager.flush();
        entityManager.clear();

        RecommendationRun persistedRun = recommendationRunRepository.findById(firstRunId).orElseThrow();
        Proposal persistedProposal = loadProposalDetail(proposalId);
        ProposalPosition persistedPosition = findPosition(persistedProposal, proposalPositionId);
        String expectedFingerprint = fingerprintGenerator.generate(
                persistedProposal,
                persistedPosition,
                DEFAULT_ALGORITHM,
                DEFAULT_TOP_K
        );

        assertThat(reusedRunId).isEqualTo(firstRunId);
        assertThat(recommendationRunRepository.count()).isEqualTo(1);
        assertThat(persistedRun.getProposalPosition().getId()).isEqualTo(proposalPositionId);
        assertThat(persistedRun.getRequestFingerprint()).isEqualTo(expectedFingerprint);
        assertThat(persistedRun.getAlgorithm()).isEqualTo(DEFAULT_ALGORITHM);
        assertThat(persistedRun.getTopK()).isEqualTo(DEFAULT_TOP_K);
        assertThat(persistedRun.getStatus()).isEqualTo(RecommendationRunStatus.PENDING);
    }

    @Test
    @DisplayName("fingerprint 입력이 바뀌면 새 recommendation run을 생성한다")
    void createOrReuse_createsNewRunWhenFingerprintInputChanges() {
        Member owner = memberRepository.save(createMember("run-change@test.com", "pw", "제안자", "010-0000-6666"));
        Position backend = persist(Position.create("런 변경 백엔드"));
        Skill java = persist(Skill.create("Run Change Java", null));

        Proposal proposal = Proposal.create(
                owner,
                "초기 제목",
                "원본 입력",
                "설명",
                5_000_000L,
                8_000_000L,
                ProposalWorkType.HYBRID,
                "서울",
                5L
        );
        ProposalPosition proposalPosition = proposal.addPosition(backend, 1L, 1_000_000L, 2_000_000L);
        proposalPosition.addSkill(java, ProposalPositionSkillImportance.ESSENTIAL);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);

        Long proposalId = proposal.getId();
        Long proposalPositionId = proposalPosition.getId();
        String ownerEmail = owner.getEmail().getValue();

        entityManager.clear();

        Long firstRunId = recommendationRunService.createOrReuse(proposalId, proposalPositionId, ownerEmail);
        entityManager.flush();
        entityManager.clear();

        Proposal editableProposal = proposalRepository.findById(proposalId).orElseThrow();
        editableProposal.update(
                "수정된 제목",
                editableProposal.getRawInputText(),
                editableProposal.getDescription(),
                editableProposal.getTotalBudgetMin(),
                editableProposal.getTotalBudgetMax(),
                editableProposal.getWorkType(),
                editableProposal.getWorkPlace(),
                editableProposal.getExpectedPeriod()
        );
        proposalRepository.saveAndFlush(editableProposal);
        entityManager.clear();

        Long secondRunId = recommendationRunService.createOrReuse(proposalId, proposalPositionId, ownerEmail);
        entityManager.flush();
        entityManager.clear();

        RecommendationRun firstRun = recommendationRunRepository.findById(firstRunId).orElseThrow();
        RecommendationRun secondRun = recommendationRunRepository.findById(secondRunId).orElseThrow();

        assertThat(secondRunId).isNotEqualTo(firstRunId);
        assertThat(recommendationRunRepository.count()).isEqualTo(2);
        assertThat(firstRun.getRequestFingerprint()).isNotEqualTo(secondRun.getRequestFingerprint());
    }

    @Test
    @DisplayName("저장된 모집 포지션이 OPEN 상태가 아니면 recommendation run 생성을 거부한다")
    void createOrReuse_rejectsNonOpenPosition() {
        Member owner = memberRepository.save(createMember("run-closed@test.com", "pw", "제안자", "010-0000-7777"));
        Position backend = persist(Position.create("런 종료 백엔드"));

        Proposal proposal = Proposal.create(
                owner,
                "닫힌 포지션 제안서",
                "원본 입력",
                null,
                null,
                null,
                ProposalWorkType.REMOTE,
                null,
                null
        );
        ProposalPosition proposalPosition = proposal.addPosition(backend, 1L, null, null);
        proposalPosition.changeStatus(ProposalPositionStatus.FULL);
        proposal.startMatching();
        proposalRepository.saveAndFlush(proposal);
        entityManager.clear();

        assertThatThrownBy(() -> recommendationRunService.createOrReuse(
                proposal.getId(),
                proposalPosition.getId(),
                owner.getEmail().getValue()
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OPEN 상태의 모집 포지션만 추천을 실행할 수 있습니다.");

        assertThat(recommendationRunRepository.count()).isZero();
    }

    private Proposal loadProposalDetail(Long proposalId) {
        Proposal proposal = proposalRepository.findWithPositionsById(proposalId).orElseThrow();
        proposalRepository.findPositionsWithSkillsByProposalId(proposalId);
        return proposal;
    }

    private ProposalPosition findPosition(Proposal proposal, Long proposalPositionId) {
        return proposal.getPositions().stream()
                .filter(position -> position.getId().equals(proposalPositionId))
                .min(Comparator.comparing(ProposalPosition::getId))
                .orElseThrow();
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
