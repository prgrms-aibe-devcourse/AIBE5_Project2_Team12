package com.generic4.itda.dto.resume;

import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Proficiency;
import com.generic4.itda.domain.resume.ResumeWritingStatus;
import com.generic4.itda.domain.resume.WorkType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResumeForm {

    @NotBlank(message = "자기소개는 필수값입니다.")
    @Size(max = 2000, message = "자기소개는 2000자를 초과할 수 없습니다.")
    private String introduction;

    @NotNull(message = "경력 연차는 필수값입니다.")
    @Min(value = 0, message = "경력 연차는 0 이상이어야 합니다.")
    @Max(value = 50, message = "경력 연차는 50을 초과할 수 없습니다.")
    private Byte careerYears;

    @Valid
    @NotNull(message = "경력 정보는 필수값입니다.")
    private CareerPayload career = new CareerPayload();

    private WorkType preferredWorkType;

    private String portfolioUrl;

    @NotNull(message = "작성 상태는 필수값입니다.")
    private ResumeWritingStatus writingStatus;

    private boolean publiclyVisible = true;

    private boolean aiMatchingEnabled = true;
}