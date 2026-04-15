package com.generic4.itda.repository;


import com.generic4.itda.domain.skill.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    // 나중에 스킬명 검색이 필요해지면 이름 기준 조회 메서드 추가 검토
//    Optional<Skill> findByName(String name);
//    boolean existsByName(String Name);
}
