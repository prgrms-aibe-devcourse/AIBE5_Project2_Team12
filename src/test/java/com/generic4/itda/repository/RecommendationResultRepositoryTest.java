package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.annotation.RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.RecommendationResult;
import com.generic4.itda.domain.recommendation.RecommendationRun;
import com.generic4.itda.domain.recommendation.constant.LlmStatus;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class RecommendationResultRepositoryTest {

    @Autowired
    private RecommendationResultRepository recommendationResultRepository;

    @Autowired
    private RecommendationRunRepository recommendationRunRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private EntityManager em;

    private RecommendationRun run;
    private Resume resume;

    @BeforeEach
    void setUp() {
        Member proposalMember = memberRepository.save(
                createMember("proposer@test.com", "pw", "제안자", "010-0000-0001"));
        Position position = persistPosition("백엔드 개발자");
        Proposal proposal = Proposal.create(
                proposalMember, "AI 매칭 플랫폼", "원문", null,
                null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                "백엔드 개발자",
                null,
                2L,
                500_000L,
                1_000_000L,
                null,
                null,
                null,
                null
        );
        proposalRepository.saveAndFlush(proposal);

        run = recommendationRunRepository.saveAndFlush(
                RecommendationRun.create(proposalPosition, "fp-001", RecommendationAlgorithm.HEURISTIC_V1, 5));

        Member resumeMember = memberRepository.save(
                createMember("applicant@test.com", "pw", "지원자", "010-0000-0002"));
        resume = resumeRepository.saveAndFlush(
                Resume.create(resumeMember, "자기소개입니다.", (byte) 3, new CareerPayload(),
                        WorkType.REMOTE, ResumeWritingStatus.WRITING, null));
    }

    @Test
    @DisplayName("ReasonFacts와 LlmStatus 기본값이 저장 후 조회 시 유지된다")
    void ReasonFacts와_LlmStatus_기본값이_저장_후_조회_시_유지된다() {
        // given
        ReasonFacts reasonFacts = new ReasonFacts(List.of("Java", "Spring"), List.of("백엔드"), 3, List.of("MSA 경험"));
        RecommendationResult result = RecommendationResult.create(
                run, resume, 1,
                new BigDecimal("0.9500"), new BigDecimal("0.8800"), reasonFacts);

        // when
        recommendationResultRepository.saveAndFlush(result);
        em.clear();

        // then
        RecommendationResult found = recommendationResultRepository.findById(result.getId()).orElseThrow();
        assertThat(found.getRank()).isEqualTo(1);
        assertThat(found.getFinalScore()).isEqualByComparingTo(new BigDecimal("0.9500"));
        assertThat(found.getEmbeddingScore()).isEqualByComparingTo(new BigDecimal("0.8800"));
        assertThat(found.getLlmStatus()).isEqualTo(LlmStatus.PENDING);
        assertThat(found.getLlmReason()).isNull();
        assertThat(found.getReasonFacts().matchedSkills()).containsExactly("Java", "Spring");
        assertThat(found.getReasonFacts().matchedDomains()).containsExactly("백엔드");
        assertThat(found.getReasonFacts().careerYears()).isEqualTo(3);
        assertThat(found.getReasonFacts().highlights()).containsExactly("MSA 경험");
    }

    @Test
    @DisplayName("같은 run에 동일한 resume는 중복 저장할 수 없다")
    void 같은_run에_동일한_resume는_중복_저장할_수_없다() {
        // given
        ReasonFacts reasonFacts = new ReasonFacts(List.of(), List.of(), 0, List.of());
        RecommendationResult first = RecommendationResult.create(
                run, resume, 1, new BigDecimal("0.9000"), new BigDecimal("0.9000"), reasonFacts);
        RecommendationResult second = RecommendationResult.create(
                run, resume, 2, new BigDecimal("0.8000"), new BigDecimal("0.8000"), reasonFacts);

        // when
        recommendationResultRepository.saveAndFlush(first);

        // then
        assertThatThrownBy(() -> recommendationResultRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 run에 동일한 rank는 중복 저장할 수 없다")
    void 같은_run에_동일한_rank는_중복_저장할_수_없다() {
        // given - 두 번째 이력서 생성
        Member anotherMember = memberRepository.save(
                createMember("another@test.com", "pw", "지원자2", "010-0000-0003"));
        Resume anotherResume = resumeRepository.save(
                Resume.create(anotherMember, "다른 자기소개", (byte) 2, new CareerPayload(),
                        WorkType.SITE, ResumeWritingStatus.WRITING, null));

        ReasonFacts reasonFacts = new ReasonFacts(List.of(), List.of(), 0, List.of());
        RecommendationResult first = RecommendationResult.create(
                run, resume, 1, new BigDecimal("0.9000"), new BigDecimal("0.9000"), reasonFacts);
        RecommendationResult second = RecommendationResult.create(
                run, anotherResume, 1, new BigDecimal("0.8000"), new BigDecimal("0.8000"), reasonFacts);

        // when
        recommendationResultRepository.saveAndFlush(first);

        // then
        assertThatThrownBy(() -> recommendationResultRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("runId로 결과를 조회하면 rank 오름차순으로 정렬되고 resume/member를 fetch join 한다")
    void runId로_결과를_조회하면_rank_오름차순으로_정렬되고_resume_member를_fetch_join_한다() {
        // given
        Member anotherMember = memberRepository.save(
                createMember("another2@test.com", "pw", "지원자2", "010-0000-0004"));
        Resume anotherResume = resumeRepository.saveAndFlush(
                Resume.create(anotherMember, "두번째 자기소개", (byte) 2, new CareerPayload(),
                        WorkType.SITE, ResumeWritingStatus.WRITING, null));

        ReasonFacts reasonFacts = new ReasonFacts(List.of(), List.of(), 0, List.of());
        RecommendationResult rank2 = RecommendationResult.create(
                run, anotherResume, 2, new BigDecimal("0.8000"), new BigDecimal("0.8000"), reasonFacts);
        RecommendationResult rank1 = RecommendationResult.create(
                run, resume, 1, new BigDecimal("0.9000"), new BigDecimal("0.9000"), reasonFacts);

        recommendationResultRepository.saveAndFlush(rank2);
        recommendationResultRepository.saveAndFlush(rank1);
        em.clear();

        // when
        List<RecommendationResult> found = recommendationResultRepository.findByRunIdWithResume(run.getId());

        // then
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getRank()).isEqualTo(1);
        assertThat(found.get(1).getRank()).isEqualTo(2);

        PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(found.get(0), "resume")).isTrue();
        assertThat(util.isLoaded(found.get(0).getResume(), "member")).isTrue();

        assertThat(found.get(0).getResume().getMember().getEmail().getValue()).isEqualTo("applicant@test.com");
        assertThat(found.get(1).getResume().getMember().getEmail().getValue()).isEqualTo("another2@test.com");
    }

    @Nested
    @DisplayName("findDetailById")
    class FindDetailById {

        @Test
        @DisplayName("추천 결과에서 매칭 생성에 필요한 상위 그래프를 함께 조회한다")
        void fetchesRecommendationResultGraphForMatchingRequest() {
            ReasonFacts reasonFacts = new ReasonFacts(List.of("Java"), List.of("백엔드"), 3, List.of("Spring 경험"));
            RecommendationResult result = recommendationResultRepository.saveAndFlush(
                    RecommendationResult.create(
                            run,
                            resume,
                            1,
                            new BigDecimal("0.9500"),
                            new BigDecimal("0.8800"),
                            reasonFacts
                    )
            );
            em.clear();

            RecommendationResult found = recommendationResultRepository.findDetailById(result.getId()).orElseThrow();

            PersistenceUnitUtil util = em.getEntityManagerFactory().getPersistenceUnitUtil();
            assertThat(util.isLoaded(found, "recommendationRun")).isTrue();
            assertThat(util.isLoaded(found, "resume")).isTrue();
            assertThat(util.isLoaded(found.getRecommendationRun(), "proposalPosition")).isTrue();
            assertThat(util.isLoaded(found.getRecommendationRun().getProposalPosition(), "proposal")).isTrue();
            assertThat(
                    util.isLoaded(found.getRecommendationRun().getProposalPosition().getProposal(), "member")).isTrue();
            assertThat(util.isLoaded(found.getResume(), "member")).isTrue();

            assertThat(found.getRecommendationRun().getId()).isEqualTo(run.getId());
            assertThat(
                    found.getRecommendationRun().getProposalPosition().getProposal().getMember().getEmail().getValue())
                    .isEqualTo("proposer@test.com");
            assertThat(found.getResume().getMember().getEmail().getValue()).isEqualTo("applicant@test.com");
        }
    }

    @Nested
    @DisplayName("포지션별 추천 이력서 ID 조회")
    class FindRecommendedResumeIds {

        @Test
        @DisplayName("proposalPosition 기준으로 추천된 resumeId를 중복 없이 정렬하여 조회한다")
        void findRecommendedResumeIdsByProposalPositionId_returnsDistinctSortedResumeIds() {
            // given
            Member proposalMember = memberRepository.save(createMember("p2@test.com", "pw", "제안자2", "010-1000-0001"));
            Position p = persistPosition("프론트엔드 개발자");
            Proposal proposal = Proposal.create(proposalMember, "프로젝트", "원문", null, null, null, null);
            ProposalPosition pp = proposal.addPosition(p, "프론트", null, 1L, null, null, null, null, null, null);
            proposalRepository.saveAndFlush(proposal);

            RecommendationRun run1 = recommendationRunRepository.save(
                    RecommendationRun.create(pp, "fp-1", RecommendationAlgorithm.HEURISTIC_V1, 5));
            RecommendationRun run2 = recommendationRunRepository.save(
                    RecommendationRun.create(pp, "fp-2", RecommendationAlgorithm.HEURISTIC_V1, 5));

            Resume res1 = createAndSaveResume("u1@test.com");
            Resume res2 = createAndSaveResume("u2@test.com");
            Resume res3 = createAndSaveResume("u3@test.com");

            ReasonFacts facts = new ReasonFacts(List.of(), List.of(), 0, List.of());
            recommendationResultRepository.save(
                    RecommendationResult.create(run1, res1, 1, new BigDecimal("0.9"), new BigDecimal("0.9"), facts));
            recommendationResultRepository.save(
                    RecommendationResult.create(run1, res2, 2, new BigDecimal("0.8"), new BigDecimal("0.8"), facts));
            recommendationResultRepository.save(
                    RecommendationResult.create(run2, res2, 1, new BigDecimal("0.9"), new BigDecimal("0.9"),
                            facts)); // 중복
            recommendationResultRepository.save(
                    RecommendationResult.create(run2, res3, 2, new BigDecimal("0.8"), new BigDecimal("0.8"), facts));
            recommendationResultRepository.flush();
            em.clear();

            // when
            List<Long> resumeIds = recommendationResultRepository.findRecommendedResumeIdsByProposalPositionId(
                    pp.getId());

            // then
            assertThat(resumeIds).hasSize(3);
            assertThat(resumeIds).isSorted();
            assertThat(resumeIds).containsExactlyInAnyOrder(res1.getId(), res2.getId(), res3.getId());
        }

        @Test
        @DisplayName("특정 runId를 제외하고 proposalPositionId 기준 추천된 resumeId를 조회한다")
        void findRecommendedResumeIdsByProposalPositionIdExceptRunId_excludesTargetRunResults() {
            // given
            Member proposalMember = memberRepository.save(createMember("p3@test.com", "pw", "제안자3", "010-2000-0001"));
            Position p = persistPosition("데이터 엔지니어");
            Proposal proposal = Proposal.create(proposalMember, "프로젝트2", "원문", null, null, null, null);
            ProposalPosition pp = proposal.addPosition(p, "데이터", null, 1L, null, null, null, null, null, null);
            proposalRepository.saveAndFlush(proposal);

            RecommendationRun run1 = recommendationRunRepository.save(
                    RecommendationRun.create(pp, "fp-1", RecommendationAlgorithm.HEURISTIC_V1, 5));
            RecommendationRun run2 = recommendationRunRepository.save(
                    RecommendationRun.create(pp, "fp-2", RecommendationAlgorithm.HEURISTIC_V1, 5));

            Resume res1 = createAndSaveResume("u4@test.com");
            Resume res2 = createAndSaveResume("u5@test.com");
            Resume res3 = createAndSaveResume("u6@test.com");

            ReasonFacts facts = new ReasonFacts(List.of(), List.of(), 0, List.of());
            recommendationResultRepository.save(
                    RecommendationResult.create(run1, res1, 1, new BigDecimal("0.9"), new BigDecimal("0.9"), facts));
            recommendationResultRepository.save(
                    RecommendationResult.create(run1, res2, 2, new BigDecimal("0.8"), new BigDecimal("0.8"), facts));
            recommendationResultRepository.save(
                    RecommendationResult.create(run2, res3, 1, new BigDecimal("0.9"), new BigDecimal("0.9"), facts));
            recommendationResultRepository.flush();
            em.clear();

            // when: run2 제외
            List<Long> resumeIds = recommendationResultRepository.findRecommendedResumeIdsByProposalPositionIdExceptRunId(
                    pp.getId(), run2.getId());

            // then: run1의 결과인 res1, res2만 조회되어야 함
            assertThat(resumeIds).hasSize(2);
            assertThat(resumeIds).containsExactlyInAnyOrder(res1.getId(), res2.getId());
            assertThat(resumeIds).doesNotContain(res3.getId());
            assertThat(resumeIds).isSorted();
        }

        @Test
        @DisplayName("다른 proposalPosition의 결과는 포함되지 않는다")
        void findRecommendedResumeIdsByProposalPositionId_onlyReturnsTargetPositionResults() {
            // given
            Member proposalMember = memberRepository.save(createMember("p4@test.com", "pw", "제안자4", "010-3000-0001"));
            Position p = persistPosition("QA");
            Proposal proposal = Proposal.create(proposalMember, "프로젝트3", "원문", null, null, null, null);
            ProposalPosition pp1 = proposal.addPosition(p, "QA1", null, 1L, null, null, null, null, null, null);
            ProposalPosition pp2 = proposal.addPosition(p, "QA2", null, 1L, null, null, null, null, null, null);
            proposalRepository.saveAndFlush(proposal);

            RecommendationRun run1 = recommendationRunRepository.save(
                    RecommendationRun.create(pp1, "fp-1", RecommendationAlgorithm.HEURISTIC_V1, 5));
            RecommendationRun run2 = recommendationRunRepository.save(
                    RecommendationRun.create(pp2, "fp-2", RecommendationAlgorithm.HEURISTIC_V1, 5));

            Resume res1 = createAndSaveResume("u7@test.com");
            Resume res2 = createAndSaveResume("u8@test.com");

            ReasonFacts facts = new ReasonFacts(List.of(), List.of(), 0, List.of());
            recommendationResultRepository.save(
                    RecommendationResult.create(run1, res1, 1, new BigDecimal("0.9"), new BigDecimal("0.9"), facts));
            recommendationResultRepository.save(
                    RecommendationResult.create(run2, res2, 1, new BigDecimal("0.9"), new BigDecimal("0.9"), facts));
            recommendationResultRepository.flush();
            em.clear();

            // when
            List<Long> resumeIds = recommendationResultRepository.findRecommendedResumeIdsByProposalPositionId(
                    pp1.getId());

            // then
            assertThat(resumeIds).isSorted();
            assertThat(resumeIds).containsExactly(res1.getId());
            assertThat(resumeIds).doesNotContain(res2.getId());
        }

        @Test
        @DisplayName("결과가 없으면 빈 리스트를 반환한다")
        void findRecommendedResumeIdsByProposalPositionId_returnsEmptyListWhenNoResults() {
            // when
            List<Long> resumeIds = recommendationResultRepository.findRecommendedResumeIdsByProposalPositionId(999L);

            // then
            assertThat(resumeIds).isEmpty();
        }

        private Resume createAndSaveResume(String email) {
            Member member = memberRepository.save(createMember(email, "pw", "지원자", "010-9999-9999"));
            return resumeRepository.save(Resume.create(member, "소개", (byte) 3, new CareerPayload(), WorkType.REMOTE,
                    ResumeWritingStatus.WRITING, null));
        }
    }

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }
}
