package com.generic4.itda.domain.recommendation;

import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_result_run_reesume",
                        columnNames = {"recommendation_run_id", "resume_id"}
                ),
                @UniqueConstraint(
                        name = "uk_recommendation_result_run_rank",
                        columnNames = {"recommendation_run_id", "rank"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationResult extends BaseEntity {

    private static final BigDecimal MIN_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAX_SCORE = BigDecimal.ONE;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_run_id", nullable = false, updatable = false)
    private RecommendationRun recommendationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, updatable = false)
    private Resume resume;

    @Column(nullable = false)
    private int rank;

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal finalScore;

    @Column(precision = 5, scale = 4, nullable = false)
    private BigDecimal embeddingScore;

    @Convert(converter = ReasonFactsConverter.class)
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private ReasonFacts reasonFacts;

    @Column(columnDefinition = "TEXT")
    private String llmReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LlmStatus llmStatus;

    private RecommendationResult(
            RecommendationRun recommendationRun,
            Resume resume,
            int rank,
            BigDecimal finalScore,
            BigDecimal embeddingScore,
            ReasonFacts reasonFacts,
            String llmReason,
            LlmStatus llmStatus
    ) {
        validateAssociation(recommendationRun, resume);
        Assert.isTrue(rank > 0, "추천 순위는 1 이상이어야 합니다.");
        validateScore(finalScore, "최종 점수는 필수값입니다.");
        validateScore(embeddingScore, "임베딩 점수는 필수값입니다.");
        Assert.notNull(reasonFacts, "추천 근거 데이터는 필수값입니다.");
        validateLlmFields(llmReason, llmStatus);

        this.recommendationRun = recommendationRun;
        this.resume = resume;
        this.rank = rank;
        this.finalScore = finalScore;
        this.embeddingScore = embeddingScore;
        this.reasonFacts = reasonFacts;
        this.llmReason = normalizeReason(llmReason);
        this.llmStatus = llmStatus;
    }

    public static RecommendationResult create(
            RecommendationRun recommendationRun,
            Resume resume,
            int rank,
            BigDecimal finalScore,
            BigDecimal embeddingScore,
            ReasonFacts reasonFacts
    ) {
        return new RecommendationResult(
                recommendationRun,
                resume,
                rank,
                finalScore,
                embeddingScore,
                reasonFacts,
                null,
                LlmStatus.PENDING
        );
    }

    public void markLlmReady(String llmReason) {
        Assert.state(this.llmStatus == LlmStatus.PENDING, "LLM 상태는 PENDING에서만 변경할 수 있습니다.");
        Assert.hasText(llmReason, "생성된 추천 설명은 비어있을 수 없습니다.");

        this.llmReason = llmReason.trim();
        this.llmStatus = LlmStatus.READY;
    }

    public void markLlmFailed() {
        Assert.state(this.llmStatus == LlmStatus.PENDING, "LLM 상태는 PENDING에서만 변경할 수 있습니다.");
        
        this.llmReason = null;
        this.llmStatus = LlmStatus.FAILED;
    }

    private static void validateAssociation(RecommendationRun recommendationRun, Resume resume) {
        Assert.notNull(recommendationRun, "추천 실행 정보는 필수값입니다.");
        Assert.notNull(resume, "추천 대상 이력서는 필수값입니다.");
    }

    private static void validateScore(BigDecimal score, String nullMessage) {
        Assert.notNull(score, nullMessage);
        Assert.isTrue(score.compareTo(MIN_SCORE) >= 0, "점수는 0 이상이어야 합니다.");
        Assert.isTrue(score.compareTo(MAX_SCORE) <= 0, "점수는 1 이하여야 합니다.");
    }

    private static void validateLlmFields(String llmReason, LlmStatus llmStatus) {
        Assert.notNull(llmStatus, "LLM 상태는 필수값입니다.");

        if (llmStatus == LlmStatus.READY) {
            Assert.hasText(llmReason, "LLM 상태가 READY이면 추천 설명은 필수값입니다.");
            return;
        }

        Assert.isTrue(!StringUtils.hasText(llmReason), "LLM 상태가 READY가 아니면 추천 설명을 저장할 수 없다.");
    }

    private static String normalizeReason(String llmReason) {
        return StringUtils.hasText(llmReason) ? llmReason.trim() : null;
    }
}
