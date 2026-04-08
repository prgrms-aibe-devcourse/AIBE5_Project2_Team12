package com.generic4.itda.domain.resume;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CareerPayloadJsonConverter implements AttributeConverter<CareerPayload, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(CareerPayload attribute) {
        if (attribute == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("경력 JSON 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public CareerPayload convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, CareerPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("경력 JSON 역직렬화에 실패했습니다.", e);
        }
    }
}
