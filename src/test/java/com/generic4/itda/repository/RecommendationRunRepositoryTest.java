package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@RepositoryTest
class RecommendationRunRepositoryTest {

    @Autowired
    private RecommendationRunRepository recommendationRunRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private EntityManager em;

    private ProposalPosition proposalPosition;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(createMember());
        Position position = persistPosition("백엔드 개발자");

        Proposal proposal = Proposal.create(
                member, "AI 매칭 플랫폼 구축", "원문 내용", null,
                null, null, ProposalWorkType.REMOTE, null, null);
        proposalPosition = proposal.addPosition(position, 2L, 500_000L, 1_000_000L);
        proposalRepository.saveAndFlush(proposal);
    }

    @Test
    @DisplayName("저장 후 ID로 조회하면 동일한 엔티티가 반환된다")
    void 저장_후_ID로_조회하면_동일한_엔티티가_반환된다() {
        // given
        RecommendationRun run = RecommendationRun.create(
                proposalPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5);

        // when
        recommendationRunRepository.saveAndFlush(run);
        em.clear();

        // then
        RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();
        assertThat(found.getProposalPosition().getId()).isEqualTo(proposalPosition.getId());
        assertThat(found.getRequestFingerprint()).isEqualTo("fp-abc123");
        assertThat(found.getAlgorithm()).isEqualTo(RecommendationAlgorithm.HEURISTIC_V1);
        assertThat(found.getTopK()).isEqualTo(5);
        assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.PENDING);
        assertThat(found.getCandidateCount()).isNull();
        assertThat(found.getHardFilterStats()).isNull();
        assertThat(found.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("markRunning() 후 변경사항이 DB에 반영된다")
    void markRunning_후_변경사항이_DB에_반영된다() {
        // given
        RecommendationRun run = recommendationRunRepository.saveAndFlush(
                RecommendationRun.create(proposalPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5));

        // when
        run.markRunning();
        recommendationRunRepository.saveAndFlush(run);
        em.clear();

        // then
        RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.RUNNING);
    }

    @Test
    @DisplayName("markCompleted() 후 status, candidateCount, hardFilterStats가 DB에 반영된다")
    void markCompleted_후_변경사항이_DB에_반영된다() {
        // given
        RecommendationRun run = recommendationRunRepository.saveAndFlush(
                RecommendationRun.create(proposalPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5));
        run.markRunning();
        HardFilterStat stat = new HardFilterStat(10, 8, 6, 3);

        // when
        run.markCompleted(stat);
        recommendationRunRepository.saveAndFlush(run);
        em.clear();

        // then
        RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.COMPUTED);
        assertThat(found.getCandidateCount()).isEqualTo(3);
        assertThat(found.getHardFilterStats()).isEqualTo(stat);
    }

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }
}
