package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CareerAdjustmentCalculator 단위 테스트")
class CareerAdjustmentCalculatorTest {

    private final CareerAdjustmentCalculator calculator = new CareerAdjustmentCalculator();

    @Test
    @DisplayName("최소/최대 경력 요구사항이 모두 없을 경우 0.0을 반환한다.")
    void calculate_WhenNoRequirements() {
        // given
        int candidateYears = 5;
        Integer minYears = null;
        Integer maxYears = null;

        // when
        double result = calculator.calculate(candidateYears, minYears, maxYears);

        // then
        assertThat(result).isEqualTo(0.0);
    }

    @ParameterizedTest
    @CsvSource({
            "5, 3, 7",  // 범위 중간
            "3, 3, 7",  // 최소값 경계
            "7, 3, 7",  // 최대값 경계
            "5, 3, ",   // 최소값만 있고 충족
            "5, , 7"    // 최대값만 있고 충족
    })
    @DisplayName("경력 요구사항 범위를 충족할 경우 보너스 점수(0.08)를 반환한다.")
    void calculate_WhenInRange(int candidateYears, Integer minYears, Integer maxYears) {
        // when
        double result = calculator.calculate(candidateYears, minYears, maxYears);

        // then
        assertThat(result).isCloseTo(0.08, offset(1e-9));
    }

    @ParameterizedTest
    @CsvSource({
            "2, 3, -0.04", // 1년 부족
            "1, 3, -0.08", // 2년 부족
            "0, 3, -0.12", // 3년 부족 (최대 페널티)
            "0, 5, -0.12"  // 5년 부족 (최대 페널티 캡핑)
    })
    @DisplayName("최소 경력보다 부족할 경우 연당 -0.04의 페널티를 부여하며 최대 -0.12까지 적용한다.")
    void calculate_WhenBelowMin(int candidateYears, Integer minYears, double expected) {
        // when
        double result = calculator.calculate(candidateYears, minYears, 10);

        // then
        assertThat(result).isCloseTo(expected, offset(1e-9));
    }

    @ParameterizedTest
    @CsvSource({
            "6, 5, -0.02", // 1년 초과
            "7, 5, -0.04", // 2년 초과
            "8, 5, -0.06", // 3년 초과 (최대 페널티)
            "10, 5, -0.06" // 5년 초과 (최대 페널티 캡핑)
    })
    @DisplayName("최대 경력보다 초과할 경우 연당 -0.02의 페널티를 부여하며 최대 -0.06까지 적용한다.")
    void calculate_WhenAboveMax(int candidateYears, Integer maxYears, double expected) {
        // when
        double result = calculator.calculate(candidateYears, 0, maxYears);

        // then
        assertThat(result).isCloseTo(expected, offset(1e-9));
    }
}
