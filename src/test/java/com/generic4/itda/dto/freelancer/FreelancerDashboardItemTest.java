package com.generic4.itda.dto.freelancer;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FreelancerDashboardItemTest {

    @Test
    @DisplayName("NEW 상태 아이템은 선택된 포지션 컨텍스트를 유지한 proposal 상세 경로를 반환한다")
    void detailPathReturnsProposalPathWithSelectedPositionForNewItem() {
        FreelancerDashboardItem item = new FreelancerDashboardItem(
                99L,
                3L,
                7L,
                "쇼핑몰 앱 개발",
                "클라이언트 회사",
                "API 백엔드 개발자",
                MatchingStatus.PROPOSED,
                3_000_000L,
                4_000_000L,
                LocalDateTime.now()
        );

        assertThat(item.detailPath()).isEqualTo("/proposals/3?proposalPositionId=7");
    }

    @Test
    @DisplayName("진행 중 상태 아이템은 matching 상세 경로를 반환한다")
    void detailPathReturnsMatchingPathForInProgressItem() {
        FreelancerDashboardItem item = new FreelancerDashboardItem(
                12L,
                3L,
                7L,
                "쇼핑몰 앱 개발",
                "클라이언트 회사",
                "API 백엔드 개발자",
                MatchingStatus.ACCEPTED,
                3_000_000L,
                4_000_000L,
                LocalDateTime.now()
        );

        assertThat(item.detailPath()).isEqualTo("/matchings/12");
    }

    @Test
    @DisplayName("matchingId가 없으면 proposal 상세 경로로 안전하게 fallback한다")
    void detailPathFallsBackToProposalPathWhenMatchingIdIsMissing() {
        FreelancerDashboardItem item = new FreelancerDashboardItem(
                null,
                5L,
                9L,
                "AI 서비스 구축",
                "클라이언트 회사",
                "모델/프롬프트 AI 엔지니어",
                MatchingStatus.COMPLETED,
                5_000_000L,
                6_000_000L,
                LocalDateTime.now()
        );

        assertThat(item.detailPath()).isEqualTo("/proposals/5");
    }
}
