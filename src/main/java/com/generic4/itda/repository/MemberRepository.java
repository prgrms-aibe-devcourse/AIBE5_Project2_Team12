package com.generic4.itda.repository;

import com.generic4.itda.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Member findByEmail_Value(String email);

    boolean existsByEmail_Value(String email);
}
