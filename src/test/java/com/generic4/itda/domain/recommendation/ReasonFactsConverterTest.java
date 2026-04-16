package com.generic4.itda.domain.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.generic4.itda.domain.recommendation.converter.ReasonFactsConverter;
import com.generic4.itda.domain.recommendation.vo.ReasonFacts;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReasonFactsConverterTest {

    private final ReasonFactsConverter converter = new ReasonFactsConverter();

    @Test
    void convert() {
        ReasonFacts reasonFacts = new ReasonFacts(
                List.of("Spring Boot", "AWS"),
                List.of("핀테크"),
                5,
                List.of("대시보드 구축 경험", "실시간 데이터 처리 경험")
        );

        String json = converter.convertToDatabaseColumn(reasonFacts);
        ReasonFacts restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isEqualTo(reasonFacts);
    }
}