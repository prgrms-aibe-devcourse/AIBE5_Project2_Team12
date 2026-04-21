package com.generic4.itda.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.generic4.itda.config.ai.AiEmbeddingProperties;
import com.generic4.itda.domain.recommendation.ResumeEmbedding;
import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.Resume;
import com.generic4.itda.repository.ResumeEmbeddingRepository;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingSourceHashGenerator;
import com.generic4.itda.service.recommend.embedding.ResumeEmbeddingTextGenerator;
import com.generic4.itda.service.recommend.scoring.QueryEmbeddingGenerator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeEmbeddingService 단위 테스트")
class ResumeEmbeddingServiceTest {

    private static final Long RESUME_ID = 1L;
    private static final String MODEL = "test-model";

    @Mock
    private ResumeEmbeddingRepository resumeEmbeddingRepository;

    @Mock
    private ResumeEmbeddingTextGenerator resumeEmbeddingTextGenerator;

    @Mock
    private ResumeEmbeddingSourceHashGenerator resumeEmbeddingSourceHashGenerator;

    @Mock
    private QueryEmbeddingGenerator queryEmbeddingGenerator;

    @Mock
    private AiEmbeddingProperties aiEmbeddingProperties;

    @InjectMocks
    private ResumeEmbeddingService resumeEmbeddingService;

    @Nested
    @DisplayName("createOrRefresh 메서드는")
    class CreateOrRefresh {

