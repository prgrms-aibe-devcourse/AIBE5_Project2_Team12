package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.service.recommend.CandidatePoolRow;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@H2RepositoryTest
@Import(ResumeQueryRepositoryImpl.class)
class ResumeQueryRepositoryTest {

    @Autowired
    private ResumeQueryRepository resumeQueryRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SkillRepository skillRepository;

    @DisplayName("findCandidatePool은 필터링 후 매칭 수, 숙련도 합, 경력, id 순으로 후보를 정렬한다")
    @Test
    void findCandidatePool_filtersAndOrdersCandidates() {
        // given
        Skill java = skillRepository.saveAndFlush(Skill.create("Java-query-pool", null));
        Skill spring = skillRepository.saveAndFlush(Skill.create("Spring-query-pool", null));
        Skill aws = skillRepository.saveAndFlush(Skill.create("Aws-query-pool", null));

        Resume strongest = saveResume("pool-1", (byte) 5, true, true, true,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.INTERMEDIATE));
        Resume sameScoreHighCareerLowId = saveResume("pool-2", (byte) 10, true, true, true,
                skill(java, Proficiency.INTERMEDIATE),
                skill(spring, Proficiency.INTERMEDIATE));
        Resume sameScoreLowCareer = saveResume("pool-3", (byte) 7, true, true, true,
                skill(java, Proficiency.INTERMEDIATE),
                skill(spring, Proficiency.INTERMEDIATE));
        Resume sameScoreHighCareerHighId = saveResume("pool-4", (byte) 10, true, true, true,
                skill(java, Proficiency.INTERMEDIATE),
                skill(spring, Proficiency.INTERMEDIATE));
        Resume partialMatch = saveResume("pool-5", (byte) 20, true, true, true,
                skill(java, Proficiency.ADVANCED));

