package com.generic4.itda.service.recommend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.generic4.itda.domain.recommendation.vo.SourceHash;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ResumeEmbeddingSourceHashGenerator 단위 테스트")
class ResumeEmbeddingSourceHashGeneratorTest {

    private final ResumeEmbeddingSourceHashGenerator generator = new ResumeEmbeddingSourceHashGenerator();

    @Test
    @DisplayName("동일한 입력 문자열에 대해 항상 동일한 SourceHash를 반환해야 한다")
    void should_return_same_hash_for_same_input() {
        // given
        String sourceText = "This is a sample source text for hashing.";

        // when
        SourceHash hash1 = generator.generate(sourceText);
        SourceHash hash2 = generator.generate(sourceText);

        // then
        // 객체 equals 대신 getValue() 값을 직접 비교하여 검증
        assertThat(hash1.getValue()).isEqualTo(hash2.getValue());
    }

    @Test
    @DisplayName("서로 다른 입력 문자열에 대해 서로 다른 SourceHash를 반환해야 한다")
    void should_return_different_hash_for_different_input() {
        // given
        String sourceText1 = "First input text";
        String sourceText2 = "Second input text";

        // when
        SourceHash hash1 = generator.generate(sourceText1);
        SourceHash hash2 = generator.generate(sourceText2);

        // then
        assertThat(hash1.getValue()).isNotEqualTo(hash2.getValue());
    }

    @Test
    @DisplayName("현재 생성기 구현은 입력 문자열을 trim()하지 않으므로 공백이 다르면 다른 해시를 반환한다")
    void should_return_different_hash_when_whitespaces_differ_due_to_no_trim_in_generator() {
        // given
        String sourceText = "  Normal text  ";
        String trimmedText = "Normal text";

        // when
        SourceHash hash1 = generator.generate(sourceText);
        SourceHash hash2 = generator.generate(trimmedText);

        // then
        // 현재 generator 구현(sourceText.getBytes())에 따르면 두 해시는 달라야 함
        assertThat(hash1.getValue()).isNotEqualTo(hash2.getValue());
    }

    @Nested
    @DisplayName("잘못된 입력 처리")
    class InvalidInputTest {

        @Test
        @DisplayName("null 입력 시 예외가 발생해야 한다")
        void should_throw_exception_when_input_is_null() {
            assertThatThrownBy(() -> generator.generate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("빈 문자열 입력 시 예외가 발생해야 한다")
        void should_throw_exception_when_input_is_empty() {
            assertThatThrownBy(() -> generator.generate(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("공백 문자열 입력 시 예외가 발생해야 한다")
        void should_throw_exception_when_input_is_blank() {
            assertThatThrownBy(() -> generator.generate("   "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("반환된 SourceHash의 value는 SHA-256 hex 문자열이므로 길이는 64여야 한다")
    void should_have_length_64_for_sha256_hex_string() {
        // given
        String sourceText = "Any source text";

        // when
        SourceHash hash = generator.generate(sourceText);

        // then
        assertThat(hash.getValue()).hasSize(64);
        assertThat(hash.getValue()).matches("^[0-9a-f]{64}$");
    }
}
