package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRepository extends JpaRepository<Matching, Long>, MatchingRepositoryCustom {

    boolean existsByProposalPosition_Proposal_Id(Long proposalId);

    boolean existsByProposalPosition_Proposal_IdAndStatusIn(Long proposalId, Collection<MatchingStatus> statuses);

    boolean existsByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(Long proposalId, String freelancerEmail);

    List<Matching> findByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(Long proposalId, String freelancerEmail);
}
