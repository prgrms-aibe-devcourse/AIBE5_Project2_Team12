package com.generic4.itda.domain.matching.constant;

import lombok.Getter;

@Getter
public enum MatchingCancellationReason {
    CLIENT_BEFORE_REQUIREMENT_CHANGED(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "프로젝트 요구사항이 변경되었어요",
            false
    ),
    CLIENT_BEFORE_BUDGET_OR_SCHEDULE_CHANGED(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "예산 또는 일정이 맞지 않아요",
            false
    ),
    CLIENT_BEFORE_SELECTED_ANOTHER_FREELANCER(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "다른 프리랜서와 진행하기로 했어요",
            false
    ),
    CLIENT_BEFORE_OTHER(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "기타",
            true
    ),
    FREELANCER_BEFORE_SCHEDULE_CONFLICT(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "일정상 참여가 어려워졌어요",
            false
    ),
    FREELANCER_BEFORE_CONDITION_MISMATCH(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "업무 조건이 맞지 않아요",
            false
    ),
    FREELANCER_BEFORE_PROJECT_SCOPE_UNCLEAR(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "프로젝트 범위가 명확하지 않아요",
            false
    ),
    FREELANCER_BEFORE_OTHER(
            MatchingCancellationPhase.BEFORE_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "기타",
            true
    ),
    CLIENT_AFTER_PROJECT_SUSPENDED(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "프로젝트가 중단되었어요",
            false
    ),
    CLIENT_AFTER_COMMUNICATION_ISSUE(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "소통이 원활하지 않아요",
            false
    ),
    CLIENT_AFTER_DELIVERABLE_ISSUE(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "산출물 품질 또는 진행 상황에 문제가 있어요",
            false
    ),
    CLIENT_AFTER_OTHER(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.CLIENT,
            "기타",
            true
    ),
    FREELANCER_AFTER_SCOPE_CHANGED(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "업무 범위가 달라졌어요",
            false
    ),
    FREELANCER_AFTER_PAYMENT_OR_CONTRACT_ISSUE(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "계약 또는 정산 조건에 문제가 있어요",
            false
    ),
    FREELANCER_AFTER_SCHEDULE_OR_HEALTH_ISSUE(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "일정 또는 개인 사정으로 진행이 어려워졌어요",
            false
    ),
    FREELANCER_AFTER_OTHER(
            MatchingCancellationPhase.AFTER_CONTRACT,
            MatchingParticipantRole.FREELANCER,
            "기타",
            true
    );

    private final MatchingCancellationPhase phase;
    private final MatchingParticipantRole requesterRole;
    private final String label;
    private final boolean other;

    MatchingCancellationReason(
            MatchingCancellationPhase phase,
            MatchingParticipantRole requesterRole,
            String label,
            boolean other
    ) {
        this.phase = phase;
        this.requesterRole = requesterRole;
        this.label = label;
        this.other = other;
    }

    public boolean matches(MatchingCancellationPhase phase, MatchingParticipantRole requesterRole) {
        return this.phase == phase && this.requesterRole == requesterRole;
    }
}
