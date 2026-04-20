package com.generic4.itda.service.recommend.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SkillAdjustmentCalculatorTest {

    private final SkillAdjustmentCalculator calculator = new SkillAdjustmentCalculator();

    @DisplayName("필수 기술과 선택 기술이 모두 일치할 때 가산점을 정확히 계산한다")
    @Test
    void calculate_allSkillsMatch_returnsMaxBonus() {
        // given
        Set<String> required = Set.of("Java", "Spring");
        Set<String> preferred = Set.of("Docker", "AWS");
        Set<String> owned = Set.of("Java", "Spring", "Docker", "AWS", "Git");

        // when
        // requiredAdjustment: (1.0 - 0.5) * 0.3 = 0.15
        // preferredAdjustment: 1.0 * 0.05 = 0.05
        // total: 0.20
        double result = calculator.calculate(required, preferred, owned);

        // then
        assertThat(result).isCloseTo(0.20, offset(1e-9));
    }

    @DisplayName("일치하는 기술이 하나도 없을 때 감점을 정확히 계산한다")
    @Test
    void calculate_noSkillsMatch_returnsMinPenalty() {
        // given
        Set<String> required = Set.of("Java", "Spring");
        Set<String> preferred = Set.of("Docker", "AWS");
        Set<String> owned = Set.of("Python", "Django");

        // when
        // requiredAdjustment: (0.0 - 0.5) * 0.3 = -0.15
        // preferredAdjustment: 0.0 * 0.05 = 0.0
        // total: -0.15
        double result = calculator.calculate(required, preferred, owned);

        // then
        assertThat(result).isCloseTo(-0.15, offset(1e-9));
    }

    @DisplayName("필수 기술이 절반만 일치할 때 필수 기술 보정치는 0이어야 한다")
    @Test
    void calculate_halfRequiredSkillsMatch_returnsZeroRequiredAdjustment() {
        // given
        Set<String> required = Set.of("Java", "Spring", "JPA", "Querydsl");
        Set<String> preferred = Collections.emptySet();
        Set<String> owned = Set.of("Java", "Spring");

        // when
        // requiredAdjustment: (2/4 - 0.5) * 0.3 = 0.0
        // preferredAdjustment: 0.0
        double result = calculator.calculate(required, preferred, owned);

        // then
        assertThat(result).isCloseTo(0.0, offset(1e-9));
    }

    @DisplayName("기술 목록이 null이거나 비어있을 때 0.0을 반환한다")
    @Test
    void calculate_emptyOrNullSkills_returnsZero() {
        // when & then
        assertThat(calculator.calculate(null, null, Set.of("Java"))).isEqualTo(0.0);
        assertThat(calculator.calculate(Collections.emptySet(), Collections.emptySet(), Set.of("Java"))).isEqualTo(0.0);
    }

    @DisplayName("선택 기술만 일부 일치할 때 가산점을 정확히 계산한다")
    @Test
    void calculate_onlyPreferredSkillsMatch_returnsCorrectBonus() {
        // given
        Set<String> required = Collections.emptySet();
        Set<String> preferred = Set.of("Docker", "AWS", "Kubernetes", "Terraform");
        Set<String> owned = Set.of("Docker", "AWS");

        // when
        // requiredAdjustment: 0.0
        // preferredAdjustment: (2/4) * 0.05 = 0.025
        double result = calculator.calculate(required, preferred, owned);

        // then
        assertThat(result).isCloseTo(0.025, offset(1e-9));
    }

    @DisplayName("필수 기술과 중복된 우대 기술은 우대 가산점에 중복 반영되지 않는다")
    @Test
    void calculate_overlappingPreferredSkills_doesNotDoubleCount() {
        // given
        Set<String> required = Set.of("Java", "Spring");
        Set<String> preferred = Set.of("Spring", "AWS");
        Set<String> owned = Set.of("Java", "Spring");

        // when
        double result = calculator.calculate(required, preferred, owned);

        // then
        // requiredAdjustment: (2/2 - 0.5) * 0.3 = 0.15
        // preferredAdjustment: AWS만 유효 preferred로 남으면 0.0
        assertThat(result).isCloseTo(0.15, offset(1e-9));
    }

    @DisplayName("보유 기술 수가 많아도 required/preferred 기준으로만 보정한다")
    @Test
    void calculate_extraOwnedSkills_doNotAffectAdjustment() {
        // given
        Set<String> required = Set.of("Java", "Spring");
        Set<String> preferred = Set.of("Docker");
        Set<String> owned1 = Set.of("Java", "Spring", "Docker");
        Set<String> owned2 = Set.of("Java", "Spring", "Docker", "Redis", "Kafka", "MySQL");

        // when
        double result1 = calculator.calculate(required, preferred, owned1);
        double result2 = calculator.calculate(required, preferred, owned2);

        // then
        assertThat(result1).isCloseTo(result2, offset(1e-9));
    }
}
