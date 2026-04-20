package com.generic4.itda.service.recommend.scoring;

import java.util.List;

public interface QueryEmbeddingGenerator {

    List<Double> generate(String queryText);
}
