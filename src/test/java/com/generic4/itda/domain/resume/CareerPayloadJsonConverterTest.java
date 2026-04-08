package com.generic4.itda.domain.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CareerPayloadJsonConverterTest {

    private final CareerPayloadJsonConverter converter = new CareerPayloadJsonConverter();

    @DisplayName("경력 payload를 JSON 문자열로 직렬화하고 다시 복원한다")
    @Test
    void convertCareerPayloadRoundTrip() {
        CareerPayload payload = createPayload();

        String json = converter.convertToDatabaseColumn(payload);
        CareerPayload restored = converter.convertToEntityAttribute(json);

        assertThat(json).contains("\"companyName\":\"Generic4\"");
        assertThat(restored.getItems()).hasSize(1);
        assertThat(restored.getItems().get(0).getCompanyName()).isEqualTo("Generic4");
        assertThat(restored.getItems().get(0).getCurrentlyWorking()).isTrue();
    }

    @DisplayName("잘못된 JSON 문자열이면 역직렬화에 실패한다")
    @Test
    void failWhenJsonIsInvalid() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{invalid-json}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("경력 JSON 역직렬화에 실패했습니다.");
    }

    private CareerPayload createPayload() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));

        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(item));
        return payload;
    }
}
