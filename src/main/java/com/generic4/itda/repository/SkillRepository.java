package com.generic4.itda.repository;

import com.generic4.itda.domain.skill.Skill;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByName(String name);
}
