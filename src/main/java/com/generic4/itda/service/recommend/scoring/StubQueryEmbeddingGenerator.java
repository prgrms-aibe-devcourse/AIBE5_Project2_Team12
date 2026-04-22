package com.generic4.itda.service.recommend.scoring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@ConditionalOnProperty(prefix = "ai.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class StubQueryEmbeddingGenerator implements QueryEmbeddingGenerator {

    private static final int VECTOR_DIMENSION = 64;

    @Override
    public List<Double> generate(String queryText) {
        Assert.hasText(queryText, "queryText는 비어있을 수 없습니다.");

        byte[] digest = sha256(queryText.trim().getBytes(StandardCharsets.UTF_8));
        List<Double> values = new ArrayList<>(VECTOR_DIMENSION);

        while (values.size() < VECTOR_DIMENSION) {
            appendValues(digest, values);
            digest = sha256(digest);
        }

        return normalize(values);
    }

    private void appendValues(byte[] digest, List<Double> values) {
        for (byte value : digest) {
            if (values.size() >= VECTOR_DIMENSION) {
                return;
            }
            values.add(((value & 0xFF) / 127.5d) - 1.0d);
        }
    }

    private List<Double> normalize(List<Double> values) {
        double magnitude = 0.0d;
        for (double value : values) {
            magnitude += value * value;
        }

        double divisor = magnitude == 0.0d ? 1.0d : Math.sqrt(magnitude);
        List<Double> normalized = new ArrayList<>(values.size());
        for (double value : values) {
            normalized.add(value / divisor);
        }
        return List.copyOf(normalized);
    }

    private byte[] sha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("stub 임베딩 생성에 실패했습니다.", e);
        }
    }
}
