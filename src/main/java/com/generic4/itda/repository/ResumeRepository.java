package com.generic4.itda.repository;

import com.generic4.itda.domain.resume.Resume;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByMemberId(Long memberId);

    boolean existsByMemberEmailValue(String email);

    @Query("select r from Resume r " +
            "left join fetch r.skills " +
            "left join fetch r.attachments " +
            "where r.member.email.value = :email")
    Optional<Resume> findByMemberEmailWithDetails(@Param("email") String email);

    @Query("""
            select distinct r
            from Resume r
            left join fetch r.skills rs
            left join fetch rs.skill
            where r.id in :resumeIds
            """)
    List<Resume> findAllWithSkillsByIds(List<Long> resumeIds);
}
