package com.generic4.itda.repository;

import com.generic4.itda.domain.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {
    Optional<Resume> findByMemberId(Long memberId);
}
