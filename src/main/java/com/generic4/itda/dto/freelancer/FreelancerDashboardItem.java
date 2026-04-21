package com.generic4.itda.dto.freelancer;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public record FreelancerDashboardItem(
        Long proposalId,
        String proposalTitle,
        String companyName,
        String positionName,
        MatchingStatus matchingStatus,
        Long budgetMin,
        Long budgetMax,
        LocalDateTime receivedAt
) {

    /**
     * Thymeleaf 템플릿에서 `item.status()` 형태로 호출하고 있어 호환을 위해 제공한다.
     */
    public DashboardProposalStatus status() {
        return getStatus();
    }

    public DashboardProposalStatus getStatus() {
        if (this.matchingStatus == null) return DashboardProposalStatus.NEW;

        return switch (this.matchingStatus) {
            // 1. 새로운 제안
            case PROPOSED -> DashboardProposalStatus.NEW;

            // 2. 수락 후 프로젝트 수행 중인 단계
            case ACCEPTED, IN_PROGRESS -> DashboardProposalStatus.IN_PROGRESS;

            // 3. 프로젝트가 끝났거나(COMPLETED), 성사되지 않고 종료된 경우
            case COMPLETED, REJECTED, CANCELED -> DashboardProposalStatus.COMPLETED;

            default -> DashboardProposalStatus.NEW;
        };
    }

    public enum DashboardProposalStatus {
        NEW("신규 제안", "bg-blue-100 text-blue-700"),
        IN_PROGRESS("진행 중", "bg-emerald-100 text-emerald-700"),
        COMPLETED("완료", "bg-slate-900 text-white"); // 프로젝트 수행 완료와 취소된 건은 완료로 통칭

        private final String label;
        private final String cssClass;

        DashboardProposalStatus(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }

        public String getLabel() { return label; }
        public String getCssClass() { return cssClass; }
    }


    public String formattedBudget() {
        if (budgetMin == null && budgetMax == null) return "협의";
        if (budgetMin == null) return "~ " + formatWon(budgetMax);
        if (budgetMax == null) return formatWon(budgetMin) + " ~";
        if (budgetMin.equals(budgetMax)) return "월 " + formatWon(budgetMin);
        return "월 " + formatWon(budgetMin) + " ~ " + formatWon(budgetMax);
    }

    private static String formatWon(Long amount) {
        return String.format("%,d만원", amount / 10_000);
    }
}
