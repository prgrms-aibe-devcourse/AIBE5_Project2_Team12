package com.generic4.itda.repository;

import com.generic4.itda.domain.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByMemberId(Long memberId);

    boolean existsByMemberEmailValue(String email);

    @Query("select r from Resume r " +
            "left join fetch r.skills " +
            "left join fetch r.attachments " +
            "where r.member.email.value = :email")
    Optional<Resume> findByMemberEmailWithDetails(@Param("email") String email);
}
