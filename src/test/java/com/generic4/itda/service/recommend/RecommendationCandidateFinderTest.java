package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.repository.ResumeQueryRepository;
import com.generic4.itda.repository.ResumeRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationCandidateFinderTest {

    @Mock
    private ResumeQueryRepository resumeQueryRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private RecommendationCandidateFinder finder;

    @DisplayName("필수 스킬이 없으면 전체 추천 가능 이력서 풀을 조회하고 id 순서를 유지한다")
    @Test
    void findCandidates_usesRecommendablePoolWhenNoEssentialSkills() {
        // given
        ProposalPosition proposalPosition = createProposalPosition(
                skillRequirement(201L, "Communication", ProposalPositionSkillImportance.PREFERENCE)
        );
        Resume first = createResume(11L, (byte) 7, skillData(101L, "Java", Proficiency.ADVANCED));
        Resume second = createResume(22L, (byte) 5, skillData(102L, "Spring", Proficiency.INTERMEDIATE));

        given(resumeQueryRepository.findRecommendableResumeIds(100)).willReturn(List.of(11L, 22L));
        given(resumeRepository.findAllWithSkillsByIds(List.of(11L, 22L))).willReturn(List.of(second, first));

        // when
        List<RecommendationCandidate> result = finder.findCandidates(proposalPosition, 3);

        // then
        assertThat(result).extracting(RecommendationCandidate::resumeId)
                .containsExactly(11L, 22L);
        assertThat(result.get(0).skills()).extracting(RecommendationCandidate.CandidateSkill::skillName)
                .containsExactly("Java");

        verify(resumeQueryRepository).findRecommendableResumeIds(100);
        verify(resumeRepository).findAllWithSkillsByIds(List.of(11L, 22L));
        verifyNoMoreInteractions(resumeQueryRepository, resumeRepository);
    }

    @DisplayName("필수 스킬이 있으면 필수 스킬 id만으로 후보 풀을 조회하고 topK에 따라 풀 크기를 늘린다")
    @Test
    void findCandidates_usesEssentialSkillsAndExpandedPoolSize() {
        // given
        ProposalPosition proposalPosition = createProposalPosition(
                skillRequirement(101L, "Java", ProposalPositionSkillImportance.ESSENTIAL),
                skillRequirement(202L, "React", ProposalPositionSkillImportance.PREFERENCE)
        );
        Resume first = createResume(11L, (byte) 9, skillData(101L, "Java", Proficiency.ADVANCED));
        Resume second = createResume(22L, (byte) 4, skillData(101L, "Java", Proficiency.INTERMEDIATE));

        given(resumeQueryRepository.findCandidatePool(List.of(101L), 120)).willReturn(List.of(
                new CandidatePoolRow(22L, 1L, 4, (byte) 4),
                new CandidatePoolRow(11L, 1L, 9, (byte) 9)
        ));
        given(resumeRepository.findAllWithSkillsByIds(List.of(22L, 11L))).willReturn(List.of(first, second));

        // when
        List<RecommendationCandidate> result = finder.findCandidates(proposalPosition, 6);

        // then
        assertThat(result).extracting(RecommendationCandidate::resumeId)
                .containsExactly(22L, 11L);

        verify(resumeQueryRepository).findCandidatePool(List.of(101L), 120);
        verify(resumeRepository).findAllWithSkillsByIds(List.of(22L, 11L));
        verifyNoMoreInteractions(resumeQueryRepository, resumeRepository);
    }

    @DisplayName("후보 풀 조회 결과가 비어 있으면 Resume 상세 조회 없이 빈 리스트를 반환한다")
    @Test
    void findCandidates_returnsEmptyWhenCandidatePoolIsEmpty() {
        // given
        ProposalPosition proposalPosition = createProposalPosition(
                skillRequirement(101L, "Java", ProposalPositionSkillImportance.ESSENTIAL)
        );
        given(resumeQueryRepository.findCandidatePool(List.of(101L), 100)).willReturn(List.of());

        // when
        List<RecommendationCandidate> result = finder.findCandidates(proposalPosition, 5);

        // then
        assertThat(result).isEmpty();
        verify(resumeQueryRepository).findCandidatePool(List.of(101L), 100);
        verifyNoInteractions(resumeRepository);
        verifyNoMoreInteractions(resumeQueryRepository);
    }

    private ProposalPosition createProposalPosition(SkillRequirement... requirements) {
        Proposal proposal = Proposal.create(
                createMember(),
                "추천 요청",
                "raw-input",
                null,
                null,
                null,
                null,
                null,
                null
        );
        ProposalPosition proposalPosition = proposal.addPosition(Position.create("백엔드"), 1L, null, null);

        for (SkillRequirement requirement : requirements) {
            Skill skill = Skill.create(requirement.name(), null);
            ReflectionTestUtils.setField(skill, "id", requirement.skillId());
            proposalPosition.addSkill(skill, requirement.importance());
        }

        return proposalPosition;
    }

    private Resume createResume(long resumeId, byte careerYears, SkillData... skills) {
        Resume resume = Resume.create(
                createMember(),
                "소개-" + resumeId,
                careerYears,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                null
        );
        ReflectionTestUtils.setField(resume, "id", resumeId);

        for (SkillData skillData : skills) {
            Skill skill = Skill.create(skillData.name(), null);
            ReflectionTestUtils.setField(skill, "id", skillData.skillId());
            resume.addSkill(skill, skillData.proficiency());
        }

        return resume;
    }

    private static SkillRequirement skillRequirement(
            long skillId,
            String name,
            ProposalPositionSkillImportance importance
    ) {
        return new SkillRequirement(skillId, name, importance);
    }

    private static SkillData skillData(long skillId, String name, Proficiency proficiency) {
        return new SkillData(skillId, name, proficiency);
    }

    private record SkillRequirement(long skillId, String name, ProposalPositionSkillImportance importance) {
    }

    private record SkillData(long skillId, String name, Proficiency proficiency) {
    }
}
