package com.generic4.itda.repository;

import static com.generic4.itda.fixture.MemberFixture.createMember;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.annotation.H2RepositoryTest;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.resume.ResumeSkill;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import com.generic4.itda.domain.skill.Skill;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@H2RepositoryTest
class ResumeSkillRepositoryTest {

    @Autowired
    private ResumeSkillRepository resumeSkillRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SkillRepository skillRepository;

    @DisplayName("같은 이력서에는 동일한 스킬을 중복 저장할 수 없다")
    @Test
    void failWhenSavingDuplicateSkillInSameResume() {
        // given
        Member member = memberRepository.save(createMember());
        Resume resume = resumeRepository.saveAndFlush(createResume(member));
        Skill skill = skillRepository.saveAndFlush(createSkill("Java", null));

        ResumeSkill first = ResumeSkill.create(resume, skill, Proficiency.ADVANCED);
        ResumeSkill second = ResumeSkill.create(resume, skill, Proficiency.INTERMEDIATE);

        resumeSkillRepository.saveAndFlush(first);

        // when & then
        assertThatThrownBy(() -> resumeSkillRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("이력서 ID로 스킬 목록을 조회할 수 있다")
    @Test
    void findAllByResumeId_success() {
        // given
        Member member1 = memberRepository.save(createMember());
        Resume resume1 = resumeRepository.saveAndFlush(createResume(member1));

        Member member2 = memberRepository.save(createMember(
                "other@example.com",
                "다른회원",
                "다른닉네임",
                "010-9999-9999"
        ));
        Resume resume2 = resumeRepository.saveAndFlush(createResume(member2));

        Skill java = skillRepository.saveAndFlush(createSkill("Java", null));
        Skill spring = skillRepository.saveAndFlush(createSkill("Spring", null));
        Skill react = skillRepository.saveAndFlush(createSkill("React", null));

        resumeSkillRepository.saveAndFlush(ResumeSkill.create(resume1, java, Proficiency.ADVANCED));
        resumeSkillRepository.saveAndFlush(ResumeSkill.create(resume1, spring, Proficiency.INTERMEDIATE));
        resumeSkillRepository.saveAndFlush(ResumeSkill.create(resume2, react, Proficiency.BEGINNER));

        // when
        List<ResumeSkill> result = resumeSkillRepository.findAllByResumeId(resume1.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(resumeSkill -> resumeSkill.getSkill().getName())
                .containsExactlyInAnyOrder("Java", "Spring");
    }

    @DisplayName("이력서에 특정 스킬이 등록되어 있는지 확인할 수 있다")
    @Test
    void existsByResumeIdAndSkillId_success() {
        // given
        Member member = memberRepository.save(createMember());
        Resume resume = resumeRepository.saveAndFlush(createResume(member));
        Skill skill = skillRepository.saveAndFlush(createSkill("Java", null));

        ResumeSkill resumeSkill = ResumeSkill.create(resume, skill, Proficiency.ADVANCED);
        resumeSkillRepository.saveAndFlush(resumeSkill);

        // when
        boolean result = resumeSkillRepository.existsByResumeIdAndSkillId(resume.getId(), skill.getId());

        // then
        assertThat(result).isTrue();
    }

    @DisplayName("이력서에 등록되지 않은 스킬이면 false를 반환한다")
    @Test
    void existsByResumeIdAndSkillId_returnsFalseWhenSkillNotRegistered() {
        // given
        Member member = memberRepository.save(createMember());
        Resume resume = resumeRepository.saveAndFlush(createResume(member));
        Skill savedSkill = skillRepository.saveAndFlush(createSkill("Java", null));
        Skill notRegisteredSkill = skillRepository.saveAndFlush(createSkill("Spring", null));

        resumeSkillRepository.saveAndFlush(
                ResumeSkill.create(resume, savedSkill, Proficiency.ADVANCED)
        );

        // when
        boolean result = resumeSkillRepository.existsByResumeIdAndSkillId(
                resume.getId(),
                notRegisteredSkill.getId()
        );

        // then
        assertThat(result).isFalse();
    }

    private static Resume createResume(Member member) {
        return Resume.create(
                member,
                "백엔드 개발자입니다.",
                (byte) 3,
                createCareerPayload(),
                WorkType.HYBRID,
                ResumeWritingStatus.WRITING,
                "https://github.com/test"
        );
    }

    private static Skill createSkill(String name, String description) {
        return Skill.create(name, description);
    }

    private static CareerPayload createCareerPayload() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.getItems().add(item);
        return payload;
    }
}
