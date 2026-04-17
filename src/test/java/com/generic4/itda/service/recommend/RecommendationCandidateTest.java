package com.generic4.itda.service.recommend;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeStatus;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RecommendationCandidateTest {

    @DisplayName("skills 리스트는 방어적으로 복사되고 외부에서 수정할 수 없다")
    @Test
    void constructor_makesDefensiveCopyOfSkills() {
        // given
        List<RecommendationCandidate.CandidateSkill> original = new ArrayList<>();
        original.add(new RecommendationCandidate.CandidateSkill(1L, "Java", Proficiency.ADVANCED));

        RecommendationCandidate candidate = new RecommendationCandidate(
                100L,
                ResumeStatus.ACTIVE,
                true,
                true,
                (byte) 5,
                original
        );

        // when
        original.add(new RecommendationCandidate.CandidateSkill(2L, "Spring", Proficiency.INTERMEDIATE));

        // then
        assertThat(candidate.skills()).hasSize(1);
        assertThatThrownBy(() -> candidate.skills()
                .add(new RecommendationCandidate.CandidateSkill(3L, "Kotlin", Proficiency.BEGINNER)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @DisplayName("ACTIVE 상태일 때만 활성 후보로 판단한다")
    @Test
    void isActive_returnsTrueOnlyWhenStatusIsActive() {
        RecommendationCandidate active = new RecommendationCandidate(
                1L,
                ResumeStatus.ACTIVE,
                true,
                true,
                (byte) 3,
                List.of()
        );
        RecommendationCandidate inactive = new RecommendationCandidate(
                2L,
                ResumeStatus.INACTIVE,
                true,
                true,
                (byte) 3,
                List.of()
        );

        assertThat(active.isActive()).isTrue();
        assertThat(inactive.isActive()).isFalse();
    }

    @DisplayName("CandidateSkill.of는 ResumeSkill의 필드를 계산용 DTO로 복사한다")
    @Test
    void candidateSkillOf_mapsResumeSkillFields() {
        // given
        Skill java = Skill.create("Java", null);
        ReflectionTestUtils.setField(java, "id", 10L);

        Resume resume = Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 5,
                new CareerPayload(),
                WorkType.REMOTE,
                ResumeWritingStatus.DONE,
                null
        );
        ResumeSkill resumeSkill = ResumeSkill.create(resume, java, Proficiency.ADVANCED);

        // when
        RecommendationCandidate.CandidateSkill result = RecommendationCandidate.CandidateSkill.of(resumeSkill);

        // then
        assertThat(result.skillId()).isEqualTo(10L);
        assertThat(result.skillName()).isEqualTo("Java");
        assertThat(result.proficiency()).isEqualTo(Proficiency.ADVANCED);
    }
}
