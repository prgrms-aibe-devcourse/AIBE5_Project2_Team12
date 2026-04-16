package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.annotation.RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
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
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
                null, null, ProposalWorkType.REMOTE, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(position, 2L, 500_000L, 1_000_000L);
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

    private Position persistPosition(String name) {
        Position position = Position.create(name);
        em.persist(position);
        return position;
    }
}
