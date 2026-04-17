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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * FingerprintGenerator 계약 검증: 1. 결정성(determinism): 동일 입력 → 동일 fingerprint 2. 순서 불변성: skills 삽입 순서가 달라도 동일 fingerprint
 * (정렬 로직) 3. 민감도(sensitivity): 입력 필드 하나가 바뀌면 fingerprint가 달라진다 4. null-safe: nullable 필드가 null이어도 예외 없이 생성된다 5. skill
 * id fallback: skill.id=null이면 name으로 대체된다
 * <p>
 * ※ Spring 컨텍스트 없이 new RecommendationFingerprintGeneratorTest()로 직접 인스턴스화한다.
 */
class RecommendationFingerprintGeneratorTest {

    private RecommendationFingerprintGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RecommendationFingerprintGenerator();
    }

    // ============================================================
    // 결정성
    // ============================================================

    @Test
    @DisplayName("동일 입력으로 여러 번 호출해도 같은 fingerprint를 반환한다")
    void generate_isDeterministic() {
        // 이 값이 같아야 하는 이유:
        // SHA-256은 순수 함수(pure function)다. 입력 바이트가 동일하면 출력 해시는 항상 동일하다.
        // 난수·시간·외부 상태에 의존하지 않으므로 반복 호출에도 동일한 값이 나와야 한다.
        Proposal proposal = createBaseProposal();
        ProposalPosition position = createBasePosition(proposal);
        addSkillWithId(position, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);

        String fp1 = generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isEqualTo(fp2);
    }

    // ============================================================
    // 순서 불변성
    // ============================================================

    @Test
    @DisplayName("skills 추가 순서가 달라도 fingerprint가 동일하다")
    void generate_isOrderInvariantForSkills() {
        // 이 값이 같아야 하는 이유:
        // generate() 내부에서 skills를 name → importance 기준으로 정렬(Comparator.comparing)한 뒤 "|"로 결합한다.
        // 따라서 addSkill() 호출 순서(삽입 순서)가 달라도 skillPart 문자열이 동일하게 구성된다.
        Proposal proposalA = createBaseProposal();
        ProposalPosition posOrderA = createBasePosition(proposalA);
        addSkillWithId(posOrderA, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);   // Java 먼저
        addSkillWithId(posOrderA, 2L, "Spring", ProposalPositionSkillImportance.PREFERENCE); // Spring 나중

        Proposal proposalB = createBaseProposal(); // id=1L (동일)
        ProposalPosition posOrderB = createBasePosition(proposalB);
        addSkillWithId(posOrderB, 2L, "Spring", ProposalPositionSkillImportance.PREFERENCE); // Spring 먼저 (역순)
        addSkillWithId(posOrderB, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL);    // Java 나중 (역순)

        String fp1 = generator.generate(proposalA, posOrderA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposalB, posOrderB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isEqualTo(fp2);
    }

    // ============================================================
    // 민감도: proposal 필드 변경
    // ============================================================

    @Test
    @DisplayName("proposal.title이 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenTitleChanges() {
        // 이 값이 달라져야 하는 이유:
        // raw string에 "proposal.title=<value>"가 포함된다.
        // title이 달라지면 raw string이 달라지고, SHA-256 출력도 달라진다.
        Proposal proposalA = createProposalWithTitle("제목 A");
        Proposal proposalB = createProposalWithTitle("제목 B"); // title만 다름, id=1L 동일
        ProposalPosition posA = createBasePosition(proposalA);
        ProposalPosition posB = createBasePosition(proposalB);

        String fp1 = generator.generate(proposalA, posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposalB, posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("proposal.description이 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenDescriptionChanges() {
        // 이 값이 달라져야 하는 이유:
        // raw string에 "proposal.description=<value>"가 포함된다.
        // description이 달라지면 raw string이 달라지고, SHA-256 출력도 달라진다.
        Proposal proposalA = createProposalWithDescription("설명 A");
        Proposal proposalB = createProposalWithDescription("설명 B"); // description만 다름, id=1L 동일
        ProposalPosition posA = createBasePosition(proposalA);
        ProposalPosition posB = createBasePosition(proposalB);

        String fp1 = generator.generate(proposalA, posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposalB, posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // ============================================================
    // 민감도: position 필드 변경
    // ============================================================

    @Test
    @DisplayName("proposalPosition.headCount가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenHeadCountChanges() {
        // 이 값이 달라져야 하는 이유:
        // raw string에 "proposalPosition.headCount=<value>"가 포함된다.
        // headCount가 달라지면 raw string이 달라지고, SHA-256 출력도 달라진다.
        Proposal proposalA = createBaseProposal();
        Proposal proposalB = createBaseProposal(); // id=1L 동일

        // 같은 proposal에 동일 position 이름을 두 번 추가할 수 없으므로 각각 별도 proposal에 추가
        ProposalPosition posA = proposalA.addPosition(Position.create("포지션"), 3L, null, null);
        setId(posA, 10L);

        ProposalPosition posB = proposalB.addPosition(Position.create("포지션"), 5L, null, null); // headCount만 다름
        setId(posB, 10L);

        String fp1 = generator.generate(proposalA, posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposalB, posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("skill importance가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenSkillImportanceChanges() {
        // 이 값이 달라져야 하는 이유:
        // toSkillToken()이 "id:importance.name()"으로 토큰을 만든다.
        // PREFERENCE → ESSENTIAL로 바뀌면 skillPart 문자열이 달라지고, fingerprint도 달라진다.
        Proposal proposalA = createBaseProposal();
        Proposal proposalB = createBaseProposal(); // id=1L 동일

        ProposalPosition posA = createBasePosition(proposalA);
        addSkillWithId(posA, 1L, "Java", ProposalPositionSkillImportance.PREFERENCE);

        ProposalPosition posB = createBasePosition(proposalB);
        addSkillWithId(posB, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL); // importance만 다름

        String fp1 = generator.generate(proposalA, posA, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposalB, posB, RecommendationAlgorithm.HEURISTIC_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // ============================================================
    // 민감도: 추천 파라미터 변경
    // ============================================================

    @Test
    @DisplayName("algorithm이 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenAlgorithmChanges() {
        // 이 값이 달라져야 하는 이유:
        // raw string에 "recommendation.algorithm=<value>"가 포함된다.
        // HEURISTIC_V1 vs VECTOR_ENGINE_V1은 name()이 다르므로 raw string이 달라진다.
        Proposal proposal = createBaseProposal();
        ProposalPosition position = createBasePosition(proposal);

        String fp1 = generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposal, position, RecommendationAlgorithm.VECTOR_ENGINE_V1, 5);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    @Test
    @DisplayName("topK가 바뀌면 fingerprint가 달라진다")
    void generate_changesWhenTopKChanges() {
        // 이 값이 달라져야 하는 이유:
        // raw string에 "recommendation.topK=<value>"가 포함된다.
        // topK=5 vs topK=10은 숫자가 달라 raw string이 달라진다.
        Proposal proposal = createBaseProposal();
        ProposalPosition position = createBasePosition(proposal);

        String fp1 = generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fp2 = generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 10);

        assertThat(fp1).isNotEqualTo(fp2);
    }

    // ============================================================
    // null-safe
    // ============================================================

    @Test
    @DisplayName("proposal.description=null이어도 예외 없이 fingerprint가 생성된다")
    void generate_nullSafe_whenDescriptionIsNull() {
        // nullSafe()가 null → ""으로 변환하므로 NullPointerException 없이 동작해야 한다.
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                "제목", "원본 입력", null, // description = null
                1_000_000L, 2_000_000L, ProposalWorkType.REMOTE, "판교", 3L
        );
        setId(proposal, 1L);
        ProposalPosition position = createBasePosition(proposal);

        assertThatCode(() ->
                generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("proposal.workPlace=null이어도 예외 없이 fingerprint가 생성된다")
    void generate_nullSafe_whenWorkPlaceIsNull() {
        // nullSafe()가 null → ""으로 변환하므로 NullPointerException 없이 동작해야 한다.
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                "제목", "원본 입력", "설명",
                1_000_000L, 2_000_000L, ProposalWorkType.REMOTE, null, // workPlace = null
                3L
        );
        setId(proposal, 1L);
        ProposalPosition position = createBasePosition(proposal);

        assertThatCode(() ->
                generator.generate(proposal, position, RecommendationAlgorithm.HEURISTIC_V1, 5)
        ).doesNotThrowAnyException();
    }

    // ============================================================
    // skill id fallback
    // ============================================================

    @Test
    @DisplayName("skill.id=null이면 name으로 fallback하여 결정적으로 fingerprint를 생성하며, id가 있는 경우와 다르다")
    void generate_skillIdNullFallsBackToName() {
        // 이 값이 달라져야 하는 이유:
        // toSkillToken()이 id != null ? id : name 으로 토큰을 결정한다.
        //   - skill.id=null  → 토큰: "Java:ESSENTIAL"
        //   - skill.id=1L    → 토큰: "1:ESSENTIAL"
        // 두 토큰이 다르므로 fingerprint도 달라야 한다.
        //
        // 각 경우는 결정적이어야 한다: 동일 입력으로 두 번 호출 시 같은 fingerprint가 나와야 한다.
        Proposal proposalWithNullId = createBaseProposal();
        ProposalPosition posNullId = createBasePosition(proposalWithNullId);
        Skill skillNoId = Skill.create("Java", null); // id = null (JPA 미할당 상태)
        posNullId.addSkill(skillNoId, ProposalPositionSkillImportance.ESSENTIAL);

        Proposal proposalWithId = createBaseProposal(); // id=1L 동일
        ProposalPosition posWithId = createBasePosition(proposalWithId);
        addSkillWithId(posWithId, 1L, "Java", ProposalPositionSkillImportance.ESSENTIAL); // id=1L

        String fpNullId1 = generator.generate(proposalWithNullId, posNullId, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fpNullId2 = generator.generate(proposalWithNullId, posNullId, RecommendationAlgorithm.HEURISTIC_V1, 5);
        String fpWithId = generator.generate(proposalWithId, posWithId, RecommendationAlgorithm.HEURISTIC_V1, 5);

        // null id 경우도 결정적이어야 한다
        assertThat(fpNullId1).isEqualTo(fpNullId2);

        // id가 있을 때와 없을 때는 토큰이 달라 fingerprint가 달라야 한다
        assertThat(fpNullId1).isNotEqualTo(fpWithId);
    }

    // ============================================================
    // private helpers
    // ============================================================

    private Proposal createBaseProposal() {
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                "기본 제안서 제목", "원본 입력 텍스트", "기본 설명",
                1_000_000L, 5_000_000L, ProposalWorkType.REMOTE, "판교", 3L
        );
        setId(proposal, 1L);
        return proposal;
    }

    private Proposal createProposalWithTitle(String title) {
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                title, "원본 입력 텍스트", "기본 설명",
                1_000_000L, 5_000_000L, ProposalWorkType.REMOTE, "판교", 3L
        );
        setId(proposal, 1L);
        return proposal;
    }

    private Proposal createProposalWithDescription(String description) {
        Proposal proposal = Proposal.create(
                MemberFixture.createMember(),
                "기본 제안서 제목", "원본 입력 텍스트", description,
                1_000_000L, 5_000_000L, ProposalWorkType.REMOTE, "판교", 3L
        );
        setId(proposal, 1L);
        return proposal;
    }

    private ProposalPosition createBasePosition(Proposal proposal) {
        ProposalPosition position = proposal.addPosition(Position.create("백엔드 개발자"), 2L, null, null);
        setId(position, 10L);
        return position;
    }

    private void addSkillWithId(ProposalPosition position, Long skillId, String name,
            ProposalPositionSkillImportance importance) {
        Skill skill = Skill.create(name, null);
        setId(skill, skillId);
        position.addSkill(skill, importance);
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
