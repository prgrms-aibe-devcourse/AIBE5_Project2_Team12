package com.generic4.itda.repository;

import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeEmbeddingRepository extends JpaRepository<ResumeEmbedding, Long> {

    List<ResumeEmbedding> findAllByResume_IdInAndEmbeddingModel(List<Long> resumeIds, String embeddingModel);
}