        @Test
        @DisplayName("resume이 null이면 예외가 발생하고 아무 작업도 수행하지 않는다")
        void should_throw_when_resume_is_null() {
            assertThatThrownBy(() -> resumeEmbeddingService.createOrRefresh(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("resume는 필수입니다.");

            verifyNoInteractions(
                    resumeEmbeddingTextGenerator,
                    resumeEmbeddingSourceHashGenerator,
                    aiEmbeddingProperties,
                    queryEmbeddingGenerator,
                    resumeEmbeddingRepository
            );
        }

        @Test
        @DisplayName("resume id가 null이면 예외가 발생하고 아무 작업도 수행하지 않는다")
        void should_throw_when_resume_id_is_null() {
            Resume resume = createResume(null);

            assertThatThrownBy(() -> resumeEmbeddingService.createOrRefresh(resume))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("저장된 resume만 임베딩을 생성할 수 있습니다.");

            verifyNoInteractions(
                    resumeEmbeddingTextGenerator,
                    resumeEmbeddingSourceHashGenerator,
                    aiEmbeddingProperties,
                    queryEmbeddingGenerator,
                    resumeEmbeddingRepository
            );
        }

        @Test
        @DisplayName("기존 embedding이 없으면 새로 생성하여 저장한다")
        void should_create_new_embedding_when_not_exists() {
            // given
            Resume resume = createResume(RESUME_ID);
            String text = "extracted text";
            SourceHash hash = new SourceHash("hash-val");
            List<Double> vectorValues = List.of(0.1, 0.2);
            ArgumentCaptor<ResumeEmbedding> savedEmbeddingCaptor = ArgumentCaptor.forClass(ResumeEmbedding.class);

            given(resumeEmbeddingTextGenerator.generate(resume)).willReturn(text);
            given(resumeEmbeddingSourceHashGenerator.generate(text)).willReturn(hash);
            given(aiEmbeddingProperties.getModel()).willReturn(MODEL);
            given(resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(RESUME_ID, MODEL))
                    .willReturn(Optional.empty());
            given(queryEmbeddingGenerator.generate(text)).willReturn(vectorValues);
            given(resumeEmbeddingRepository.save(any(ResumeEmbedding.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // when
            ResumeEmbedding result = resumeEmbeddingService.createOrRefresh(resume);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getResume()).isSameAs(resume);
            assertThat(result.getEmbeddingModel()).isEqualTo(MODEL);
            assertThat(result.getSourceHash()).isEqualTo(hash);
            assertThat(result.getEmbeddingVector().values()).isEqualTo(vectorValues);
            verify(queryEmbeddingGenerator).generate(text);
            verify(resumeEmbeddingRepository).save(savedEmbeddingCaptor.capture());
            ResumeEmbedding savedEmbedding = savedEmbeddingCaptor.getValue();
            assertThat(savedEmbedding.getResume()).isSameAs(resume);
            assertThat(savedEmbedding.getSourceHash()).isEqualTo(hash);
            assertThat(savedEmbedding.getEmbeddingModel()).isEqualTo(MODEL);
            assertThat(savedEmbedding.getEmbeddingVector().values()).isEqualTo(vectorValues);
        }

        @Test
        @DisplayName("기존 embedding이 있고 sourceHash가 동일하면 재사용한다")
        void should_reuse_existing_embedding_when_source_hash_matches() {
            // given
            Resume resume = createResume(RESUME_ID);
            String text = "extracted text";
            SourceHash hash = new SourceHash("hash-val");

            ResumeEmbedding existing = createExistingEmbedding(resume, hash, List.of(0.1, 0.2));

            given(resumeEmbeddingTextGenerator.generate(resume)).willReturn(text);
            given(resumeEmbeddingSourceHashGenerator.generate(text)).willReturn(hash);
            given(aiEmbeddingProperties.getModel()).willReturn(MODEL);
            given(resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(RESUME_ID, MODEL))
                    .willReturn(Optional.of(existing));

            // when
            ResumeEmbedding result = resumeEmbeddingService.createOrRefresh(resume);

            // then
            assertThat(result).isSameAs(existing);
            verify(queryEmbeddingGenerator, never()).generate(any());
            verify(resumeEmbeddingRepository, never()).save(any());
        }

        @Test
        @DisplayName("기존 embedding이 있지만 sourceHash가 다르면 실제 객체의 필드를 갱신한다")
        void should_refresh_existing_embedding_when_source_hash_differs() {
            // given
            Resume resume = createResume(RESUME_ID);
            String text = "updated text";
            SourceHash oldHash = new SourceHash("old-hash");
            SourceHash newHash = new SourceHash("new-hash");
            List<Double> oldVectorValues = List.of(0.1, 0.2);
            List<Double> newVectorValues = List.of(0.3, 0.4);

            ResumeEmbedding existing = createExistingEmbedding(resume, oldHash, oldVectorValues);

            given(resumeEmbeddingTextGenerator.generate(resume)).willReturn(text);
            given(resumeEmbeddingSourceHashGenerator.generate(text)).willReturn(newHash);
            given(aiEmbeddingProperties.getModel()).willReturn(MODEL);
            given(resumeEmbeddingRepository.findByResume_IdAndEmbeddingModel(RESUME_ID, MODEL))
                    .willReturn(Optional.of(existing));
            given(queryEmbeddingGenerator.generate(text)).willReturn(newVectorValues);

            // when
            ResumeEmbedding result = resumeEmbeddingService.createOrRefresh(resume);

            // then
            assertThat(result).isSameAs(existing);
            assertThat(result.getEmbeddingModel()).isEqualTo(MODEL);
            assertThat(result.getSourceHash()).isEqualTo(newHash);
            assertThat(result.getEmbeddingVector().values()).isEqualTo(newVectorValues);
            verify(queryEmbeddingGenerator).generate(text);
            verify(resumeEmbeddingRepository, never()).save(any());
        }
    }

    private Resume createResume(Long resumeId) {
        Resume resume = mock(Resume.class);
        given(resume.getId()).willReturn(resumeId);
        return resume;
    }

    private ResumeEmbedding createExistingEmbedding(Resume resume, SourceHash hash, List<Double> vectorValues) {
        return ResumeEmbedding.create(
                resume,
                hash,
                MODEL,
                new EmbeddingVector(vectorValues)
        );
    }
}
