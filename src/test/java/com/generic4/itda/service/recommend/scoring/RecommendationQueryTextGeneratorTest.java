package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalWorkType;
import com.generic4.itda.fixture.MemberFixture;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("RecommendationQueryTextGenerator 단위 테스트")
class RecommendationQueryTextGeneratorTest {

    private final RecommendationQueryTextGenerator generator = new RecommendationQueryTextGenerator();

    @Test
    @DisplayName("제안서 정보를 바탕으로 임베딩용 쿼리 텍스트를 생성한다.")
    void generate_ReturnsFormattedText() {
        // given
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        ReflectionTestUtils.setField(position, "id", 1L);

        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, 3, 5, null
        );

        Set<String> requiredSkills = Set.of("Java", "Spring");
        Set<String> preferredSkills = Set.of("AWS", "Docker", "Java"); // Java is redundant

        // when
        String result = generator.generate(proposal, proposalPosition, requiredSkills, preferredSkills);

        // then
        assertThat(result).contains("position: Backend Developer");
        assertThat(result).contains("workType: REMOTE");
        assertThat(result).contains("career: 3~5 years");
        assertThat(result).contains("required skills: Java, Spring");
        assertThat(result).contains("preferred skills: AWS, Docker"); // Java should be filtered out
        assertThat(result).contains("title: Title");
        assertThat(result).contains("description: Description");
    }

    @Test
    @DisplayName("경력 요구사항이 null일 경우 빈 문자열로 처리한다.")
    void generate_WithNullCareer_ReturnsEmptyCareer() {
        // given
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, null, null, null
        );

        // when
        String result = generator.generate(proposal, proposalPosition, null, null);

        // then
        assertThat(result).contains("career: ");
        assertThat(result).doesNotContain("career: null");
    }

    @Test
    @DisplayName("최소 경력만 있을 경우의 포맷을 확인한다.")
    void generate_WithMinCareerOnly() {
        // given
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, 3, null, null
        );

        // when
        String result = generator.generate(proposal, proposalPosition, null, null);

        // then
        assertThat(result).contains("career: 3+ years");
    }

    @Test
    @DisplayName("최대 경력만 있을 경우의 포맷을 확인한다.")
    void generate_WithMaxCareerOnly() {
        // given
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, null, 5, null
        );

        // when
        String result = generator.generate(proposal, proposalPosition, null, null);

        // then
        assertThat(result).contains("career: up to 5 years");
    }

    @Test
    @DisplayName("required/preferred skill이 null이어도 null 문자열을 출력하지 않는다.")
    void generate_WithNullSkills_DoesNotPrintNull() {
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, 3, 5, null
        );

        String result = generator.generate(proposal, proposalPosition, null, null);

        assertThat(result).contains("required skills: ");
        assertThat(result).contains("preferred skills: ");
        assertThat(result).doesNotContain("null");
    }

    @Test
    @DisplayName("required skill이 비어 있으면 preferred skill을 그대로 포함한다.")
    void generate_WithOnlyPreferredSkills_IncludesPreferredSkills() {
        Member client = MemberFixture.createMember();
        Position position = Position.create("Backend Developer");
        Proposal proposal = Proposal.create(client, "Title", "Raw Input", "Description", null, null, null);
        ProposalPosition proposalPosition = proposal.addPosition(
                position, "Position Title", ProposalWorkType.REMOTE, 1L, null, null, null, 3, 5, null
        );

        String result = generator.generate(
                proposal,
                proposalPosition,
                null,
                Set.of("AWS", "Docker")
        );

        assertThat(result).contains("preferred skills: AWS, Docker");
    }
}
