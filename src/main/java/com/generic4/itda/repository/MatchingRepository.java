package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchingRepository extends JpaRepository<Matching, Long>, MatchingRepositoryCustom {

    boolean existsByProposalPosition_Proposal_Id(Long proposalId);

    boolean existsByProposalPosition_Proposal_IdAndStatusIn(Long proposalId, Collection<MatchingStatus> statuses);

    boolean existsByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(Long proposalId, String freelancerEmail);

    boolean existsByProposalPosition_IdAndResume_IdAndStatusIn(
            Long proposalPositionId,
            Long resumeId,
            Collection<MatchingStatus> statuses
    );

    long countByProposalPosition_IdAndStatusIn(Long proposalPositionId, Collection<MatchingStatus> statuses);

    List<Matching> findByProposalPosition_Proposal_IdAndFreelancerMember_Email_Value(Long proposalId, String freelancerEmail);

    @Query("""
            select m
            from Matching m
            join fetch m.proposalPosition pp
            join fetch pp.proposal p
            join fetch p.member proposalOwner
            join fetch m.resume resume
            join fetch resume.member resumeOwner
            join fetch m.clientMember clientMember
            join fetch m.freelancerMember freelancerMember
            where m.id = :matchingId
            """)
    Optional<Matching> findDetailById(Long matchingId);
}