        saveResume("pool-hidden", (byte) 30, false, true, true,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.ADVANCED));
        saveResume("pool-ai-off", (byte) 30, true, false, true,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.ADVANCED));
        saveResume("pool-inactive", (byte) 30, true, true, false,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.ADVANCED));
        saveResume("pool-other-skill", (byte) 30, true, true, true,
                skill(aws, Proficiency.ADVANCED));

        // when
        List<CandidatePoolRow> result = resumeQueryRepository.findCandidatePool(
                List.of(java.getId(), spring.getId()),
                10
        );

        // then
        assertThat(result).extracting(CandidatePoolRow::resumeId)
                .containsExactly(
                        strongest.getId(),
                        sameScoreHighCareerLowId.getId(),
                        sameScoreHighCareerHighId.getId(),
                        sameScoreLowCareer.getId(),
                        partialMatch.getId()
                );
        assertThat(result).extracting(CandidatePoolRow::matchedRequiredSkillCount)
                .containsExactly(2L, 2L, 2L, 2L, 1L);
        assertThat(result).extracting(CandidatePoolRow::weightedProficiencySum)
                .containsExactly(13, 8, 8, 8, 9);
    }

    @DisplayName("findCandidatePool은 정렬된 후보 중 limit 개수만 반환한다")
    @Test
    void findCandidatePool_appliesLimitAfterOrdering() {
        // given
        Skill java = skillRepository.saveAndFlush(Skill.create("Java-query-pool-limit", null));
        Skill spring = skillRepository.saveAndFlush(Skill.create("Spring-query-pool-limit", null));

        Resume strongest = saveResume("pool-limit-1", (byte) 5, true, true, true,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.INTERMEDIATE));
        Resume higherCareer = saveResume("pool-limit-2", (byte) 10, true, true, true,
                skill(java, Proficiency.INTERMEDIATE),
                skill(spring, Proficiency.INTERMEDIATE));
        saveResume("pool-limit-3", (byte) 7, true, true, true,
                skill(java, Proficiency.INTERMEDIATE),
                skill(spring, Proficiency.INTERMEDIATE));
        saveResume("pool-limit-4", (byte) 20, true, true, true,
                skill(java, Proficiency.ADVANCED));

        // when
        List<CandidatePoolRow> result = resumeQueryRepository.findCandidatePool(
                List.of(java.getId(), spring.getId()),
                2
        );

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CandidatePoolRow::resumeId)
                .containsExactly(
                        strongest.getId(),
                        higherCareer.getId()
                );
    }

    @DisplayName("findRecommendableResumeIds는 추천 가능 이력서만 경력과 id 순으로 조회한다")
    @Test
    void findRecommendableResumeIds_filtersAndOrdersResumes() {
        // given
        Resume highestCareer = saveResume("recommend-1", (byte) 9, true, true, true);
        Resume mediumCareerLowId = saveResume("recommend-2", (byte) 5, true, true, true);
        Resume mediumCareerHighId = saveResume("recommend-3", (byte) 5, true, true, true);

        saveResume("recommend-hidden", (byte) 99, false, true, true);
        saveResume("recommend-ai-off", (byte) 99, true, false, true);
        saveResume("recommend-inactive", (byte) 99, true, true, false);

        // when
        List<Long> result = resumeQueryRepository.findRecommendableResumeIds(10);

        // then
        assertThat(result).containsExactly(
                highestCareer.getId(),
                mediumCareerLowId.getId(),
                mediumCareerHighId.getId()
        );
    }

    @DisplayName("findCandidatePool은 작성중인 이력서를 후보 풀에서 제외한다")
    @Test
    void findCandidatePool_excludesWritingResumes() {
        // given
        Skill java = skillRepository.saveAndFlush(Skill.create("Java-query-pool-writing", null));
        Skill spring = skillRepository.saveAndFlush(Skill.create("Spring-query-pool-writing", null));

        Resume doneResume = saveResume(
                "pool-done",
                (byte) 8,
                true,
                true,
                true,
                ResumeWritingStatus.DONE,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.INTERMEDIATE)
        );
        saveResume(
                "pool-writing",
                (byte) 20,
                true,
                true,
                true,
                ResumeWritingStatus.WRITING,
                skill(java, Proficiency.ADVANCED),
                skill(spring, Proficiency.ADVANCED)
        );

        // when
        List<CandidatePoolRow> result = resumeQueryRepository.findCandidatePool(
                List.of(java.getId(), spring.getId()),
                10
        );

        // then
        assertThat(result).extracting(CandidatePoolRow::resumeId)
                .containsExactly(doneResume.getId());
    }

    @DisplayName("findRecommendableResumeIds는 정렬된 이력서 중 limit 개수만 반환한다")
    @Test
    void findRecommendableResumeIds_appliesLimitAfterOrdering() {
        // given
        Resume highestCareer = saveResume("recommend-limit-1", (byte) 9, true, true, true);
        Resume mediumCareerLowId = saveResume("recommend-limit-2", (byte) 5, true, true, true);
        saveResume("recommend-limit-3", (byte) 5, true, true, true);
        saveResume("recommend-limit-4", (byte) 1, true, true, true);

        // when
        List<Long> result = resumeQueryRepository.findRecommendableResumeIds(2);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(
                highestCareer.getId(),
                mediumCareerLowId.getId()
        );
    }

    @DisplayName("findRecommendableResumeIds는 작성중인 이력서를 제외한다")
    @Test
    void findRecommendableResumeIds_excludesWritingResumes() {
        // given
        Resume doneResume = saveResume(
                "recommend-done",
                (byte) 7,
                true,
                true,
                true,
                ResumeWritingStatus.DONE
        );
        saveResume(
                "recommend-writing",
                (byte) 30,
                true,
                true,
                true,
                ResumeWritingStatus.WRITING
        );

        // when
        List<Long> result = resumeQueryRepository.findRecommendableResumeIds(10);

        // then
        assertThat(result).containsExactly(doneResume.getId());
    }

    private Resume saveResume(
            String suffix,
            byte careerYears,
            boolean publiclyVisible,
            boolean aiMatchingEnabled,
            boolean active,
            SkillAssignment... skills
    ) {
        return saveResume(
                suffix,
                careerYears,
                publiclyVisible,
                aiMatchingEnabled,
                active,
                ResumeWritingStatus.DONE,
                skills
        );
    }

    private Resume saveResume(
            String suffix,
            byte careerYears,
            boolean publiclyVisible,
            boolean aiMatchingEnabled,
            boolean active,
            ResumeWritingStatus writingStatus,
            SkillAssignment... skills
    ) {
        String phone = "010-1111-" + String.format("%04d", Math.abs(suffix.hashCode() % 10_000));
        Member member = memberRepository.save(createMember(
                "resume-query-" + suffix + "@example.com",
                "hashed-password",
                "name-" + suffix,
                "nickname-" + suffix,
                phone
        ));

        Resume resume = Resume.create(
                member,
                "소개-" + suffix,
                careerYears,
                new CareerPayload(),
                WorkType.REMOTE,
                writingStatus,
                null
        );

        if (!publiclyVisible) {
            resume.togglePubliclyVisible();
        }
        if (!aiMatchingEnabled) {
            resume.toggleAiMatchingEnabled();
        }
        if (!active) {
            resume.delete();
        }
        for (SkillAssignment skill : skills) {
            resume.addSkill(skill.skill(), skill.proficiency());
        }

        return resumeRepository.saveAndFlush(resume);
    }

    private static SkillAssignment skill(Skill skill, Proficiency proficiency) {
        return new SkillAssignment(skill, proficiency);
    }

    private record SkillAssignment(Skill skill, Proficiency proficiency) {

    }
}
