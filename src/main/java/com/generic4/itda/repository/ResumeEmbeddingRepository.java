package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, Long> {

    List<ResumeEmbedding> findAllByResume_IdInAndEmbeddingModel(List<Long> resumeIds, String embeddingModel);

    Optional<ResumeEmbedding> findByResume_IdAndEmbeddingModel(Long resumeId, String embeddingModel);
}
