package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CosineSimilarityCalculatorTest {

    private final CosineSimilarityCalculator calculator = new CosineSimilarityCalculator();

    @DisplayName("동일한 두 벡터의 코사인 유사도는 1.0 이어야 한다")
    @Test
    void calculate_identicalVectors_returnsOne() {
        // given
        List<Double> query = List.of(1.0, 2.0, 3.0);
        List<Double> target = List.of(1.0, 2.0, 3.0);

        // when
        double result = calculator.calculate(query, target);

        // then
        assertThat(result).isCloseTo(1.0, offset(1e-9));
    }

    @DisplayName("서로 직교하는 두 벡터의 코사인 유사도는 0.0 이어야 한다")
    @Test
    void calculate_orthogonalVectors_returnsZero() {
        // given
        List<Double> query = List.of(1.0, 0.0);
        List<Double> target = List.of(0.0, 1.0);

        // when
        double result = calculator.calculate(query, target);

        // then
        assertThat(result).isCloseTo(0.0, offset(1e-9));
    }

    @DisplayName("서로 다른 두 벡터의 코사인 유사도를 정확히 계산한다")
    @Test
    void calculate_differentVectors_returnsCorrectSimilarity() {
        // given
        // query: (1, 1), target: (0, 1)
        // dotProduct: 1*0 + 1*1 = 1
        // queryNorm: sqrt(1^2 + 1^2) = sqrt(2)
        // targetNorm: sqrt(0^2 + 1^2) = 1
        // similarity: 1 / (sqrt(2) * 1) = 1/sqrt(2) ≈ 0.707106781
        List<Double> query = List.of(1.0, 1.0);
        List<Double> target = List.of(0.0, 1.0);

        // when
        double result = calculator.calculate(query, target);

        // then
        assertThat(result).isCloseTo(1.0 / Math.sqrt(2.0), offset(1e-9));
    }

    @DisplayName("벡터 중 하나가 영벡터인 경우 유사도는 0.0 이어야 한다")
    @Test
    void calculate_withZeroVector_returnsZero() {
        // given
        List<Double> query = List.of(0.0, 0.0);
        List<Double> target = List.of(1.0, 1.0);

        // when
        double result = calculator.calculate(query, target);

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @DisplayName("입력 리스트가 null인 경우 IllegalArgumentException이 발생한다")
    @Test
    void calculate_nullInput_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(null, List.of(1.0)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> calculator.calculate(List.of(1.0), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("입력 리스트가 비어 있는 경우 IllegalArgumentException이 발생한다")
    @Test
    void calculate_emptyInput_throwsException() {
        assertThatThrownBy(() -> calculator.calculate(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("두 벡터의 차원이 다른 경우 IllegalArgumentException이 발생한다")
    @Test
    void calculate_mismatchedDimensions_throwsException() {
        // given
        List<Double> query = List.of(1.0, 2.0);
        List<Double> target = List.of(1.0, 2.0, 3.0);

        // then
        assertThatThrownBy(() -> calculator.calculate(query, target))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("코사인 유사도는 대칭성을 만족해야 한다")
    @Test
    void calculate_isSymmetric() {
        // given
        List<Double> a = List.of(1.0, 2.0, 3.0);
        List<Double> b = List.of(4.0, 5.0, 6.0);

        // when
        double ab = calculator.calculate(a, b);
        double ba = calculator.calculate(b, a);

        // then
        assertThat(ab).isCloseTo(ba, offset(1e-9));
    }
}
