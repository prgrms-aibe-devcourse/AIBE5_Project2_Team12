package com.generic4.itda.service.recommend.scoring;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ai.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubQueryEmbeddingGenerator implements QueryEmbeddingGenerator {

    @Override
    public List<Double> generate(String queryText) {
        throw new UnsupportedOperationException("쿼리 임베딩 생성은 별도 이슈에서 구현합니다.");
    }
}
