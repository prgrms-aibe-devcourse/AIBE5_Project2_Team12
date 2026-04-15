package com.generic4.itda.repository;

import com.generic4.itda.domain.proposal.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

}
