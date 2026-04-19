package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
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
public class ProposalForm {

    @NotBlank(message = "제안서 제목은 필수값입니다.")
    @Size(max = 200, message = "제안서 제목은 200자를 초과할 수 없습니다.")
    private String title;

    @Size(max = 10000, message = "제안서 원본 입력은 10000자를 초과할 수 없습니다.")
    private String rawInputText;

    @Size(max = 10000, message = "설명은 10000자를 초과할 수 없습니다.")
    private String description;

    @Min(value = 0, message = "최소 예산은 0 이상이어야 합니다.")
    private Long totalBudgetMin;

    @Min(value = 0, message = "최대 예산은 0 이상이어야 합니다.")
    private Long totalBudgetMax;

    /**
     * 기존 단일 폼 화면과의 호환을 위해 잠시 유지합니다.
     * 실제 저장 모델은 proposal_position으로 이동합니다.
     */
    private ProposalWorkType workType;

    /**
     * 기존 단일 폼 화면과의 호환을 위해 잠시 유지합니다.
     * 실제 저장 모델은 proposal_position으로 이동합니다.
     */
    @Size(max = 255, message = "근무 장소는 255자를 초과할 수 없습니다.")
    private String workPlace;

    @Min(value = 1, message = "예상 기간은 1 이상이어야 합니다.")
    private Long expectedPeriod;

    private ProposalStatus status;

    @Valid
    private List<ProposalPositionForm> positions = new ArrayList<>();

    public static ProposalForm from(Proposal proposal) {
        ProposalForm form = new ProposalForm();
        form.setTitle(proposal.getTitle());
        form.setRawInputText(proposal.getRawInputText());
        form.setDescription(proposal.getDescription());
        form.setTotalBudgetMin(proposal.getTotalBudgetMin());
        form.setTotalBudgetMax(proposal.getTotalBudgetMax());
        form.setExpectedPeriod(proposal.getExpectedPeriod());
        form.setStatus(proposal.getStatus());
        form.setPositions(proposal.getPositions().stream()
                .map(ProposalPositionForm::from)
                .toList());
        return form;
    }

    public static ProposalForm createDefault() {
        ProposalForm form = new ProposalForm();
        form.setStatus(ProposalStatus.WRITING);
        form.setTitle("");
        form.setRawInputText("");
        form.setPositions(new ArrayList<>());
        return form;
    }
}
