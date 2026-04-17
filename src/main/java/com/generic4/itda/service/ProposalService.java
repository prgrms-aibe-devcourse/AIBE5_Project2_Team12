package com.generic4.itda.service;

import com.generic4.itda.domain.member.Member;
import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.dto.proposal.ProposalForm;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.MemberRepository;
import com.generic4.itda.repository.ProposalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final MemberRepository memberRepository;

    public Proposal create(String memberEmail, ProposalForm form) {
        Member member = getMemberByEmail(memberEmail);

        Proposal proposal = Proposal.create(
                member,
                form.getTitle(),
                form.getRawInputText(),
                form.getDescription(),
                form.getTotalBudgetMin(),
                form.getTotalBudgetMax(),
                form.getWorkType(),
                form.getWorkPlace(),
                form.getExpectedPeriod()
        );

        return proposalRepository.save(proposal);
    }

    public Proposal update(Long proposalId, String memberEmail, ProposalForm form) {
        Proposal proposal = getOwnedProposal(proposalId, memberEmail);

        proposal.update(
                form.getTitle(),
                form.getRawInputText(),
                form.getDescription(),
                form.getTotalBudgetMin(),
                form.getTotalBudgetMax(),
                form.getWorkType(),
                form.getWorkPlace(),
                form.getExpectedPeriod()
        );

        return proposal;
    }

    @Transactional(readOnly = true)
    public Proposal findOwnedProposal(Long proposalId, String memberEmail) {
        return getOwnedProposal(proposalId, memberEmail);
    }

    private Member getMemberByEmail(String memberEmail) {
        Member member = memberRepository.findByEmail_Value(memberEmail);
        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다.");
        }
        return member;
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
