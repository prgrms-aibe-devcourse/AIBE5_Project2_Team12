package com.generic4.itda.service.recommend.scoring;

import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.repository.ResumeEmbeddingRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@RequiredArgsConstructor
public class ResumeEmbeddingReaderImpl implements ResumeEmbeddingReader {

    private final ResumeEmbeddingRepository resumeEmbeddingRepository;

    @Override
    public Map<Long, List<Double>> readEmbeddingsByResumeIds(List<Long> resumeIds, String embeddingModel) {
        Assert.hasText(embeddingModel, "임베딩 모델명은 필수입니다.");

        if (resumeIds == null || resumeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return resumeEmbeddingRepository.findAllByResume_IdInAndEmbeddingModel(resumeIds, embeddingModel).stream()
                .collect(Collectors.toMap(
                        embedding -> embedding.getResume().getId(),
                        embedding -> embedding.getEmbeddingVector().values()
                ));
    }
}
