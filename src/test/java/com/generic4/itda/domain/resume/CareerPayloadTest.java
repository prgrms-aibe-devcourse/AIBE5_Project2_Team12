package com.generic4.itda.domain.resume;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CareerPayloadTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("유효한 경력 JSON payload를 검증한다")
    @Test
    void validateCareerPayload() {
        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(createCurrentCareer()));

        Set<ConstraintViolation<CareerPayload>> violations = validator.validate(payload);

        assertThat(violations).isEmpty();
    }

    @DisplayName("경력이 없으면 빈 목록으로도 검증에 성공한다")
    @Test
    void validateCareerPayloadWithEmptyItems() {
        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of());

        Set<ConstraintViolation<CareerPayload>> violations = validator.validate(payload);

        assertThat(violations).isEmpty();
    }

    @DisplayName("종료 연월이 시작 연월보다 빠르면 검증에 실패한다")
    @Test
    void failWhenEndYearMonthIsBeforeStartYearMonth() {
        CareerPayload payload = new CareerPayload();
        CareerItemPayload item = createPastCareer();
        item.setStartYearMonth("2024-03");
        item.setEndYearMonth("2024-02");
        payload.setItems(List.of(item));

        Set<String> messages = validateMessages(payload);

        assertThat(messages).contains("종료 연월은 시작 연월보다 빠를 수 없습니다.");
    }

    @DisplayName("재직중이 아닌 경력에 종료 연월이 없으면 검증에 실패한다")
    @Test
    void failWhenPastCareerHasNoEndYearMonth() {
        CareerPayload payload = new CareerPayload();
        CareerItemPayload item = createPastCareer();
        item.setEndYearMonth(null);
        payload.setItems(List.of(item));

        Set<String> messages = validateMessages(payload);

        assertThat(messages).contains("재직중이 아닌 경력은 종료 연월이 필요합니다.");
    }

    @DisplayName("재직중인 경력에 종료 연월이 있으면 검증에 실패한다")
    @Test
    void failWhenCurrentCareerHasEndYearMonth() {
        CareerPayload payload = new CareerPayload();
        CareerItemPayload item = createCurrentCareer();
        item.setEndYearMonth("2024-03");
        payload.setItems(List.of(item));

        Set<String> messages = validateMessages(payload);

        assertThat(messages).contains("재직중인 경력은 종료 연월을 비워야 합니다.");
    }

    @DisplayName("경력 항목이 50개를 초과하면 검증에 실패한다")
    @Test
    void failWhenCareerItemCountExceedsLimit() {
        CareerPayload payload = new CareerPayload();
        payload.setItems(java.util.stream.IntStream.range(0, 51)
                .mapToObj(index -> createCurrentCareer())
                .toList());

        Set<String> messages = validateMessages(payload);

        assertThat(messages).contains("경력 항목은 50개를 초과할 수 없습니다.");
    }

    @DisplayName("경력 payload를 JSON으로 직렬화한다")
    @Test
    void serializeCareerPayload() throws Exception {
        CareerPayload payload = new CareerPayload();
        payload.setItems(List.of(createCurrentCareer()));

        String json = objectMapper.writeValueAsString(payload);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("version").asInt()).isEqualTo(1);
        assertThat(root.get("items")).hasSize(1);
        assertThat(root.at("/items/0/companyName").asText()).isEqualTo("Generic4");
        assertThat(root.at("/items/0/position").asText()).isEqualTo("Backend Engineer");
        assertThat(root.at("/items/0/currentlyWorking").asBoolean()).isTrue();
        assertThat(root.at("/items/0/techStack/0").asText()).isEqualTo("Java");
    }

    private Set<String> validateMessages(CareerPayload payload) {
        return validator.validate(payload).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private CareerItemPayload createCurrentCareer() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2024-01");
        item.setEndYearMonth(null);
        item.setCurrentlyWorking(true);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));
        return item;
    }

    private CareerItemPayload createPastCareer() {
        CareerItemPayload item = new CareerItemPayload();
        item.setCompanyName("Generic4");
        item.setPosition("Backend Engineer");
        item.setEmploymentType(CareerEmploymentType.FULL_TIME);
        item.setStartYearMonth("2023-01");
        item.setEndYearMonth("2023-12");
        item.setCurrentlyWorking(false);
        item.setSummary("Spring Boot 기반 API를 개발하고 운영했습니다.");
        item.setTechStack(List.of("Java", "Spring Boot", "PostgreSQL"));
        return item;
    }
}
