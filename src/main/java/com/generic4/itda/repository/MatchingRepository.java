package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.Matching;
import com.generic4.itda.domain.matching.constant.MatchingStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"proposalPosition"})
    List<Matching> findByProposalPosition_Proposal_IdAndClientMember_Email_Value(Long proposalId, String clientEmail);

    @EntityGraph(attributePaths = {"freelancerMember"})
    List<Matching> findByProposalPosition_IdAndClientMember_Email_Value(Long proposalPositionId, String clientEmail);

    /**
     * 주어진 포지션에 대해 resume ID 목록에 해당하는 매칭 상태를 한 번에 조회한다.
     * 추천 결과 페이지에서 후보별 매칭 상태를 N+1 없이 표시하기 위해 사용한다.
     */
    @Query("select m from Matching m where m.proposalPosition.id = :positionId and m.resume.id in :resumeIds")
    List<Matching> findByProposalPositionIdAndResumeIdIn(
            @Param("positionId") Long positionId,
            @Param("resumeIds") Collection<Long> resumeIds
    );

    @Query("""
            select m
            from Matching m
            join fetch m.freelancerMember freelancerMember
            where m.proposalPosition.id = :proposalPositionId
              and m.clientMember.email.value = :clientEmail
            """)
    List<Matching> findWithFreelancerMemberByProposalPositionIdAndClientEmail(
            @Param("proposalPositionId") Long proposalPositionId,
            @Param("clientEmail") String clientEmail
    );

    @Query("""
            select distinct m
            from Matching m
            join fetch m.proposalPosition pp
            join fetch pp.position position
            left join fetch pp.skills pps
            left join fetch pps.skill skill
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
