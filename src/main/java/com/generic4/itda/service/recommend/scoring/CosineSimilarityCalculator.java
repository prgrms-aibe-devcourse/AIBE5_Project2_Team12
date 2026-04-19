package com.generic4.itda.service.recommend.scoring;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CosineSimilarityCalculator {

    public double calculate(List<Double> queryEmbedding, List<Double> targetEmbedding) {
        validate(queryEmbedding, targetEmbedding);

        double dotProduct = 0.0;
        double queryNorm = 0.0;
        double targetNorm = 0.0;

        for (int i = 0; i < queryEmbedding.size(); i++) {
            double q = queryEmbedding.get(i);
            double t = targetEmbedding.get(i);

            dotProduct += q * t;
            queryNorm += q * q;
            targetNorm += t * t;
        }

        if (queryNorm == 0.0 || targetNorm == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(queryNorm) * Math.sqrt(targetNorm));
    }

    private void validate(List<Double> queryEmbedding, List<Double> targetEmbedding) {
        if (queryEmbedding == null || targetEmbedding == null) {
            throw new IllegalArgumentException("embedding 은 null 일 수 업습니다.");
        }
        if (queryEmbedding.isEmpty() || targetEmbedding.isEmpty()) {
            throw new IllegalArgumentException("embedding 은 비어 있을 수 없습니다.");
        }
        if (queryEmbedding.size() != targetEmbedding.size()) {
            throw new IllegalArgumentException("embedding 차원이 다릅니다.");
        }
    }
}
