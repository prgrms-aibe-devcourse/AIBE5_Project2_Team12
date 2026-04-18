package com.generic4.itda.service;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.position.Position;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalPositionSkillImportance;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.domain.skill.Skill;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.dto.proposal.ProposalPositionForm;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.PositionRepository;
import com.generic4.itda.repository.ProposalRepository;
import com.generic4.itda.repository.SkillRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final MemberRepository memberRepository;
    private final PositionRepository positionRepository;
    private final SkillRepository skillRepository;

    public Proposal saveDraft(String memberEmail, ProposalForm form) {
        Member member = getMemberByEmail(memberEmail);
        List<ProposalPositionForm> positionForms = getPositionForms(form);

        Proposal proposal = Proposal.create(
                member,
                form.getTitle(),
                form.getRawInputText(),
                form.getDescription(),
                calculateTotalBudgetMin(positionForms),
                calculateTotalBudgetMax(positionForms),
                form.getExpectedPeriod()
        );

        replacePositions(proposal, positionForms);
        return proposalRepository.save(proposal);
    }

    public Proposal register(String memberEmail, ProposalForm form) {
        Proposal proposal = saveDraft(memberEmail, form);
        proposal.startMatching();
        return proposal;
    }

    public Proposal saveDraft(Long proposalId, String memberEmail, ProposalForm form) {
        Proposal proposal = getOwnedProposal(proposalId, memberEmail);
        validateEditable(proposal);
        proposal.revertToWriting();
        List<ProposalPositionForm> positionForms = getPositionForms(form);

        proposal.update(
                form.getTitle(),
                form.getRawInputText(),
                form.getDescription(),
                calculateTotalBudgetMin(positionForms),
                calculateTotalBudgetMax(positionForms),
                form.getExpectedPeriod()
        );

        replacePositions(proposal, positionForms);
        return proposal;
    }

    public Proposal register(Long proposalId, String memberEmail, ProposalForm form) {
        Proposal proposal = getOwnedProposal(proposalId, memberEmail);
        validateEditable(proposal);
        List<ProposalPositionForm> positionForms = getPositionForms(form);

        proposal.update(
                form.getTitle(),
                form.getRawInputText(),
                form.getDescription(),
                calculateTotalBudgetMin(positionForms),
                calculateTotalBudgetMax(positionForms),
                form.getExpectedPeriod()
        );

        replacePositions(proposal, positionForms);
        if (proposal.getStatus() == ProposalStatus.WRITING) {
            proposal.startMatching();
        }
        return proposal;
    }

    @Transactional(readOnly = true)
    public Proposal findOwnedProposal(Long proposalId, String memberEmail) {
        return getOwnedProposal(proposalId, memberEmail);
    }

    public Long calculateTotalBudgetMin(ProposalForm form) {
        return calculateTotalBudgetMin(getPositionForms(form));
    }

    public Long calculateTotalBudgetMax(ProposalForm form) {
        return calculateTotalBudgetMax(getPositionForms(form));
    }

    private void replacePositions(Proposal proposal, List<ProposalPositionForm> positionForms) {
        List<ProposalPosition> existingPositions = new ArrayList<>(proposal.getPositions());
        for (ProposalPosition existingPosition : existingPositions) {
            proposal.removePosition(existingPosition);
        }

        for (ProposalPositionForm positionForm : positionForms) {
            Position position = getPosition(positionForm.getPositionId());
            ProposalPosition proposalPosition = proposal.addPosition(
                    position,
                    positionForm.getTitle(),
                    positionForm.getWorkType(),
                    positionForm.getHeadCount(),
                    positionForm.getUnitBudgetMin(),
                    positionForm.getUnitBudgetMax(),
                    positionForm.getExpectedPeriod(),
                    positionForm.getCareerMinYears(),
                    positionForm.getCareerMaxYears(),
                    positionForm.getWorkPlace()
            );

            if (positionForm.getStatus() != null) {
                proposalPosition.changeStatus(positionForm.getStatus());
            }

            addSkills(proposalPosition, positionForm.getEssentialSkillNames(), ProposalPositionSkillImportance.ESSENTIAL);
            addSkills(proposalPosition, positionForm.getPreferredSkillNames(), ProposalPositionSkillImportance.PREFERENCE);
        }
    }

    private void addSkills(ProposalPosition proposalPosition, List<String> skillNames,
            ProposalPositionSkillImportance importance) {
        for (String skillName : normalizeSkillNames(skillNames)) {
            Skill skill = skillRepository.findByName(skillName)
                    .orElseGet(() -> skillRepository.save(Skill.create(skillName, null)));
            proposalPosition.addSkill(skill, importance);
        }
    }

    private List<String> normalizeSkillNames(List<String> skillNames) {
        if (skillNames == null || skillNames.isEmpty()) {
            return List.of();
        }

        return skillNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private Long calculateTotalBudgetMin(List<ProposalPositionForm> positions) {
        return calculateBudget(positions, true);
    }

    private Long calculateTotalBudgetMax(List<ProposalPositionForm> positions) {
        return calculateBudget(positions, false);
    }

    private Long calculateBudget(List<ProposalPositionForm> positions, boolean minBudget) {
        if (positions.isEmpty()) {
            return null;
        }

        long sum = 0L;
        for (ProposalPositionForm position : positions) {
            Long headCount = position.getHeadCount();
            Long budget = minBudget ? position.getUnitBudgetMin() : position.getUnitBudgetMax();
            if (headCount == null || budget == null) {
                return null;
            }
            sum = Math.addExact(sum, Math.multiplyExact(headCount, budget));
        }
        return sum;
    }

    private List<ProposalPositionForm> getPositionForms(ProposalForm form) {
        return form.getPositions() == null ? List.of() : form.getPositions();
    }

    private Member getMemberByEmail(String memberEmail) {
        Member member = memberRepository.findByEmail_Value(memberEmail);
        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        return member;
    }

    private void validateEditable(Proposal proposal) {
        if (proposal.getStatus() == ProposalStatus.COMPLETE) {
            throw new IllegalStateException("종료된 제안서는 수정할 수 없습니다.");
        }
    }

    private Position getPosition(Long positionId) {
        return positionRepository.findById(positionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 직무입니다. id=" + positionId));
    }

    private Proposal getOwnedProposal(Long proposalId, String memberEmail) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=" + proposalId));

        if (!proposal.getMember().getEmail().getValue().equals(memberEmail)) {
            throw new AccessDeniedException("본인 제안서만 조회하거나 수정할 수 있습니다.");
        }
        return proposal;
    }
}
