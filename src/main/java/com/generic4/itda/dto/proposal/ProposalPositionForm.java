package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalPositionStatus;
import com.generic4.itda.domain.proposal.ProposalWorkType;
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
public class ProposalPositionForm {

    private Long id;

    @NotNull(message = "직무는 필수값입니다.")
    private Long positionId;

    @NotBlank(message = "포지션 제목은 필수값입니다.")
    @Size(max = 200, message = "포지션 제목은 200자를 초과할 수 없습니다.")
    private String title;

    @NotNull(message = "근무 형태는 필수값입니다.")
    private ProposalWorkType workType;

    @NotNull(message = "모집 인원은 필수값입니다.")
    @Min(value = 1, message = "모집 인원은 1 이상이어야 합니다.")
    private Long headCount;

    @Min(value = 0, message = "최소 예산은 0 이상이어야 합니다.")
    private Long unitBudgetMin;

    @Min(value = 0, message = "최대 예산은 0 이상이어야 합니다.")
    private Long unitBudgetMax;

    @Min(value = 1, message = "예상 기간은 1 이상이어야 합니다.")
    private Long expectedPeriod;

    @Min(value = 0, message = "최소 경력 연차는 0 이상이어야 합니다.")
    private Integer careerMinYears;

    @Min(value = 0, message = "최대 경력 연차는 0 이상이어야 합니다.")
    private Integer careerMaxYears;

    @Size(max = 255, message = "근무지는 255자를 초과할 수 없습니다.")
    private String workPlace;

    private ProposalPositionStatus status;

    private List<String> essentialSkillNames = new ArrayList<>();

    private List<String> preferredSkillNames = new ArrayList<>();

    public static ProposalPositionForm from(ProposalPosition proposalPosition) {
        ProposalPositionForm form = new ProposalPositionForm();
        form.setId(proposalPosition.getId());
        form.setPositionId(proposalPosition.getPosition().getId());
        form.setTitle(proposalPosition.getTitle());
        form.setWorkType(proposalPosition.getWorkType());
        form.setHeadCount(proposalPosition.getHeadCount());
        form.setUnitBudgetMin(proposalPosition.getUnitBudgetMin());
        form.setUnitBudgetMax(proposalPosition.getUnitBudgetMax());
        form.setExpectedPeriod(proposalPosition.getExpectedPeriod());
        form.setCareerMinYears(proposalPosition.getCareerMinYears());
        form.setCareerMaxYears(proposalPosition.getCareerMaxYears());
        form.setWorkPlace(proposalPosition.getWorkPlace());
        form.setStatus(proposalPosition.getStatus());
        form.setEssentialSkillNames(proposalPosition.getSkills().stream()
                .filter(skill -> skill.getImportance() == ProposalPositionSkillImportance.ESSENTIAL)
                .map(skill -> skill.getSkill().getName())
                .toList());
        form.setPreferredSkillNames(proposalPosition.getSkills().stream()
                .filter(skill -> skill.getImportance() == ProposalPositionSkillImportance.PREFERENCE)
                .map(skill -> skill.getSkill().getName())
                .toList());
        return form;
    }
}
