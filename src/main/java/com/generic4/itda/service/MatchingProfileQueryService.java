package com.generic4.itda.service;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.resume.CareerItemPayload;
import com.generic4.itda.domain.resume.CareerPayload;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.dto.profile.ProfileAccessLevel;
import com.generic4.itda.dto.profile.ProfileCareerItemViewModel;
import com.generic4.itda.dto.profile.ProfileClientBodyViewModel;
import com.generic4.itda.dto.profile.ProfileContextType;
import com.generic4.itda.dto.profile.ProfileFreelancerBodyViewModel;
import com.generic4.itda.dto.profile.ProfileMatchingContextViewModel;
import com.generic4.itda.dto.profile.ProfileProjectSummaryViewModel;
import com.generic4.itda.dto.profile.ProfileShellViewModel;
import com.generic4.itda.dto.profile.ProfileSkillItemViewModel;
import com.generic4.itda.dto.profile.ProfileSubjectType;
import com.generic4.itda.repository.MatchingRepository;
import java.text.NumberFormat;
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
public class MatchingProfileQueryService {

    private static final String VIEWER_ROLE_CLIENT = "CLIENT";
    private static final String VIEWER_ROLE_FREELANCER = "FREELANCER";

    private final MatchingRepository matchingRepository;

    public ProfileShellViewModel getCounterpartProfile(Long matchingId, String email) {
        Matching matching = matchingRepository.findDetailById(matchingId)
                .orElseThrow(() -> new IllegalArgumentException("매칭 정보를 찾을 수 없습니다. id=" + matchingId));

        String viewerRole = resolveViewerRole(matching, email);
        boolean contactVisible = isContactVisible(matching);

        if (VIEWER_ROLE_CLIENT.equals(viewerRole)) {
            return toFreelancerProfile(matching, viewerRole, contactVisible);
        }

        return toClientProfile(matching, viewerRole, contactVisible);
    }

    private ProfileShellViewModel toFreelancerProfile(Matching matching, String viewerRole, boolean contactVisible) {
        Resume resume = matching.getResume();
        Member freelancer = matching.getFreelancerMember();
        ProposalPosition proposalPosition = matching.getProposalPosition();
        ProfileProjectSummaryViewModel projectSummary = toProjectSummary(proposalPosition);

        String displayName = resolveProfileDisplayName(freelancer, contactVisible);

        return new ProfileShellViewModel(
                ProfileSubjectType.FREELANCER,
                ProfileContextType.MATCHING,
                toAccessLevel(contactVisible),
                displayName,
                toSubtitle(proposalPosition),
                matching.getStatus().getDescription(),
                "/matchings/" + matching.getId(),
                toFreelancerBody(resume, displayName),
                null,
                projectSummary,
                toMatchingContext(matching, viewerRole, contactVisible, freelancer),
                null
        );
    }

    private ProfileShellViewModel toClientProfile(Matching matching, String viewerRole, boolean contactVisible) {
        Member client = matching.getClientMember();
        ProposalPosition proposalPosition = matching.getProposalPosition();
        ProfileProjectSummaryViewModel projectSummary = toProjectSummary(proposalPosition);

        String displayName = resolveProfileDisplayName(client, contactVisible);

        return new ProfileShellViewModel(
                ProfileSubjectType.CLIENT,
                ProfileContextType.MATCHING,
                toAccessLevel(contactVisible),
                displayName,
                toSubtitle(proposalPosition),
                matching.getStatus().getDescription(),
                "/matchings/" + matching.getId(),
                null,
                toClientBody(client, displayName, projectSummary),
                projectSummary,
                toMatchingContext(matching, viewerRole, contactVisible, client),
                null
        );
    }

    private ProfileFreelancerBodyViewModel toFreelancerBody(Resume resume, String displayName) {
        return new ProfileFreelancerBodyViewModel(
                displayName,
                "프리랜서 프로필",
                resume.getIntroduction(),
                resume.getCareerYears() != null ? resume.getCareerYears().intValue() : null,
                resume.getPreferredWorkType() != null ? resume.getPreferredWorkType().getDescription() : "미정",
                resume.getPortfolioUrl(),
                resume.getSkills().stream()
                        .map(skill -> new ProfileSkillItemViewModel(
                                skill.getSkill().getName(),
                                skill.getProficiency().getDescription(),
                                skill.getProficiency().name()
                        ))
                        .toList(),
                toCareerItems(resume)
        );
    }

    private ProfileClientBodyViewModel toClientBody(
            Member client,
            String displayName,
            ProfileProjectSummaryViewModel projectSummary
    ) {
        return new ProfileClientBodyViewModel(
                displayName,
                client.getType() != null ? client.getType().getDescription() : null,
                client.getMemo(),
                projectSummary
        );
    }

    private ProfileMatchingContextViewModel toMatchingContext(
            Matching matching,
            String viewerRole,
            boolean contactVisible,
            Member counterpart
    ) {
        return new ProfileMatchingContextViewModel(
                matching.getId(),
                viewerRole,
                matching.getStatus().name(),
                contactVisible,
                matching.getStatus().getDescription(),
                resolveHelperMessage(matching, viewerRole),
                "/matchings/" + matching.getId(),
                "/proposals/" + matching.getProposalPosition().getProposal().getId(),
                "/matchings/" + matching.getId() + "/accept",
                "/matchings/" + matching.getId() + "/reject",
                resolveContactEmail(counterpart, contactVisible),
                resolveContactPhone(counterpart, contactVisible)
        );
    }

