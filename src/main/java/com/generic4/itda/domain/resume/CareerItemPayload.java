package com.generic4.itda.domain.resume;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CareerItemPayload {

    private static final String YEAR_MONTH_REGEX = "^\\d{4}-(0[1-9]|1[0-2])$";

    @NotBlank(message = "회사명은 필수값입니다.")
    @Size(max = 100, message = "회사명은 100자를 초과할 수 없습니다.")
    private String companyName;

    @NotBlank(message = "직무명은 필수값입니다.")
    @Size(max = 100, message = "직무명은 100자를 초과할 수 없습니다.")
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

    @Size(max = 1000, message = "경력 요약은 1000자를 초과할 수 없습니다.")
    private String summary;

    @Size(max = 20, message = "기술 스택은 20개를 초과할 수 없습니다.")
    private List<
            @NotBlank(message = "기술 스택 항목은 비어 있을 수 없습니다.")
            @Size(max = 50, message = "기술 스택 항목은 50자를 초과할 수 없습니다.")
            String> techStack = new ArrayList<>();

    @JsonIgnore
    @AssertTrue(message = "재직중인 경력은 종료 연월을 비워야 합니다.")
    public boolean isEndYearMonthEmptyWhenCurrentlyWorking() {
        if (currentlyWorking == null || !currentlyWorking) {
            return true;
        }
        return endYearMonth == null;
    }

    @JsonIgnore
    @AssertTrue(message = "재직중이 아닌 경력은 종료 연월이 필요합니다.")
    public boolean isEndYearMonthPresentWhenNotCurrentlyWorking() {
        if (currentlyWorking == null || currentlyWorking) {
            return true;
        }
        return endYearMonth != null && !endYearMonth.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "종료 연월은 시작 연월보다 빠를 수 없습니다.")
    public boolean isEndYearMonthNotBeforeStartYearMonth() {
        YearMonth start = parseYearMonth(startYearMonth);
        YearMonth end = parseYearMonth(endYearMonth);

        if (start == null || end == null) {
            return true;
        }
        return !end.isBefore(start);
    }

    private YearMonth parseYearMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return YearMonth.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
