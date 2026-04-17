package com.generic4.itda.dto.client;

import com.generic4.itda.domain.proposal.ProposalStatus;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum ClientDashboardFilter {
    ALL("all", "전체 프로젝트", "현재 관리 중인 모든 프로젝트 제안서 및 매칭 현황"),
    WAITING("waiting", "매칭 대기 중", "아직 매칭을 시작하지 않은 프로젝트"),
    MATCHING("matching", "진행 중 매칭", "프리랜서에게 요청을 보내고 응답을 기다리거나 매칭이 진행 중인 프로젝트"),
    COMPLETE("complete", "종료된 참여", "계약이 종료되었거나 성공적으로 완료된 프로젝트");

    private final String key;
    private final String title;
    private final String description;

    ClientDashboardFilter(String key, String title, String description) {
        this.key = key;
        this.title = title;
        this.description = description;
    }

    public ProposalStatus toProposalStatus() {
        return switch (this) {
            case WAITING -> ProposalStatus.WRITING;
            case MATCHING -> ProposalStatus.MATCHING;
            case COMPLETE -> ProposalStatus.COMPLETE;
            case ALL -> null;
        };
    }

    public boolean isAll() {
        return this == ALL;
    }

    public static ClientDashboardFilter from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }

        return Arrays.stream(values())
                .filter(filter -> filter.key.equalsIgnoreCase(value))
                .findFirst()
                .orElse(ALL);
    }
}