    private List<ProfileCareerItemViewModel> toCareerItems(Resume resume) {
        CareerPayload career = resume.getCareer();
        if (career == null || career.getItems() == null) {
            return List.of();
        }

        return career.getItems().stream()
                .map(this::toCareerItem)
                .toList();
    }

    private ProfileCareerItemViewModel toCareerItem(CareerItemPayload career) {
        return new ProfileCareerItemViewModel(
                career.getCompanyName(),
                career.getPosition(),
                career.getEmploymentType() != null ? career.getEmploymentType().getDescription() : "미정",
                formatCareerPeriod(career),
                career.getSummary(),
                career.getTechStack()
        );
    }

    private ProfileProjectSummaryViewModel toProjectSummary(ProposalPosition proposalPosition) {
        Proposal proposal = proposalPosition.getProposal();
        return new ProfileProjectSummaryViewModel(
                proposal.getId(),
                proposal.getTitle(),
                proposal.getDescription(),
                proposalPosition.getTitle(),
                proposalPosition.getWorkType() != null ? proposalPosition.getWorkType().getDescription() : "미정",
                formatBudgetText(proposalPosition.getUnitBudgetMin(), proposalPosition.getUnitBudgetMax()),
                formatExpectedPeriod(proposalPosition.getExpectedPeriod()),
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

    private ProfileAccessLevel toAccessLevel(boolean contactVisible) {
        return contactVisible ? ProfileAccessLevel.FULL : ProfileAccessLevel.PREVIEW;
    }

    private String toSubtitle(ProposalPosition proposalPosition) {
        return proposalPosition.getProposal().getTitle()
                + " · "
                + proposalPosition.getTitle();
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

    private String resolveProfileDisplayName(Member member, boolean contactVisible) {
        if (contactVisible) {
            return resolveDisplayName(member);
        }
        return maskName(resolveDisplayName(member));
    }

    private String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return "익명";
        }

        name = name.trim();

        if (name.length() == 1) {
            return "*";
        }

        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }

        return name.charAt(0)
                + "*".repeat(name.length() - 2)
                + name.charAt(name.length() - 1);
    }


    private String resolveDisplayName(Member member) {
        if (StringUtils.hasText(member.getNickname())) {
            return member.getNickname().trim();
        }
        return member.getName().trim();
    }

    private String resolveContactEmail(Member member, boolean contactVisible) {
        if (!contactVisible) {
            return null;
        }
        return member.getEmail().getValue();
    }

    private String resolveContactPhone(Member member, boolean contactVisible) {
        if (!contactVisible) {
            return null;
        }
        return formatPhone(member.getPhone().getValue());
    }

    private String resolveHelperMessage(Matching matching, String viewerRole) {
        MatchingStatus status = matching.getStatus();

        return switch (status) {
            case PROPOSED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "프리랜서가 요청을 확인하고 응답하면 다음 단계로 넘어갈 수 있습니다."
                    : "요청 내용을 확인한 뒤 수락 또는 거절할 수 있습니다.";
            case ACCEPTED -> "연락처가 공개되었습니다. 제안서를 다시 확인하고 협의를 이어가세요.";
            case REJECTED -> VIEWER_ROLE_CLIENT.equals(viewerRole)
                    ? "다른 추천 후보를 검토하거나 제안서 상태를 다시 확인해보세요."
                    : "이 매칭은 종료 상태이며 추가 응답은 필요하지 않습니다.";
            case IN_PROGRESS -> "프로젝트가 진행 중입니다. 연락처와 제안서 정보를 확인하며 협업을 이어가세요.";
            case COMPLETED -> "완료된 매칭입니다. 프로젝트 진행 이력을 확인할 수 있습니다.";
            case CANCELED -> isContractCancellation(matching)
                    ? "취소된 계약입니다. 필요한 경우 진행 이력과 연락처 정보를 확인해보세요."
                    : "취소된 매칭입니다. 필요한 경우 다른 추천 후보를 검토해보세요.";
        };
    }

    private boolean isContactVisible(Matching matching) {
        return switch (matching.getStatus()) {
            case ACCEPTED, IN_PROGRESS, COMPLETED -> true;
            case REJECTED, PROPOSED -> false;
            case CANCELED -> isContractCancellation(matching);
        };
    }

    private String formatCareerPeriod(CareerItemPayload career) {
        String start = career.getStartYearMonth();
        String end = Boolean.TRUE.equals(career.getCurrentlyWorking())
                ? "재직 중"
                : career.getEndYearMonth();

        if (!StringUtils.hasText(start) && !StringUtils.hasText(end)) {
            return "기간 미정";
        }

        if (!StringUtils.hasText(end)) {
            return start + " ~";
        }

        return start + " ~ " + end;
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

    private boolean isContractCancellation(Matching matching) {
        return matching.getStatus() == MatchingStatus.CANCELED && matching.getContractDate() != null;
    }
}
