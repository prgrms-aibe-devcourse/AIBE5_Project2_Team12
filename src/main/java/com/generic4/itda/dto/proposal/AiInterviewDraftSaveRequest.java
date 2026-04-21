package com.generic4.itda.dto.proposal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiInterviewDraftSaveRequest {

    @NotBlank(message = "제안서 제목은 필수값입니다.")
    @Size(max = 200, message = "제안서 제목은 200자를 초과할 수 없습니다.")
    private String title;

    @Size(max = 10000, message = "설명은 10000자를 초과할 수 없습니다.")
    private String description;

    @Min(value = 1, message = "예상 기간은 1 이상이어야 합니다.")
    private Long expectedPeriod;

    @Valid
    private List<ProposalPositionForm> positions = new ArrayList<>();
}