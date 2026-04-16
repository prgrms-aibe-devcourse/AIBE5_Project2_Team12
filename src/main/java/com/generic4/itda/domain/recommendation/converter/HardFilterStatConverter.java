package com.generic4.itda.domain.recommendation.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.generic4.itda.domain.recommendation.vo.HardFilterStat;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;

@Convert
public class HardFilterStatConverter implements AttributeConverter<HardFilterStat, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(HardFilterStat attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("HardFilterStat을 JSON으로 변환할 수 없습니다.", e);
        }
    }

    @Override
    public HardFilterStat convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(dbData, HardFilterStat.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON을 HardFilterStat로 변환할 수 없습니다.", e);
        }
    }
}
