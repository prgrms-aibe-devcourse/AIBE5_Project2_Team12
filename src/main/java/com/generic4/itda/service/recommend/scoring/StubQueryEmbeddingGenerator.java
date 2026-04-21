package com.generic4.itda.service.recommend.scoring;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StubQueryEmbeddingGenerator implements QueryEmbeddingGenerator {

    @Override
    public List<Double> generate(String queryText) {
        throw new UnsupportedOperationException("쿼리 임베딩 생성은 별도 이슈에서 구현합니다.");
    }
}
