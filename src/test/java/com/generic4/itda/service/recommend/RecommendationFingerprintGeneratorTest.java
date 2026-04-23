package com.generic4.itda.service.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.fixture.MemberFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * FingerprintGenerator 계약 검증:
 * 1. 동일한 proposalPosition 입력이면 항상 같은 fingerprint를 만든다.
 * 2. skills 입력 순서가 달라도 fingerprint는 동일하다.
 * 3. fingerprint를 구성하는 proposalPosition/추천 파라미터가 바뀌면 fingerprint도 달라진다.
 * 4. nullable 필드가 null이어도 예외 없이 fingerprint를 생성한다.
 * 5. skill.id가 없으면 name으로 fallback 한다.
 */
class RecommendationFingerprintGeneratorTest {

    private RecommendationFingerprintGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RecommendationFingerprintGenerator();
    }

    @Test
    @DisplayName("동일 입력으로 여러 번 호출해도 같은 fingerprint를 반환한다")
    void generate_isDeterministic() {
        ProposalPosition position = createBasePosition(createBaseProposal());
        addSkillWithId(position, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fp1 = generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    @DisplayName("skills 추가 순서가 달라도 fingerprint가 동일하다")
    void generate_isOrderInvariantForSkills() {
        ProposalPosition posOrderA = createBasePosition(createBaseProposal());
        addSkillWithId(posOrderA, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);
        addSkillWithId(posOrderA, 2L, "Spring", ProposalPositionSkillImportance.PREFERENCE);

        ProposalPosition posOrderB = createBasePosition(createBaseProposal());
        addSkillWithId(posOrderB, 2L, "Spring", ProposalPositionSkillImportance.PREFERENCE);
        addSkillWithId(posOrderB, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fp1 = generator.generate(posOrderA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posOrderB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    @DisplayName("proposalPosition.id가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenProposalPositionIdChanges() {
        ProposalPosition posA = createBasePosition(createBaseProposal());
        ProposalPosition posB = createBasePosition(createBaseProposal());
        setId(posB, 11L);

        String fp1 = generator.generate(posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("position.id가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenPositionIdChanges() {
        ProposalPosition posA = createBasePosition(createBaseProposal());
        ProposalPosition posB = createBasePosition(createBaseProposal());
        setId(posB.getPosition(), 1002L);

        String fp1 = generator.generate(posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("proposalPosition.unitBudgetMax가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenUnitBudgetMaxChanges() {
        ProposalPosition posA = createBasePosition(createBaseProposal());
        ProposalPosition posB = createBasePosition(createBaseProposal());
        posB.update(
                posB.getPosition(),
                posB.getTitle(),
                posB.getWorkType(),
                posB.getHeadCount(),
                posB.getUnitBudgetMin(),
                2_500_000L,
                posB.getExpectedPeriod(),
                posB.getCareerMinYears(),
                posB.getCareerMaxYears(),
                posB.getWorkPlace()
        );

        String fp1 = generator.generate(posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("proposalPosition.careerMinYears가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenCareerMinYearsChanges() {
        ProposalPosition posA = createBasePosition(createBaseProposal());
        ProposalPosition posB = createBasePosition(createBaseProposal());
        posB.update(
                posB.getPosition(),
                posB.getTitle(),
                posB.getWorkType(),
                posB.getHeadCount(),
                posB.getUnitBudgetMin(),
                posB.getUnitBudgetMax(),
                posB.getExpectedPeriod(),
                3,
                posB.getCareerMaxYears(),
                posB.getWorkPlace()
        );

        String fp1 = generator.generate(posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("skill importance가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenSkillImportanceChanges() {
        ProposalPosition posA = createBasePosition(createBaseProposal());
        addSkillWithId(posA, 1L, "Java", ProposalPositionSkillImportance.PREFERENCE);

        ProposalPosition posB = createBasePosition(createBaseProposal());
        addSkillWithId(posB, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fp1 = generator.generate(posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("algorithm이 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenAlgorithmChanges() {
        ProposalPosition position = createBasePosition(createBaseProposal());

        String fp1 = generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(position, RecommendationAlgorithm.VECTOR_ENGINE_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("topK가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenTopKChanges() {
        ProposalPosition position = createBasePosition(createBaseProposal());

        String fp1 = generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 10);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("nullable 필드가 null이어도 예외 없이 fingerprint가 생성된다")
    void generate_nullSafe_whenOptionalFieldsAreNull() {
        ProposalPosition position = createPositionWithOptionalFields(
                createBaseProposal(),
                10L,
                1001L,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThatCode(() ->
                generator.generate(position, RecommendationAlgorithm.HEURISTIC_V1, 5)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("skill.id=null이면 name으로 fallback하여 결정적으로 fingerprint를 생성하며, id가 있는 경우와 다르다")
    void generate_skillIdNullFallsBackToName() {
        ProposalPosition posNullId = createBasePosition(createBaseProposal());
        Skill skillNoId = Skill.create("Java", null);
        posNullId.addSkill(skillNoId, ProposalPositionSkillImportance.ESSENTIAL);

        ProposalPosition posWithId = createBasePosition(createBaseProposal());
        addSkillWithId(posWithId, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fpNullId1 = generator.generate(posNullId, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fpNullId2 = generator.generate(posNullId, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fpWithId = generator.generate(posWithId, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fpNullId1).isEqualTo(fpNullId2);
        assertThat(fpNullId1).isNotEqualTo(fpWithId);
    }

    @Test
    @DisplayName("추가 추천 fingerprint는 제외 이력서 id 순서가 달라도 동일하다")
    void generateAdditional_isOrderInvariantForExcludedResumeIds() {
        ProposalPosition position = createBasePosition(createBaseProposal());
        addSkillWithId(position, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fp1 = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                List.of(3L, 1L, 2L)
        );
        String fp2 = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                List.of(2L, 3L, 1L)
        );

        assertThat(fp1).isEqualTo(fp2);
    }

    @Test
    @DisplayName("추가 추천 fingerprint는 제외 이력서 id가 바뀌면 달라진다")
    void generateAdditional_changesWhenExcludedResumeIdsChange() {
        ProposalPosition position = createBasePosition(createBaseProposal());

        String fp1 = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                List.of(1L, 2L)
        );
        String fp2 = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                List.of(1L, 3L)
        );

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("추가 추천 fingerprint는 제외 이력서 id가 null 또는 empty여도 안정적으로 생성된다")
    void generateAdditional_nullAndEmptyExcludedResumeIdsAreEquivalent() {
        ProposalPosition position = createBasePosition(createBaseProposal());

        String nullExcludedFingerprint = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                null
        );
        String emptyExcludedFingerprint = generator.generateAdditional(
                position,
                RecommendationAlgorithm.HEURISTIC_V1,
                10,
                List.of()
        );

        assertThat(nullExcludedFingerprint).isEqualTo(emptyExcludedFingerprint);
    }

    private Proposal createBaseProposal() {
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                "기본 제안서 제목",
                "원본 입력 텍스트",
                "기본 설명",
                1_000_000L,
                5_000_000L,
                3L
        );
        setId(proposal, 1L);
        return proposal;
    }

    private ProposalPosition createBasePosition(Proposal proposal) {
        return createPositionWithOptionalFields(
                proposal,
                10L,
                1001L,
                null,
                1_000_000L,
                2_000_000L,
                null,
                null,
                null
        );
    }

    private ProposalPosition createPositionWithOptionalFields(
            Proposal proposal,
            Long proposalPositionId,
            Long positionId,
            ProposalWorkType workType,
            Long unitBudgetMin,
            Long unitBudgetMax,
            Integer careerMinYears,
            Integer careerMaxYears,
            String workPlace
    ) {
        Position position = Position.create("백엔드 개발자");
        setId(position, positionId);

        ProposalPosition proposalPosition = proposal.addPosition(
                position,
                "백엔드 개발자",
                workType,
                2L,
                unitBudgetMin,
                unitBudgetMax,
                null,
                careerMinYears,
                careerMaxYears,
                workPlace
        );
        setId(proposalPosition, proposalPositionId);
        return proposalPosition;
    }

    private void addSkillWithId(
            ProposalPosition position,
            Long skillId,
            String name,
            ProposalPositionSkillImportance importance
    ) {
        Skill skill = Skill.create(name, null);
        setId(skill, skillId);
        position.addSkill(skill, importance);
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
