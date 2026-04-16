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
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
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
    @DisplayName("markCompleted() 후 HardFilterStat와 candidateCount가 DB round-trip 된다")
    void markCompleted_후_HardFilterStat와_candidateCount가_DB_round_trip된다() {
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

    @Test
    @DisplayName("markFailed() 후 status와 errorMessage가 DB round-trip 된다")
    void markFailed_후_status와_errorMessage가_DB_round_trip된다() {
        // given
        RecommendationRun run = recommendationRunRepository.saveAndFlush(
                RecommendationRun.create(proposalPosition, "fp-abc123", RecommendationAlgorithm.HEURISTIC_V1, 5));
        run.markRunning();
        String errorMessage = "AI 서버 타임아웃";

        // when
        run.markFailed(errorMessage);
        recommendationRunRepository.saveAndFlush(run);
        em.clear();

        // then
        RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.FAILED);
        assertThat(found.getErrorMessage()).isEqualTo(errorMessage);
    }

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }
}
