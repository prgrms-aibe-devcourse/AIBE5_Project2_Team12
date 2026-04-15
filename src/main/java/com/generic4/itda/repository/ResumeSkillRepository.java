package com.generic4.itda.repository;

import com.generic4.itda.domain.resume.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {
    boolean existsByResumeIdAndSkillId(Long resumeId, Long skillId);

    List<ResumeSkill> findAllByResumeId(Long resumeId);

    Optional<ResumeSkill> findByResumeIdAndSkillId(Long resumeId, Long skillId);
}
