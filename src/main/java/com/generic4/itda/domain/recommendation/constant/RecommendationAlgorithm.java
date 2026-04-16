package com.generic4.itda.domain.recommendation.constant;

import lombok.Getter;

@Getter
public enum RecommendationAlgorithm {
    /**
     * MVP 버전: 하드 필터링 + 코사인 유사도(Java 계산) + 스킬/경력 가중치 보정
     */
    HEURISTIC_V1("v1"),

    /**
     * 향후 확장: pgvector 도입 및 HNSW 인덱스 활용 버전
     */
    VECTOR_ENGINE_V1("v2");

    private final String code;

    RecommendationAlgorithm(String code) {
        this.code = code;
    }

    @Getter
    public enum RecommendationRunStatus {
        PENDING("대기중"), RUNNING("실행중"), COMPUTED("계산 완료"), FAILED("실패");

        private final String description;

        RecommendationRunStatus(String description) {
            this.description = description;
        }
    }
}
