package com.generic4.itda.dto.resume;

import com.generic4.itda.domain.resume.Proficiency;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResumeSkillItemForm {

    @NotNull(message = "스킬은 필수값입니다.")
    private Long skillId;

    @NotNull(message = "숙련도는 필수값입니다.")
    private Proficiency proficiency;
}

