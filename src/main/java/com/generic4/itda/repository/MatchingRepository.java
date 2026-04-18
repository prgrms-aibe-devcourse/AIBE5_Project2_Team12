package com.generic4.itda.repository;

import com.generic4.itda.domain.matching.Matching;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchingRepository extends JpaRepository<Matching, Long>, MatchingRepositoryCustom {
}
