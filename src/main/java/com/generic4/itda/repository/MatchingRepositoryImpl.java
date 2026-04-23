package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.QMatching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.QMember;
import com.generic4.itda.domain.proposal.QProposal;
import com.generic4.itda.domain.proposal.QProposalPosition;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.generic4.itda.dto.matching.LatestMatchingSummary;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class MatchingRepositoryImpl implements MatchingRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QMatching matching = QMatching.matching;
    private final QProposalPosition proposalPosition = QProposalPosition.proposalPosition;
    private final QProposal proposal = QProposal.proposal;
    private final QMember clientMember = new QMember("clientMember");
    private final QMember freelancerMember = new QMember("freelancerMember");

    private static final EnumSet<MatchingStatus> ACTIVE_STATUSES = EnumSet.of(
            MatchingStatus.PROPOSED,
            MatchingStatus.ACCEPTED,
            MatchingStatus.IN_PROGRESS
    );
    private static final EnumSet<MatchingStatus> NEW_ROW_STATUSES = EnumSet.of(
            MatchingStatus.PROPOSED
    );
    private static final EnumSet<MatchingStatus> IN_PROGRESS_ROW_STATUSES = EnumSet.of(
            MatchingStatus.ACCEPTED,
            MatchingStatus.IN_PROGRESS
    );
    private static final EnumSet<MatchingStatus> COMPLETED_ROW_STATUSES = EnumSet.of(
            MatchingStatus.COMPLETED,
            MatchingStatus.REJECTED,
            MatchingStatus.CANCELED
    );

    @Override
    public List<FreelancerDashboardItem> getDashboardItems(String email, String status, String q) {
        return queryFactory
                .select(Projections.constructor(FreelancerDashboardItem.class,
                        matching.id,
                        proposal.id,
                        proposalPosition.id,
                        proposal.title,
                        clientMember.nickname,
                        proposalPosition.title,
                        matching.status, // 여기서 MatchingStatus를 가져옴
                        proposalPosition.unitBudgetMin,
                        proposalPosition.unitBudgetMax,
                        matching.createdAt
                ))
                .from(matching)
                .join(matching.proposalPosition, proposalPosition)
                .join(proposalPosition.proposal, proposal)
                .join(matching.clientMember, clientMember)
                .join(matching.freelancerMember, freelancerMember)
                .where(
                        freelancerMember.email.value.eq(email),
                        statusEq(status),
                        searchKw(q)
                )
                .orderBy(matching.createdAt.desc())
                .fetch();
    }

    // 1. 상태 필터링 (MatchingStatus 기준)
    private BooleanExpression statusEq(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        String normalizedStatus = status.trim().toUpperCase();

        return switch (normalizedStatus) {
            case "NEW" -> matching.status.in(NEW_ROW_STATUSES);
            case "IN_PROGRESS" -> matching.status.in(IN_PROGRESS_ROW_STATUSES);
            case "COMPLETED" -> matching.status.in(COMPLETED_ROW_STATUSES);
            default -> matchingStatusEq(normalizedStatus);
        };
    }

    private BooleanExpression matchingStatusEq(String normalizedStatus) {
        try {
            MatchingStatus matchingStatus = MatchingStatus.valueOf(normalizedStatus);
            return matching.status.eq(matchingStatus);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 2. 키워드 검색 (제안 제목 또는 기업명 기준)
    private BooleanExpression searchKw(String q) {
        if (!StringUtils.hasText(q)) return null;
        String cleanQ = q.trim();

        // 제안서 제목에 포함되어 있거나, 기업(clientMember)의 닉네임에 포함된 경우
        return proposal.title.containsIgnoreCase(cleanQ)
                .or(clientMember.nickname.containsIgnoreCase(cleanQ));
    }

    @Override
    public Map<Long, MatchingStatus> getLatestMatchingStatusMap(Long proposalPositionId, Collection<Long> resumeIds) {
        if (proposalPositionId == null || resumeIds == null || resumeIds.isEmpty()) {
            return Map.of();
        }

        NumberExpression<Integer> activeFirst = new CaseBuilder()
                .when(matching.status.in(ACTIVE_STATUSES)).then(0)
                .otherwise(1);

        List<com.querydsl.core.Tuple> rows = queryFactory
                .select(matching.resume.id, matching.status)
                .from(matching)
                .where(
                        matching.proposalPosition.id.eq(proposalPositionId),
                        matching.resume.id.in(resumeIds)
                )
                // resumeId별로 묶고, ACTIVE 우선 + 최신 우선(createdAt/id desc)으로 정렬하여 "대표 1건"이 먼저 오게 한다.
                .orderBy(
                        matching.resume.id.asc(),
                        activeFirst.asc(),
                        matching.createdAt.desc(),
                        matching.id.desc()
                )
                .fetch();

        Map<Long, MatchingStatus> map = new LinkedHashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long resumeId = row.get(matching.resume.id);
            MatchingStatus status = row.get(matching.status);
            if (resumeId != null && status != null) {
                map.putIfAbsent(resumeId, status);
            }
        }
        return map;
    }

    @Override
    public Optional<MatchingStatus> getLatestMatchingStatus(Long proposalPositionId, Long resumeId) {
        if (proposalPositionId == null || resumeId == null) {
            return Optional.empty();
        }

        NumberExpression<Integer> activeFirst = new CaseBuilder()
                .when(matching.status.in(ACTIVE_STATUSES)).then(0)
                .otherwise(1);

        MatchingStatus status = queryFactory
                .select(matching.status)
                .from(matching)
                .where(
                        matching.proposalPosition.id.eq(proposalPositionId),
                        matching.resume.id.eq(resumeId)
                )
                .orderBy(
                        activeFirst.asc(),
                        matching.createdAt.desc(),
                        matching.id.desc()
                )
                .fetchFirst();

        return Optional.ofNullable(status);
    }

    @Override
    public Map<Long, LatestMatchingSummary> getLatestMatchingSummaryMap(Long proposalPositionId, Collection<Long> resumeIds) {
        if (proposalPositionId == null || resumeIds == null || resumeIds.isEmpty()) {
            return Map.of();
        }

        NumberExpression<Integer> activeFirst = new CaseBuilder()
                .when(matching.status.in(ACTIVE_STATUSES)).then(0)
                .otherwise(1);

        List<com.querydsl.core.Tuple> rows = queryFactory
                .select(matching.resume.id, matching.id, matching.status)
                .from(matching)
                .where(
                        matching.proposalPosition.id.eq(proposalPositionId),
                        matching.resume.id.in(resumeIds)
                )
                .orderBy(
                        matching.resume.id.asc(),
                        activeFirst.asc(),
                        matching.createdAt.desc(),
                        matching.id.desc()
                )
                .fetch();

        Map<Long, LatestMatchingSummary> map = new LinkedHashMap<>();
        for (com.querydsl.core.Tuple row : rows) {
            Long resumeId = row.get(matching.resume.id);
            Long matchingId = row.get(matching.id);
            MatchingStatus status = row.get(matching.status);
            if (resumeId != null && matchingId != null && status != null) {
                map.putIfAbsent(resumeId, new LatestMatchingSummary(matchingId, status));
            }
        }
        return map;
    }

    @Override
    public Optional<LatestMatchingSummary> getLatestMatchingSummary(Long proposalPositionId, Long resumeId) {
        if (proposalPositionId == null || resumeId == null) {
            return Optional.empty();
        }

        NumberExpression<Integer> activeFirst = new CaseBuilder()
                .when(matching.status.in(ACTIVE_STATUSES)).then(0)
                .otherwise(1);

        com.querydsl.core.Tuple row = queryFactory
                .select(matching.id, matching.status)
                .from(matching)
                .where(
                        matching.proposalPosition.id.eq(proposalPositionId),
                        matching.resume.id.eq(resumeId)
                )
                .orderBy(
                        activeFirst.asc(),
                        matching.createdAt.desc(),
                        matching.id.desc()
                )
                .fetchFirst();

        if (row == null) {
            return Optional.empty();
        }

        Long matchingId = row.get(matching.id);
        MatchingStatus status = row.get(matching.status);
        if (matchingId == null || status == null) {
            return Optional.empty();
        }

        return Optional.of(new LatestMatchingSummary(matchingId, status));
    }
}
