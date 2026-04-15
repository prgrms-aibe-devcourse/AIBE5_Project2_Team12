package com.generic4.itda.domain.resume;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.skill.Skill;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResumeSkillTest {

    @DisplayName("유효한 입력이 주어지면 이력서 스킬을 생성한다")
    @Test
    void createWithValidInputs() {
        Resume resume = createResume();
        Skill skill = Skill.create("Java", "백엔드 언어");

        ResumeSkill resumeSkill = ResumeSkill.create(resume, skill, Proficiency.ADVANCED);

        assertThat(resumeSkill.getResume()).isEqualTo(resume);
        assertThat(resumeSkill.getSkill()).isEqualTo(skill);
        assertThat(resumeSkill.getProficiency()).isEqualTo(Proficiency.ADVANCED);
    }

    @DisplayName("이력서가 누락되면 이력서 스킬 생성에 실패한다")
    @Test
    void failWhenResumeIsNull() {
        Skill skill = Skill.create("Java", "백엔드 언어");

        assertThatThrownBy(() -> ResumeSkill.create(null, skill, Proficiency.BEGINNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이력서는 필수 입력값입니다.");
    }

    @DisplayName("스킬이 누락되면 이력서 스킬 생성에 실패한다")
    @Test
    void failWhenSkillIsNull() {
        Resume resume = createResume();

        assertThatThrownBy(() -> ResumeSkill.create(resume, null, Proficiency.BEGINNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수 입력값입니다.");
    }

    @DisplayName("숙련도가 누락되면 이력서 스킬 생성에 실패한다")
    @Test
    void failWhenProficiencyIsNull() {
        Resume resume = createResume();
        Skill skill = Skill.create("Java", "백엔드 언어");

        assertThatThrownBy(() -> ResumeSkill.create(resume, skill, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("숙련도는 필수 입력값입니다.");
    }

    @DisplayName("유효한 입력이 주어지면 이력서 스킬을 수정한다")
    @Test
    void updateWithValidInputs() {
        Resume resume = createResume();
        Skill skill = Skill.create("Java", "백엔드 언어");
        ResumeSkill resumeSkill = ResumeSkill.create(resume, skill, Proficiency.BEGINNER);

        Skill updatedSkill = Skill.create("Spring Boot", "웹 프레임워크");
        resumeSkill.update(updatedSkill, Proficiency.ADVANCED);

        assertThat(resumeSkill.getSkill()).isEqualTo(updatedSkill);
        assertThat(resumeSkill.getProficiency()).isEqualTo(Proficiency.ADVANCED);
    }

    @DisplayName("수정 시 스킬이 누락되면 실패한다")
    @Test
    void failWhenUpdateSkillIsNull() {
        Resume resume = createResume();
        Skill skill = Skill.create("Java", "백엔드 언어");
        ResumeSkill resumeSkill = ResumeSkill.create(resume, skill, Proficiency.BEGINNER);

        assertThatThrownBy(() -> resumeSkill.update(null, Proficiency.ADVANCED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스킬은 필수 입력값입니다.");
    }

    @DisplayName("수정 시 숙련도가 누락되면 실패한다")
    @Test
    void failWhenUpdateProficiencyIsNull() {
        Resume resume = createResume();
        Skill skill = Skill.create("Java", "백엔드 언어");
        ResumeSkill resumeSkill = ResumeSkill.create(resume, skill, Proficiency.BEGINNER);

        assertThatThrownBy(() -> resumeSkill.update(skill, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("숙련도는 필수 입력값입니다.");
    }

    private static Resume createResume() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot"));

        CareerPayload career = new CareerPayload();
        career.setItems(List.of(item));

        return Resume.create(
                createMember(),
                "백엔드 개발자입니다.",
                (byte) 3,
                career,
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                null
        );
    }
}
