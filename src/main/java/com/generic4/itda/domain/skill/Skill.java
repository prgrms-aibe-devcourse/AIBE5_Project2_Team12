package com.generic4.itda.domain.skill;

import com.generic4.itda.domain.shared.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = false)
public class Skill extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 2000)
    private String description;

    private Skill(String name, String description) {
        Assert.hasText(name, "스킬 이름은 필수값입니다.");

        this.name = name.trim();
        this.description = StringUtils.hasText(description) ? description.trim() : null;
    }

    public static Skill create(String name, String description) {
        return new Skill(name, description);
    }

    public void update(String name, String description) {
        Assert.hasText(name, "스킬 이름은 필수값입니다.");

        this.name = name.trim();
        this.description = StringUtils.hasText(description) ? description.trim() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Skill that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
