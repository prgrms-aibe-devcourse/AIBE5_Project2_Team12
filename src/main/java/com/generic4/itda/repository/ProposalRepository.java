package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.Proposal;
import com.generic4.itda.domain.proposal.ProposalPosition;
import com.generic4.itda.domain.proposal.ProposalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

    Optional<Proposal> findByMemberIdAndTitle(Long memberId, String title);

    List<Proposal> findAllByMember_Email_ValueOrderByModifiedAtDesc(String email);

    List<Proposal> findAllByMember_Email_ValueAndStatusOrderByModifiedAtDesc(String email, ProposalStatus status);

    long countByMember_Email_Value(String email);

    long countByMember_Email_ValueAndStatus(String email, ProposalStatus status);

    @Query("""
            select distinct p
            from Proposal p
            left join fetch p.positions pp
            left join fetch pp.position pos
            where p.id = :proposalId
            """)
    Optional<Proposal> findWithPositionsById(Long proposalId);

    @Query("""
            select distinct pp
            from ProposalPosition pp
            left join fetch pp.skills pps
            left join fetch pps.skill s
            where pp.proposal.id = :proposalId
            """)
    List<ProposalPosition> findPositionsWithSkillsByProposalId(Long proposalId);
}
