package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.RecommendationResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    @Query("""
            select distinct rr from RecommendationResult rr
            join fetch rr.recommendationRun run
            join fetch run.proposalPosition pp
            join fetch pp.proposal p
            join fetch p.member m
            join fetch pp.position pos
            left join fetch pp.skills pps
            left join fetch pps.skill s
            where rr.resume.member.email.value = :email
            order by rr.createdAt desc
            """)
    List<RecommendationResult> findAllByResumeOwnerEmail(@Param("email") String email);
}
