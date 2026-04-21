package com.generic4.itda.service.recommend.embedding;

import com.generic4.itda.domain.recommendation.vo.SourceHash;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class ResumeEmbeddingSourceHashGenerator {

    public SourceHash generate(String sourceText) {
        Assert.hasText(sourceText, "sourceText는 필수입니다.");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sourceText.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            for (byte hashByte : hashBytes) {
                builder.append(String.format("%02x", hashByte));
            }

            return new SourceHash(builder.toString());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("sourceHash 생성에 실패했습니다.", e);
        }
    }
}
