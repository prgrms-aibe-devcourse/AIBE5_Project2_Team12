package com.generic4.itda.service;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import com.generic4.itda.domain.proposal.ProposalStatus;
import com.generic4.itda.dto.proposal.AiBriefGenerateRequest;
import com.generic4.itda.dto.proposal.AiBriefResult;
import com.generic4.itda.exception.AiBriefGenerationException;
import com.generic4.itda.exception.ProposalNotFoundException;
import com.generic4.itda.repository.ProposalAiInterviewMessageRepository;
import com.generic4.itda.repository.ProposalRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
public class ProposalAiBriefService {

    private final ProposalRepository proposalRepository;
    private final ProposalAiInterviewMessageRepository messageRepository;
    private final AiBriefGenerator aiBriefGenerator;
    private final AiBriefProposalMapper aiBriefProposalMapper;

    public AiBriefResult generate(Long proposalId, String memberEmail) {
        Assert.notNull(proposalId, "제안서 id는 필수값입니다.");
        Assert.hasText(memberEmail, "회원 이메일은 필수값입니다.");

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException("제안서를 찾을 수 없습니다. id=" + proposalId));

        validateOwnership(proposal, memberEmail);
        Assert.state(proposal.getStatus() == ProposalStatus.WRITING,
                "작성 중인 제안서만 AI 브리프를 생성할 수 있습니다.");

        String aiBriefInput = resolveAiBriefInput(proposalId, proposal);
        syncRawInputText(proposal, aiBriefInput);

        AiBriefResult aiBriefResult = generateAiBrief(aiBriefInput);
        aiBriefProposalMapper.apply(proposal, aiBriefResult);
        return aiBriefResult;
    }

    private String resolveAiBriefInput(Long proposalId, Proposal proposal) {
        String conversationText = findConversationText(proposalId);
        if (StringUtils.hasText(conversationText)) {
            return conversationText;
        }
        return proposal.getRawInputText();
    }

    private String findConversationText(Long proposalId) {
        List<ProposalAiInterviewMessage> messages = messageRepository.findAllByProposalIdOrderBySequenceAsc(proposalId);
        if (messages.isEmpty()) {
            return "";
        }

        return messages.stream()
                .map(ProposalAiInterviewMessage::toRawInputLine)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private void syncRawInputText(Proposal proposal, String rawInputText) {
        if (!StringUtils.hasText(rawInputText)) {
            return;
        }

        proposal.update(
                proposal.getTitle(),
                rawInputText,
                proposal.getDescription(),
                proposal.getTotalBudgetMin(),
                proposal.getTotalBudgetMax(),
                proposal.getExpectedPeriod()
        );
    }

    private AiBriefResult generateAiBrief(String rawInputText) {
        try {
            return aiBriefGenerator.generate(AiBriefGenerateRequest.from(rawInputText));
        } catch (AiBriefGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiBriefGenerationException("AI 브리프 생성에 실패했습니다.", exception);
        }
    }

    private void validateOwnership(Proposal proposal, String memberEmail) {
        if (!proposal.getMember().getEmail().getValue().equals(memberEmail)) {
            throw new AccessDeniedException("본인 제안서에 대해서만 AI 브리프를 생성할 수 있습니다.");
        }
    }
}