package com.generic4.itda.service;

import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeEmbeddingRepository;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingSourceHashGenerator;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingTextGenerator;
import com.generic4.itda.service.recommend.scoring.QueryEmbeddingGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Service
@Transactional
@RequiredArgsConstructor
public class ResumeEmbeddingService {

    private final ResumeEmbeddingRepository resumeEmbeddingRepository;
    private final ResumeEmbeddingTextGenerator resumeEmbeddingTextGenerator;
    private final ResumeEmbeddingSourceHashGenerator resumeEmbeddingSourceHashGenerator;
    private final QueryEmbeddingGenerator queryEmbeddingGenerator;
    private final AiEmbeddingProperties aiEmbeddingProperties;

    public ResumeEmbedding createOrRefresh(Resume resume) {
        Assert.notNull(resume, "resume는 필수입니다.");
        Assert.notNull(resume.getId(), "저장된 resume만 임베딩을 생성할 수 있습니다.");

        String embeddingText = resumeEmbeddingTextGenerator.generate(resume);
        SourceHash sourceHash = resumeEmbeddingSourceHashGenerator.generate(embeddingText);
        String embeddingModel = aiEmbeddingProperties.getModel();

        ResumeEmbedding existing = resumeEmbeddingRepository
                .findByResume_IdAndEmbeddingModel(resume.getId(), embeddingModel)
                .orElse(null);

        if (existing != null && existing.isSameSource(sourceHash)) {
            return existing;
        }

        EmbeddingVector embeddingVector = new EmbeddingVector(
                queryEmbeddingGenerator.generate(embeddingText)
        );

        if (existing == null) {
            ResumeEmbedding created = ResumeEmbedding.create(
                    resume,
                    sourceHash,
                    embeddingModel,
                    embeddingVector
            );
            return resumeEmbeddingRepository.save(created);
        }

        existing.refresh(sourceHash, embeddingVector);
        return existing;
    }
}
