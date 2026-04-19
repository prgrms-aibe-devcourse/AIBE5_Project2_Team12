package com.generic4.itda.dto.resume;

import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.dto.resume.validation.ValidCareerPeriod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@ValidCareerPeriod
public class CareerItemForm {

    private static final String YEAR_MONTH_REGEX = "^\\d{4}-(0[1-9]|1[0-2])$";

    private Integer index; // 수정/삭제 시 사용

    @NotBlank(message = "회사명은 필수값입니다.")
    @Size(max = 100, message = "회사명은 100자를 초과할 수 없습니다.")
    private String companyName;

    @NotBlank(message = "직책은 필수값입니다.")
    @Size(max = 100, message = "직책은 100자를 초과할 수 없습니다.")
    private String position;

    @NotNull(message = "고용 형태는 필수값입니다.")
    private CareerEmploymentType employmentType;

    @NotBlank(message = "시작 연월은 필수값입니다.")
    @Pattern(regexp = YEAR_MONTH_REGEX, message = "시작 연월은 yyyy-MM 형식이어야 합니다.")
    private String startYearMonth;

    @Pattern(regexp = YEAR_MONTH_REGEX, message = "종료 연월은 yyyy-MM 형식이어야 합니다.")
    private String endYearMonth;

    @NotNull(message = "재직 여부는 필수값입니다.")
    private Boolean currentlyWorking;

    @Size(max = 1000, message = "요약은 1000자를 초과할 수 없습니다.")
    private String summary;

    private String techStackRaw; // 쉼표 구분 문자열

    public CareerItemPayload toPayload() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName(companyName);
        item.setPosition(position);
        item.setEmploymentType(employmentType);
        item.setStartYearMonth(startYearMonth);
        item.setCurrentlyWorking(Boolean.TRUE.equals(currentlyWorking));
        item.setEndYearMonth(Boolean.TRUE.equals(currentlyWorking) ? null : endYearMonth);
        item.setSummary(summary);

        if (techStackRaw != null && !techStackRaw.isBlank()) {
            List<String> stack = Arrays.stream(techStackRaw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            item.setTechStack(stack);
        } else {
            item.setTechStack(Collections.emptyList());
        }

        return item;
    }
}
