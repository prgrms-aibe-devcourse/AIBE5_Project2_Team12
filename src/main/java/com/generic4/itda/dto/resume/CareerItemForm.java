package com.generic4.itda.dto.resume;

import com.generic4.itda.domain.resume.CareerEmploymentType;
import com.generic4.itda.domain.resume.CareerItemPayload;
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
public class CareerItemForm {

    private Integer index; // 수정/삭제 시 사용

    private String companyName;
    private String position;
    private CareerEmploymentType employmentType;
    private String startYearMonth;
    private String endYearMonth;
    private Boolean currentlyWorking;
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
