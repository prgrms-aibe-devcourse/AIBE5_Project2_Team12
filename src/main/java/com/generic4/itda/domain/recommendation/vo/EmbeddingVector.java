package com.generic4.itda.domain.recommendation.vo;

import java.util.List;
import org.springframework.util.Assert;

public record EmbeddingVector(
        List<Double> values
) {

    public EmbeddingVector(List<Double> values) {
        Assert.notNull(values, "embeddingVector는 필수입니다.");
        Assert.notEmpty(values, "embeddingVector는 비어 있을 수 없습니다.");
        Assert.noNullElements(values, "embeddingVector에는 null 원소가 포함될 수 없습니다.");

        this.values = List.copyOf(values);
    }

    public int dimension() {
        return values.size();
    }
}
