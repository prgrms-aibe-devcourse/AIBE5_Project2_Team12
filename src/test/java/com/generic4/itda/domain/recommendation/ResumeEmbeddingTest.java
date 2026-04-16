package com.generic4.itda.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.generic4.itda.domain.recommendation.vo.EmbeddingVector;
import com.generic4.itda.domain.recommendation.vo.SourceHash;
import com.generic4.itda.domain.resume.Resume;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ResumeEmbeddingTest {

    private static final SourceHash HASH_A = new SourceHash("abc123");
    private static final SourceHash HASH_B = new SourceHash("def456");
    private static final EmbeddingVector VECTOR_A = new EmbeddingVector(List.of(0.1, 0.2, 0.3));
    private static final EmbeddingVector VECTOR_B = new EmbeddingVector(List.of(0.4, 0.5, 0.6));
    private static final String MODEL = "text-embedding-3-small";

    private static Resume createResume() {
        return mock(Resume.class);
    }

    private static ResumeEmbedding createEmbedding() {
        return ResumeEmbedding.create(createResume(), HASH_A, MODEL, VECTOR_A);
    }

    // -------------------------------------------------------------------------
    // 1. 생성
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create() 정적 팩토리 메서드")
    class Create {

        @Test
        @DisplayName("유효한 인자가 주어지면 필드 값이 올바르게 설정된다")
        void 정상_생성_시_필드_설정() {
            // given
            Resume resume = createResume();

            // when
            ResumeEmbedding embedding = ResumeEmbedding.create(resume, HASH_A, MODEL, VECTOR_A);

            // then
            assertThat(embedding.getResume()).isSameAs(resume);
            assertThat(embedding.getSourceHash()).isEqualTo(HASH_A);
            assertThat(embedding.getEmbeddingModel()).isEqualTo(MODEL);
            assertThat(embedding.getEmbeddingVector()).isEqualTo(VECTOR_A);
        }

        @Test
        @DisplayName("embeddingModel 앞뒤 공백은 trim되어 저장된다")
        void embeddingModel_공백_trim() {
            // given
            String modelWithSpaces = "  " + MODEL + "  ";

            // when
            ResumeEmbedding embedding = ResumeEmbedding.create(createResume(), HASH_A, modelWithSpaces, VECTOR_A);

            // then
            assertThat(embedding.getEmbeddingModel()).isEqualTo(MODEL);
        }

        @Test
        @DisplayName("resume가 null이면 예외가 발생한다")
        void resume_null이면_예외() {
            assertThatThrownBy(() -> ResumeEmbedding.create(null, HASH_A, MODEL, VECTOR_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("sourceHash가 null이면 예외가 발생한다")
        void sourceHash_null이면_예외() {
            assertThatThrownBy(() -> ResumeEmbedding.create(createResume(), null, MODEL, VECTOR_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingModel이 null이면 예외가 발생한다")
        void embeddingModel_null이면_예외() {
            assertThatThrownBy(() -> ResumeEmbedding.create(createResume(), HASH_A, null, VECTOR_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingModel이 빈 문자열이면 예외가 발생한다")
        void embeddingModel_blank이면_예외() {
            assertThatThrownBy(() -> ResumeEmbedding.create(createResume(), HASH_A, "   ", VECTOR_A))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingVector가 null이면 예외가 발생한다")
        void embeddingVector_null이면_예외() {
            assertThatThrownBy(() -> ResumeEmbedding.create(createResume(), HASH_A, MODEL, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 2. refresh (모델 유지)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refresh(sourceHash, embeddingVector) — 모델 유지 갱신")
    class RefreshWithoutModel {

        @Test
        @DisplayName("sourceHash와 embeddingVector가 새 값으로 갱신된다")
        void sourceHash와_embeddingVector_갱신() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when
            embedding.refresh(HASH_B, VECTOR_B);

            // then
            assertThat(embedding.getSourceHash()).isEqualTo(HASH_B);
            assertThat(embedding.getEmbeddingVector()).isEqualTo(VECTOR_B);
        }

        @Test
        @DisplayName("embeddingModel은 변경되지 않는다")
        void embeddingModel_변경_없음() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when
            embedding.refresh(HASH_B, VECTOR_B);

            // then
            assertThat(embedding.getEmbeddingModel()).isEqualTo(MODEL);
        }

        @Test
        @DisplayName("sourceHash가 null이면 예외가 발생한다")
        void sourceHash_null이면_예외() {
            ResumeEmbedding embedding = createEmbedding();

            assertThatThrownBy(() -> embedding.refresh(null, VECTOR_B))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingVector가 null이면 예외가 발생한다")
        void embeddingVector_null이면_예외() {
            ResumeEmbedding embedding = createEmbedding();

            assertThatThrownBy(() -> embedding.refresh(HASH_B, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // 3. refresh (모델 변경 포함)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("refresh(sourceHash, embeddingModel, embeddingVector) — 모델 변경 포함 갱신")
    class RefreshWithModel {

        @Test
        @DisplayName("세 필드가 모두 새 값으로 갱신된다")
        void 세_필드_모두_갱신() {
            // given
            ResumeEmbedding embedding = createEmbedding();
            String newModel = "text-embedding-ada-002";

            // when
            embedding.refresh(HASH_B, newModel, VECTOR_B);

            // then
            assertThat(embedding.getSourceHash()).isEqualTo(HASH_B);
            assertThat(embedding.getEmbeddingModel()).isEqualTo(newModel);
            assertThat(embedding.getEmbeddingVector()).isEqualTo(VECTOR_B);
        }

        @Test
        @DisplayName("embeddingModel 앞뒤 공백은 trim되어 저장된다")
        void embeddingModel_공백_trim() {
            // given
            ResumeEmbedding embedding = createEmbedding();
            String newModel = "text-embedding-ada-002";

            // when
            embedding.refresh(HASH_B, "  " + newModel + "  ", VECTOR_B);

            // then
            assertThat(embedding.getEmbeddingModel()).isEqualTo(newModel);
        }

        @Test
        @DisplayName("embeddingModel이 빈 문자열이면 예외가 발생한다")
        void embeddingModel_blank이면_예외() {
            ResumeEmbedding embedding = createEmbedding();

            assertThatThrownBy(() -> embedding.refresh(HASH_B, "   ", VECTOR_B))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingModel 길이가 100을 초과하면 예외가 발생한다")
        void embeddingModel_100자_초과_예외() {
            // given
            ResumeEmbedding embedding = createEmbedding();
            String tooLongModel = "a".repeat(101);

            // when / then
            assertThatThrownBy(() -> embedding.refresh(HASH_B, tooLongModel, VECTOR_B))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("embeddingModel 길이가 정확히 100이면 정상 처리된다")
        void embeddingModel_100자_경계값_성공() {
            // given
            ResumeEmbedding embedding = createEmbedding();
            String exactModel = "a".repeat(100);

            // when
            embedding.refresh(HASH_B, exactModel, VECTOR_B);

            // then
            assertThat(embedding.getEmbeddingModel()).isEqualTo(exactModel);
        }
    }

    // -------------------------------------------------------------------------
    // 4. 동등성 체크
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("isSameSource() — sourceHash 동등성 확인")
    class IsSameSource {

        @Test
        @DisplayName("동일한 sourceHash이면 true를 반환한다")
        void 동일_hash이면_true() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when / then
            assertThat(embedding.isSameSource(HASH_A)).isTrue();
        }

        @Test
        @DisplayName("다른 sourceHash이면 false를 반환한다")
        void 다른_hash이면_false() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when / then
            assertThat(embedding.isSameSource(HASH_B)).isFalse();
        }

        @Test
        @DisplayName("null이면 예외가 발생한다")
        void null이면_예외() {
            ResumeEmbedding embedding = createEmbedding();

            assertThatThrownBy(() -> embedding.isSameSource(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("isSameModel() — embeddingModel 동등성 확인")
    class IsSameModel {

        @Test
        @DisplayName("동일한 모델명이면 true를 반환한다")
        void 동일_모델명이면_true() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when / then
            assertThat(embedding.isSameModel(MODEL)).isTrue();
        }

        @Test
        @DisplayName("앞뒤 공백이 포함된 동일 모델명이면 true를 반환한다")
        void 공백_포함_동일_모델명이면_true() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when / then
            assertThat(embedding.isSameModel("  " + MODEL + "  ")).isTrue();
        }

        @Test
        @DisplayName("다른 모델명이면 false를 반환한다")
        void 다른_모델명이면_false() {
            // given
            ResumeEmbedding embedding = createEmbedding();

            // when / then
            assertThat(embedding.isSameModel("text-embedding-ada-002")).isFalse();
        }

        @Test
        @DisplayName("빈 문자열이면 예외가 발생한다")
        void blank이면_예외() {
            ResumeEmbedding embedding = createEmbedding();

            assertThatThrownBy(() -> embedding.isSameModel("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
