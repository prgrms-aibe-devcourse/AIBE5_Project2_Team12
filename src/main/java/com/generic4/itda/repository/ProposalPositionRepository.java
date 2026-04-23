package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.ProposalPosition;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProposalPositionRepository extends JpaRepository<ProposalPosition, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pp from ProposalPosition pp where pp.id = :id")
    Optional<ProposalPosition> findByIdForUpdate(@Param("id") Long id);
}
