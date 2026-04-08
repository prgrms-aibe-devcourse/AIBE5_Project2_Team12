package com.generic4.itda.domain.resume;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
public class CareerPayload {

    @NotNull(message = "경력 스키마 버전은 필수값입니다.")
    @Min(value = 1, message = "경력 스키마 버전은 1 이상이어야 합니다.")
    private Integer version = 1;

    @Valid
    @NotNull(message = "경력 목록은 필수값입니다.")
    @Size(max = 50, message = "경력 항목은 50개를 초과할 수 없습니다.")
    private List<CareerItemPayload> items = new ArrayList<>();
}
