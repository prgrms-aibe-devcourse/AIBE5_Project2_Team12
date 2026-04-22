package com.generic4.itda.service;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkill;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.dto.matching.MatchingDetailContactViewModel;
import com.generic4.itda.dto.matching.MatchingDetailHeaderViewModel;
import com.generic4.itda.dto.matching.MatchingDetailProjectSummaryViewModel;
import com.generic4.itda.dto.matching.MatchingDetailSummaryViewModel;
import com.generic4.itda.dto.matching.MatchingDetailViewModel;
import com.generic4.itda.dto.matching.MatchingParticipantContactViewModel;
import com.generic4.itda.dto.matching.MatchingTimelineItemViewModel;
import com.generic4.itda.repository.MatchingRepository;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MatchingQueryService {

    private static final String VIEWER_ROLE_CLIENT = "CLIENT";
    private static final String VIEWER_ROLE_FREELANCER = "FREELANCER";

    private final MatchingRepository matchingRepository;

    public MatchingDetailViewModel getDetail(Long matchingId, String email) {
        Matching matching = matchingRepository.findDetailById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 정보를 찾을 수 없습니다. id=" + matchingId));

        String viewerRole = resolveViewerRole(matching, email);

        return new MatchingDetailViewModel(
                matching.getId(),
                viewerRole,
                matching.getStatus(),
                isContactVisible(matching.getStatus()),
                toHeader(matching, viewerRole),
                toSummary(matching, viewerRole),
                toContacts(matching),
                toProjectSummary(matching.getProposalPosition()),
                toTimeline(matching)
        );
    }

    private String resolveViewerRole(Matching matching, String email) {
        if (matching.getClientMember().getEmail().getValue().equals(email)) {
            return VIEWER_ROLE_CLIENT;
        }
        if (matching.getFreelancerMember().getEmail().getValue().equals(email)) {
            return VIEWER_ROLE_FREELANCER;
        }
        throw new AccessDeniedException("해당 매칭 정보에 접근할 수 없습니다.");
    }

    private boolean isContactVisible(MatchingStatus status) {
        return switch (status) {
            case ACCEPTED, IN_PROGRESS, COMPLETED, CANCELED -> true;
            case PROPOSED, REJECTED -> false;
        };
    }

    private MatchingDetailHeaderViewModel toHeader(Matching matching, String viewerRole) {
        ProposalPosition proposalPosition = matching.getProposalPosition();
        Member counterpart = VIEWER_ROLE_CLIENT.equals(viewerRole)
                ? matching.getFreelancerMember()
                : matching.getClientMember();

        return new MatchingDetailHeaderViewModel(
                proposalPosition.getProposal().getTitle(),
                proposalPosition.getTitle(),
                resolveDisplayName(counterpart),
                matching.getStatus().getDescription()
        );
    }

    private MatchingDetailSummaryViewModel toSummary(Matching matching, String viewerRole) {
        MatchingStatus status = matching.getStatus();
        String headline = switch (status) {
            case PROPOSED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "프리랜서 응답을 기다리는 중입니다."
                    : "매칭 요청이 도착했습니다.";
            case ACCEPTED -> "매칭이 수락되었습니다.";
            case REJECTED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "매칭 요청이 거절되었습니다."
                    : "매칭 요청을 거절했습니다.";
            case IN_PROGRESS -> "프로젝트가 진행 중입니다.";
            case COMPLETED -> "프로젝트가 완료되었습니다.";
            case CANCELED -> "매칭이 취소되었습니다.";
        };

        String helperMessage = switch (status) {
            case PROPOSED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다."
                    : "요청 내용을 확인한 뒤 수락 또는 거절할 수 있습니다.";
            case ACCEPTED -> "연락처가 공개되었습니다. 제안서를 다시 확인하고 협의를 이어가세요.";
            case REJECTED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "다른 추천 후보를 검토하거나 제안서 상태를 다시 확인해보세요."
                    : "이 매칭은 종료 상태이며 추가 응답은 필요하지 않습니다.";
            case IN_PROGRESS -> "계약 및 진행 현황을 이 화면에서 이어서 관리할 예정입니다.";
            case COMPLETED -> "완료된 매칭입니다.";
            case CANCELED -> "취소된 매칭입니다.";
        };

        String contactGuideMessage = isContactVisible(status)
                ? "매칭이 성사되어 연락처 정보가 공개된 상태입니다."
                : "매칭이 수락되기 전까지는 연락처가 공개되지 않습니다.";

        LocalDateTime respondedAt = switch (status) {
            case ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, CANCELED -> matching.getModifiedAt();
            case PROPOSED -> null;
        };

        return new MatchingDetailSummaryViewModel(
                headline,
                helperMessage,
                matching.getCreatedAt(),
                respondedAt,
                contactGuideMessage
        );
    }

    private MatchingDetailContactViewModel toContacts(Matching matching) {
        return new MatchingDetailContactViewModel(
                isContactVisible(matching.getStatus()),
                toParticipant("클라이언트", matching.getClientMember()),
                toParticipant("프리랜서", matching.getFreelancerMember())
        );
    }

    private MatchingParticipantContactViewModel toParticipant(String roleLabel, Member member) {
        return new MatchingParticipantContactViewModel(
                roleLabel,
                resolveDisplayName(member),
                member.getEmail().getValue(),
                formatPhone(member.getPhone().getValue())
        );
    }

    private MatchingDetailProjectSummaryViewModel toProjectSummary(ProposalPosition proposalPosition) {
        return new MatchingDetailProjectSummaryViewModel(
                proposalPosition.getProposal().getId(),
                proposalPosition.getProposal().getTitle(),
                proposalPosition.getProposal().getDescription(),
                proposalPosition.getTitle(),
                proposalPosition.getPosition().getName(),
                formatBudgetText(proposalPosition.getUnitBudgetMin(), proposalPosition.getUnitBudgetMax()),
                formatExpectedPeriod(proposalPosition.getExpectedPeriod()),
                proposalPosition.getWorkType() != null ? proposalPosition.getWorkType().getDescription() : "미정",
                proposalPosition.getSkills().stream()
                        .filter(skill -> skill.getImportance() == ProposalPositionSkillImportance.ESSENTIAL)
                        .map(skill -> skill.getSkill().getName())
                        .toList(),
                proposalPosition.getSkills().stream()
                        .filter(skill -> skill.getImportance() == ProposalPositionSkillImportance.PREFERENCE)
                        .map(skill -> skill.getSkill().getName())
                        .toList()
        );
    }

    private List<MatchingTimelineItemViewModel> toTimeline(Matching matching) {
        List<MatchingTimelineItemViewModel> timeline = new java.util.ArrayList<>();
        timeline.add(new MatchingTimelineItemViewModel(
                matching.getCreatedAt(),
                "클라이언트",
                "매칭 요청 전송",
                "프리랜서에게 매칭 요청을 보냈습니다."
        ));

        if (matching.getStatus() == MatchingStatus.ACCEPTED) {
            timeline.add(new MatchingTimelineItemViewModel(
                    matching.getModifiedAt(),
                    "프리랜서",
                    "매칭 요청 수락",
                    "매칭 요청이 수락되어 연락처가 공개되었습니다."
            ));
        }

        if (matching.getStatus() == MatchingStatus.REJECTED) {
            timeline.add(new MatchingTimelineItemViewModel(
                    matching.getModifiedAt(),
                    "프리랜서",
                    "매칭 요청 거절",
                    "매칭 요청이 거절되어 이 매칭은 종료되었습니다."
            ));
        }

        return List.copyOf(timeline);
    }

    private String resolveDisplayName(Member member) {
        if (StringUtils.hasText(member.getNickname())) {
            return member.getNickname().trim();
        }
        return member.getName();
    }

    private String formatBudgetText(Long budgetMin, Long budgetMax) {
        if (budgetMin == null && budgetMax == null) {
            return "미정";
        }
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.KOREA);
        if (budgetMin == null) {
            return "~ " + formatter.format(budgetMax) + "원";
        }
        if (budgetMax == null) {
            return formatter.format(budgetMin) + "원 ~";
        }
        if (budgetMin.equals(budgetMax)) {
            return formatter.format(budgetMin) + "원";
        }
        return formatter.format(budgetMin) + "원 ~ " + formatter.format(budgetMax) + "원";
    }

    private String formatExpectedPeriod(Long expectedPeriod) {
        return expectedPeriod != null ? expectedPeriod + "주" : "미정";
    }

    private String formatPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "-";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("02")) {
            if (digits.length() == 9) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
            }
            if (digits.length() == 10) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
            }
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        return digits;
    }
}
