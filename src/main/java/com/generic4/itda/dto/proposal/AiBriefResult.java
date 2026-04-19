package com.generic4.itda.dto.proposal;

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
    private final Long expectedPeriod;
    private final List<AiBriefPositionResult> positions;

    @Builder(access = AccessLevel.PRIVATE)
    private AiBriefResult(String title, String description, Long totalBudgetMin, Long totalBudgetMax,
            Long expectedPeriod, List<AiBriefPositionResult> positions) {
        this.title = title;
        this.description = description;
        this.totalBudgetMin = totalBudgetMin;
        this.totalBudgetMax = totalBudgetMax;
        this.expectedPeriod = expectedPeriod;
        this.positions = positions == null ? List.of() : List.copyOf(positions);
    }

    public static AiBriefResult of(String title, String description, Long totalBudgetMin, Long totalBudgetMax,
            Long expectedPeriod, List<AiBriefPositionResult> positions) {
        return AiBriefResult.builder()
                .title(title)
                .description(description)
                .totalBudgetMin(totalBudgetMin)
                .totalBudgetMax(totalBudgetMax)
                .expectedPeriod(expectedPeriod)
                .positions(positions)
                .build();
    }
}
