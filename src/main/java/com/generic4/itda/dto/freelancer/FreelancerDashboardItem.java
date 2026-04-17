package com.generic4.itda.dto.freelancer;

import java.time.LocalDate;
import java.util.List;

public record FreelancerDashboardItem(
        Long proposalId,
        String proposalTitle,
        String companyName,
        String positionName,
        DashboardProposalStatus status,
        Long budgetMin,
        Long budgetMax,
        LocalDate receivedAt,
        List<String> skillNames
) {

    public enum DashboardProposalStatus {
        NEW("신규 제안", "bg-blue-100 text-blue-700"),
        IN_PROGRESS("진행 중", "bg-emerald-100 text-emerald-700"),
        MATCHED("매칭 완료", "bg-slate-900 text-white");

        private final String label;
        private final String cssClass;

        DashboardProposalStatus(String label, String cssClass) {
            this.label    = label;
            this.cssClass = cssClass;
        }

        public String getLabel()    { return label; }
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
