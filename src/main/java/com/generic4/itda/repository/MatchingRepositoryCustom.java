package com.generic4.itda.repository;

import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.dto.matching.LatestMatchingSummary;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MatchingRepositoryCustom {
    List<FreelancerDashboardItem> getDashboardItems(String email, String status, String q);

    /**
     * 같은 (proposalPositionId, resumeId) 조합에 매칭 row가 여러 개 있을 수 있으므로,
     * ACTIVE 상태를 우선하고(없으면 최신), 후보별 대표 매칭 상태를 resumeId 기준으로 집계한다.
     */
    Map<Long, MatchingStatus> getLatestMatchingStatusMap(Long proposalPositionId, Collection<Long> resumeIds);

    /**
     * 단일 후보의 대표 매칭 상태를 조회한다(ACTIVE 우선, 없으면 최신).
     */
    Optional<MatchingStatus> getLatestMatchingStatus(Long proposalPositionId, Long resumeId);

    Map<Long, LatestMatchingSummary> getLatestMatchingSummaryMap(Long proposalPositionId, Collection<Long> resumeIds);

    Optional<LatestMatchingSummary> getLatestMatchingSummary(Long proposalPositionId, Long resumeId);
}
