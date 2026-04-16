package com.generic4.itda.repository;

import com.generic4.itda.domain.position.Position;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByName(String name);
}
