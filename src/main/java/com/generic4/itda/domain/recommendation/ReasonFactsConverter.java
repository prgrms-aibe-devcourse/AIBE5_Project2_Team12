package com.generic4.itda.domain.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

@Convert
public class ReasonFactsConverter implements AttributeConverter<ReasonFacts, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ReasonFacts attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("ReasonFacts를 JSON으로 변환할 수 없습니다.", e);
        }
    }

    @Override
    public ReasonFacts convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ReasonFacts.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON을 ReasonFacts로 변환할 수 없습니다.", e);
        }
    }
}
