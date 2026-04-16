package com.generic4.itda.dto.proposal;

import com.generic4.itda.domain.proposal.ProposalWorkType;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AiBriefResult {

    private final String title;
    private final String description;
    private final Long totalBudgetMin;
    private final Long totalBudgetMax;
    private final ProposalWorkType workType;
    private final String workPlace;
    private final Long expectedPeriod;
    private final List<AiBriefPositionResult> positions;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefResult(String title, String description, Long totalBudgetMin, Long totalBudgetMax,
            ProposalWorkType workType, String workPlace, Long expectedPeriod,
            List<AiBriefPositionResult> positions) {
        this.title = title;
        this.description = description;
        this.totalBudgetMin = totalBudgetMin;
        this.totalBudgetMax = totalBudgetMax;
        this.workType = workType;
        this.workPlace = workPlace;
        this.expectedPeriod = expectedPeriod;
        this.positions = positions == null ? List.of() : List.copyOf(positions);
    }

    public static AiBriefResult of(String title, String description, Long totalBudgetMin, Long totalBudgetMax,
            ProposalWorkType workType, String workPlace, Long expectedPeriod,
            List<AiBriefPositionResult> positions) {
        return AiBriefResult.builder()
                .title(title)
                .description(description)
                .totalBudgetMin(totalBudgetMin)
                .totalBudgetMax(totalBudgetMax)
                .workType(workType)
                .workPlace(workPlace)
                .expectedPeriod(expectedPeriod)
                .positions(positions)
                .build();
    }
}
