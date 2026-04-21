package com.generic4.itda.service.recommend.scoring;

import java.util.List;
import java.util.Map;

public interface ResumeEmbeddingReader {

    Map<Long, List<Double>> readEmbeddingsByResumeIds(List<Long> resumeIds, String embeddingModel);
}
