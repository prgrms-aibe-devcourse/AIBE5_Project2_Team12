package com.generic4.itda.domain.resume;

import com.generic4.itda.domain.shared.BaseTimeEntity;
import com.generic4.itda.domain.skill.Skill;
import jakarta.persistence.*;

import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resume_skill_resume_id_skill_id",
                        columnNames = {"resume_id", "skill_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeSkill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 필수 연관관계이므로 DB에서도 null을 허용하지 않음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    // 필수 연관관계이므로 DB에서도 null을 허용하지 않음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)  // 필수값이므로 DB에서도 null 방어
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResumeSkill that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
