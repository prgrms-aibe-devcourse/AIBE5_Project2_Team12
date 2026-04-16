package com.generic4.itda.domain.recommendation;

import com.generic4.itda.domain.recommendation.converter.EmbeddingVectorConverter;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.domain.shared.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_resume_embedding_resume_model",
                        columnNames = {"resume_id", "embedding_model"}
                )
        }
)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumeEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false, updatable = false)
    private Resume resume;

    @Embedded
    @AttributeOverride(
            name = "value",
            column = @Column(name = "source_hash", nullable = false, length = 128)
    )
    private SourceHash sourceHash;

    @Column(name = "embedding_model", length = 100, nullable = false)
    private String embeddingModel;

    @Convert(converter = EmbeddingVectorConverter.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private EmbeddingVector embeddingVector;

    private ResumeEmbedding(Resume resume, SourceHash sourceHash, String embeddingModel,
            EmbeddingVector embeddingVector) {
        Assert.notNull(resume, "이력서는 필수입니다.");
        Assert.notNull(sourceHash, "sourceHash는 필수입니다.");
        Assert.hasText(embeddingModel, "임베딩 모델명은 필수입니다.");
        Assert.notNull(embeddingVector, "embeddingVector는 필수입니다.");

        String normalizedModel = embeddingModel.trim();
        Assert.isTrue(normalizedModel.length() <= 100, "임베딩 모델명은 100자를 초과할 수 없습니다.");

        this.resume = resume;
        this.sourceHash = sourceHash;
        this.embeddingModel = normalizedModel;
        this.embeddingVector = embeddingVector;
    }

    public static ResumeEmbedding create(
            Resume resume,
            SourceHash sourceHash,
            String embeddingModel,
            EmbeddingVector embeddingVector
    ) {
        return new ResumeEmbedding(resume, sourceHash, embeddingModel, embeddingVector);
    }

    public void refresh(SourceHash sourceHash, EmbeddingVector embeddingVector) {
        refresh(sourceHash, embeddingModel, embeddingVector);
    }

    public void refresh(SourceHash sourceHash, String embeddingModel, EmbeddingVector embeddingVector) {
        Assert.notNull(sourceHash, "sourceHash는 필수입니다.");
        Assert.hasText(embeddingModel, "임베딩 모델명은 필수입니다.");
        Assert.notNull(embeddingVector, "embeddingVector는 필수입니다.");

        String normalizedModel = embeddingModel.trim();
        Assert.isTrue(normalizedModel.length() <= 100, "임베딩 모델명은 100자를 초과할 수 없습니다.");

        this.sourceHash = sourceHash;
        this.embeddingModel = normalizedModel;
        this.embeddingVector = embeddingVector;
    }

    public boolean isSameSource(SourceHash sourceHash) {
        Assert.notNull(sourceHash, "sourceHash는 필수입니다.");
        return this.sourceHash.equals(sourceHash);
    }

    public boolean isSameModel(String embeddingModel) {
        Assert.hasText(embeddingModel, "임베딩 모델명은 필수입니다.");
        return this.embeddingModel.equals(embeddingModel.trim());
    }
}
