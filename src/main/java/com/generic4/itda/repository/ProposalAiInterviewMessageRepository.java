package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.ProposalAiInterviewMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalAiInterviewMessageRepository extends JpaRepository<ProposalAiInterviewMessage, Long> {

    List<ProposalAiInterviewMessage> findAllByProposalIdOrderBySequenceAsc(Long proposalId);

    Optional<ProposalAiInterviewMessage> findTopByProposalIdOrderBySequenceDesc(Long proposalId);

    void deleteAllByProposalId(Long proposalId);
}