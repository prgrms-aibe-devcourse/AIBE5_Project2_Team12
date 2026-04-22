package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.QMatching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.QMember;
import com.generic4.itda.domain.proposal.QProposal;
import com.generic4.itda.domain.proposal.QProposalPosition;
import com.generic4.itda.dto.freelancer.FreelancerDashboardItem;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class MatchingRepositoryImpl implements MatchingRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QMatching matching = QMatching.matching;
    private final QProposalPosition proposalPosition = QProposalPosition.proposalPosition;
    private final QProposal proposal = QProposal.proposal;
    private final QMember clientMember = new QMember("clientMember");
    private final QMember freelancerMember = new QMember("freelancerMember");

    @Override
    public List<FreelancerDashboardItem> getDashboardItems(String email, String status, String q) {
        return queryFactory
                .select(Projections.constructor(FreelancerDashboardItem.class,
                        proposal.id,
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
        if (!StringUtils.hasText(status)) return null;
        try {
            // 전달받은 문자열을 Enum으로 변환하여 비교
            MatchingStatus matchingStatus = MatchingStatus.valueOf(status.toUpperCase());
            return matching.status.eq(matchingStatus);
        } catch (IllegalArgumentException e) {
            // 잘못된 상태값이 들어오면 조건을 무시 (전체 조회와 동일 효과)
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
}
