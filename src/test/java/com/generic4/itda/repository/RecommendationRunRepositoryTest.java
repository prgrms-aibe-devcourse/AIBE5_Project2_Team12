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
import jakarta.persistence.PersistenceUnitUtil;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

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

    private Proposal proposal;
    private ProposalPosition proposalPosition;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(createMember());
        Position position = persistPosition("백엔드 개발자");

        proposal = Proposal.create(
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

    @Nested
    @DisplayName("findByProposalPosition_IdAndRequestFingerprintAndAlgorithm")
    class FindByCompositeKey {

        private static final String FP = "fp-target";
        private static final RecommendationAlgorithm ALG = RecommendationAlgorithm.HEURISTIC_V1;

        @BeforeEach
        void saveRun() {
            recommendationRunRepository.saveAndFlush(
                    RecommendationRun.create(proposalPosition, FP, ALG, 5));
            em.clear();
        }

        @Test
        @DisplayName("세 필드가 모두 일치하면 해당 실행 이력이 조회된다")
        void 세_필드가_모두_일치하면_조회된다() {
            Optional<RecommendationRun> result =
                    recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                            proposalPosition.getId(), FP, ALG);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("requestFingerprint가 다르면 조회되지 않는다")
        void requestFingerprint가_다르면_조회되지_않는다() {
            Optional<RecommendationRun> result =
                    recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                            proposalPosition.getId(), "fp-other", ALG);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("algorithm이 다르면 조회되지 않는다")
        void algorithm이_다르면_조회되지_않는다() {
            Optional<RecommendationRun> result =
                    recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                            proposalPosition.getId(), FP, RecommendationAlgorithm.VECTOR_ENGINE_V1);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("proposalPositionId가 다르면 조회되지 않는다")
        void proposalPositionId가_다르면_조회되지_않는다() {
            Position otherPosition = persistPosition("프론트엔드 개발자");
            ProposalPosition otherPP = proposal.addPosition(otherPosition, 1L, 300_000L, 600_000L);
            proposalRepository.saveAndFlush(proposal);

            Optional<RecommendationRun> result =
                    recommendationRunRepository.findByProposalPosition_IdAndRequestFingerprintAndAlgorithm(
                            otherPP.getId(), FP, ALG);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findDetailById")
    class FindDetailById {

        private RecommendationRun run;

        @BeforeEach
        void saveRun() {
            run = recommendationRunRepository.saveAndFlush(
                    RecommendationRun.create(proposalPosition, "fp-detail", RecommendationAlgorithm.HEURISTIC_V1, 5));
            em.clear();
        }

        @Test
        @DisplayName("run, proposalPosition, proposal, member를 한 번에 조회한다")
        void run과_연결된_상위_그래프를_함께_조회한다() {
            // when
            Optional<RecommendationRun> result = recommendationRunRepository.findDetailById(run.getId());

            // then
            assertThat(result).isPresent();

            RecommendationRun found = result.orElseThrow();
            PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();

            assertThat(util.isLoaded(found, "proposalPosition")).isTrue();
            assertThat(found.getProposalPosition().getId()).isEqualTo(proposalPosition.getId());

            assertThat(util.isLoaded(found.getProposalPosition(), "proposal")).isTrue();
            assertThat(found.getProposalPosition().getProposal().getId()).isEqualTo(proposal.getId());

            assertThat(util.isLoaded(found.getProposalPosition().getProposal(), "member")).isTrue();
            assertThat(found.getProposalPosition().getProposal().getMember().getId())
                    .isEqualTo(proposal.getMember().getId());
        }

        @Test
        @DisplayName("쿼리에 포함되지 않은 연관은 LAZY 상태로 남는다")
        void 쿼리에_포함되지_않은_연관은_LAZY_상태로_남는다() {
            // when
            RecommendationRun found = recommendationRunRepository.findDetailById(run.getId()).orElseThrow();

            // then
            PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();

            assertThat(util.isLoaded(found.getProposalPosition(), "position")).isFalse();
            assertThat(util.isLoaded(found.getProposalPosition().getProposal(), "positions")).isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 id면 빈 Optional을 반환한다")
        void 존재하지_않는_id면_빈_Optional을_반환한다() {
            // when
            Optional<RecommendationRun> result = recommendationRunRepository.findDetailById(Long.MAX_VALUE);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findPendingRunIds")
    class FindPendingRunIds {

        @Test
        @DisplayName("PENDING 상태인 run id만 반환된다")
        void PENDING_상태인_run_id만_반환된다() {
            RecommendationRun pending = saveRunWithStatus(RecommendationRunStatus.PENDING);
            saveRunWithStatus(RecommendationRunStatus.RUNNING);
            saveRunWithStatus(RecommendationRunStatus.COMPUTED);
            saveRunWithStatus(RecommendationRunStatus.FAILED);
            em.clear();

            List<Long> ids = recommendationRunRepository.findPendingRunIds(PageRequest.of(0, 100));

            assertThat(ids).containsExactly(pending.getId());
        }

        @Test
        @DisplayName("id asc 순으로 정렬되어 반환된다")
        void id_asc_순으로_정렬되어_반환된다() {
            RecommendationRun first = saveRunWithStatus(RecommendationRunStatus.PENDING);
            RecommendationRun second = recommendationRunRepository.saveAndFlush(
                    RecommendationRun.create(proposalPosition, "fp-second", RecommendationAlgorithm.VECTOR_ENGINE_V1,
                            5));
            em.clear();

            List<Long> ids = recommendationRunRepository.findPendingRunIds(PageRequest.of(0, 100));

            assertThat(ids).containsExactly(first.getId(), second.getId());
        }

        @Test
        @DisplayName("PageRequest size 1 적용 시 1건만 반환된다")
        void PageRequest_size_1_적용_시_1건만_반환된다() {
            saveRunWithStatus(RecommendationRunStatus.PENDING);
            recommendationRunRepository.saveAndFlush(
                    RecommendationRun.create(proposalPosition, "fp-second", RecommendationAlgorithm.VECTOR_ENGINE_V1,
                            5));
            em.clear();

            List<Long> ids = recommendationRunRepository.findPendingRunIds(PageRequest.of(0, 1));

            assertThat(ids).hasSize(1);
        }

        @Test
        @DisplayName("PENDING run이 없으면 빈 리스트를 반환한다")
        void PENDING_run이_없으면_빈_리스트를_반환한다() {
            saveRunWithStatus(RecommendationRunStatus.RUNNING);
            em.clear();

            List<Long> ids = recommendationRunRepository.findPendingRunIds(PageRequest.of(0, 100));

            assertThat(ids).isEmpty();
        }
    }

    @Nested
    @DisplayName("claimAsRunning")
    class ClaimAsRunning {

        @Test
        @DisplayName("PENDING 상태 run 점유 시 update count가 1이다")
        void PENDING_상태_run_점유_시_update_count가_1이다() {
            RecommendationRun run = saveRunWithStatus(RecommendationRunStatus.PENDING);

            int count = recommendationRunRepository.claimAsRunning(run.getId());

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("claimAsRunning 후 DB에서 다시 조회하면 RUNNING 상태이고 errorMessage가 null이다")
        void claimAsRunning_후_상태가_RUNNING이고_errorMessage가_null이다() {
            RecommendationRun run = saveRunWithStatus(RecommendationRunStatus.PENDING);

            recommendationRunRepository.claimAsRunning(run.getId());
            em.clear();

            RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.RUNNING);
            assertThat(found.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("같은 run을 두 번 호출하면 첫 번째만 1, 두 번째는 0이다")
        void 같은_run을_두_번_호출하면_첫_번째만_성공한다() {
            RecommendationRun run = saveRunWithStatus(RecommendationRunStatus.PENDING);

            int first = recommendationRunRepository.claimAsRunning(run.getId());
            int second = recommendationRunRepository.claimAsRunning(run.getId());

            assertThat(first).isEqualTo(1);
            assertThat(second).isEqualTo(0);
        }

        @Test
        @DisplayName("RUNNING 상태 run은 점유되지 않는다")
        void RUNNING_상태_run은_점유되지_않는다() {
            RecommendationRun run = saveRunWithStatus(RecommendationRunStatus.RUNNING);

            int count = recommendationRunRepository.claimAsRunning(run.getId());

            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("COMPUTED 또는 FAILED 상태 run은 점유되지 않는다")
        void COMPUTED_또는_FAILED_상태_run은_점유되지_않는다() {
            RecommendationRun computed = saveRunWithStatus(RecommendationRunStatus.COMPUTED);
            RecommendationRun failed = saveRunWithStatus(RecommendationRunStatus.FAILED);

            assertThat(recommendationRunRepository.claimAsRunning(computed.getId())).isEqualTo(0);
            assertThat(recommendationRunRepository.claimAsRunning(failed.getId())).isEqualTo(0);
        }

        @Test
        @DisplayName("claimAsRunning 시 modifiedAt이 전달한 시각으로 갱신된다")
        void claimAsRunning_시_modifiedAt이_전달한_시각으로_갱신된다() {
            RecommendationRun run = saveRunWithStatus(RecommendationRunStatus.PENDING);
            LocalDateTime startedAt = LocalDateTime.of(2026, 4, 17, 15, 30, 0);

            recommendationRunRepository.claimAsRunning(run.getId(), startedAt);
            em.flush();
            em.clear();

            RecommendationRun found = recommendationRunRepository.findById(run.getId()).orElseThrow();

            assertThat(found.getStatus()).isEqualTo(RecommendationRunStatus.RUNNING);
            assertThat(found.getModifiedAt()).isEqualTo(startedAt);
        }
    }

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }

    private RecommendationRun saveRunWithStatus(RecommendationRunStatus targetStatus) {
        RecommendationRun run = recommendationRunRepository.saveAndFlush(
                RecommendationRun.create(proposalPosition, "fp-" + targetStatus.name().toLowerCase(),
                        RecommendationAlgorithm.HEURISTIC_V1, 5));
        if (targetStatus == RecommendationRunStatus.RUNNING
                || targetStatus == RecommendationRunStatus.COMPUTED
                || targetStatus == RecommendationRunStatus.FAILED) {
            run.markRunning();
        }
        if (targetStatus == RecommendationRunStatus.COMPUTED) {
            run.markCompleted(new HardFilterStat(10, 8, 6, 3));
        } else if (targetStatus == RecommendationRunStatus.FAILED) {
            run.markFailed("의도된 실패");
        }
        return recommendationRunRepository.saveAndFlush(run);
    }
}
