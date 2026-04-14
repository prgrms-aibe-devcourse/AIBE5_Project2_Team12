package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.shared.BaseTimeEntity;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeSkill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    private Proficiency proficiency;

    private ResumeSkill(Resume resume, Skill skill, Proficiency proficiency) {
        Assert.notNull(resume, "이력서는 필수 입력값입니다.");
        Assert.notNull(skill, "스킬은 필수 입력값입니다.");
        Assert.notNull(proficiency, "숙련도는 필수 입력값입니다.");

        this.resume = resume;
        this.skill = skill;
        this.proficiency = proficiency;
    }

    public static ResumeSkill create(Resume resume, Skill skill, Proficiency proficiency) {
        return new ResumeSkill(resume, skill, proficiency);
    }

    public void update(Skill skill, Proficiency proficiency) {
        Assert.notNull(skill, "스킬은 필수 입력값입니다.");
        Assert.notNull(proficiency, "숙련도는 필수 입력값입니다.");

        this.skill = skill;
        this.proficiency = proficiency;
    }
}
