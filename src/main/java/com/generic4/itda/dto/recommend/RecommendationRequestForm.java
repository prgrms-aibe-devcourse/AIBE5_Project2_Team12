package com.generic4.itda.dto.recommend;

import jakarta.validation.constraints.NotNull;

public record RecommendationRequestForm(
        @NotNull(message = "추천 대상 포지션을 선택해주세요.")
        Long proposalPositionId
) {

}
