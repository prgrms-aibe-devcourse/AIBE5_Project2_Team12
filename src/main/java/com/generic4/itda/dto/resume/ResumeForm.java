package com.generic4.itda.dto.resume;

import com.generic4.itda.domain.resume.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
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

    @Valid
    private List<ResumeSkillItemForm> skillItems = new ArrayList<>();

    public static ResumeForm from(Resume resume) {
        ResumeForm form = new ResumeForm();
        form.setIntroduction(resume.getIntroduction());
        form.setCareerYears(resume.getCareerYears());
        form.setCareer(resume.getCareer());
        form.setPreferredWorkType(resume.getPreferredWorkType());
        form.setPortfolioUrl(resume.getPortfolioUrl());
        form.setWritingStatus(resume.getWritingStatus());
        form.setPubliclyVisible(resume.isPubliclyVisible());
        form.setAiMatchingEnabled(resume.isAiMatchingEnabled());
        return form;
    }

    public static ResumeForm createDefault() {
        ResumeForm form = new ResumeForm();
        form.setCareerYears((byte) 0);
        form.setWritingStatus(ResumeWritingStatus.WRITING);
        form.setIntroduction("");
        form.setPubliclyVisible(true);
        form.setAiMatchingEnabled(true);
        return form;
    }
}
