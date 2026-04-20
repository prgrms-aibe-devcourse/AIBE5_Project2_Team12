package com.generic4.itda.domain.recommendation;

import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.recommendation.constant.RecommendationAlgorithm;
import com.generic4.itda.domain.recommendation.constant.RecommendationRunStatus;
import com.generic4.itda.domain.recommendation.converter.HardFilterStatConverter;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_run_position_fingerprint_algorithm",
                        columnNames = {"proposal_position_id", "request_fingerprint", "algorithm_version"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationRun extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_position_id", nullable = false, updatable = false)
    private ProposalPosition proposalPosition;

    @Column(name = "request_fingerprint", length = 128, nullable = false)
    private String requestFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_version", length = 50, nullable = false)
    private RecommendationAlgorithm algorithm;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    @Column(nullable = false)
    private int topK;

    @Convert(converter = HardFilterStatConverter.class)
    @Column(name = "hard_filter_stats", columnDefinition = "TEXT")
    private HardFilterStat hardFilterStats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationRunStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private RecommendationRun(
            ProposalPosition proposalPosition,
            String requestFingerprint,
            RecommendationAlgorithm algorithm,
            int topK
    ) {
        Assert.notNull(proposalPosition, "제안 포지션은 필수입니다.");
        Assert.hasText(requestFingerprint, "요청 fingerprint는 필수입니다.");
        Assert.notNull(algorithm, "추천 알고리즘은 필수입니다.");
        Assert.isTrue(topK > 0, "topK는 1 이상이어야 합니다.");

        this.proposalPosition = proposalPosition;
        this.requestFingerprint = requestFingerprint;
        this.algorithm = algorithm;
        this.topK = topK;

        this.status = RecommendationRunStatus.PENDING;
    }

    public static RecommendationRun create(
            ProposalPosition proposalPosition,
            String requestFingerprint,
            RecommendationAlgorithm algorithm,
            int topK
    ) {
        return new RecommendationRun(proposalPosition, requestFingerprint, algorithm, topK);
    }

    public void markRunning() {
        Assert.state(this.status == RecommendationRunStatus.PENDING,
                "PENDING 상태에서만 실행 시작할 수 있습니다.");

        this.status = RecommendationRunStatus.RUNNING;
        this.errorMessage = null;
    }

    public void markCompleted(HardFilterStat stat) {
        Assert.state(this.status == RecommendationRunStatus.RUNNING,
                "RUNNING 상태에서만 완료 처리할 수 있습니다.");
        Assert.notNull(stat, "하드 필터 통계는 필수입니다.");

        this.status = RecommendationRunStatus.COMPUTED;
        this.hardFilterStats = stat;
        this.candidateCount = stat.finalCandidates();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        Assert.state(this.status == RecommendationRunStatus.RUNNING,
                "RUNNING 상태에서만 완료 처리할 수 있습니다.");
        Assert.hasText(errorMessage, "실패 사유는 필수입니다.");

        this.status = RecommendationRunStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public boolean isPending() {
        return this.status == RecommendationRunStatus.PENDING;
    }

    public boolean isRunning() {
        return this.status == RecommendationRunStatus.RUNNING;
    }

    public boolean isComputed() {
        return this.status == RecommendationRunStatus.COMPUTED;
    }

    public boolean isFailed() {
        return this.status == RecommendationRunStatus.FAILED;
    }

    public boolean isTerminalStatus() {
        return isComputed() || isFailed();
    }
}
